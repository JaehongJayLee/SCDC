/**
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Funf is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package edu.mit.media.funf.probe.builtin;

import android.content.Context;
import android.os.Bundle;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.CellKeys;
import edu.mit.media.funf.time.TimeUtil;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.service.probe.InsensitiveProbe;

@DisplayName("Nearby Cellular Towers Probe")
@RequiredFeatures("android.hardware.telephony")
@RequiredPermissions(android.Manifest.permission.ACCESS_COARSE_LOCATION)
public class CellTowerProbe extends InsensitiveProbe implements Probe.ContinuousProbe, CellKeys {

	private double checkInterval = 10.0;
	private CellTowerChecker cellTowerChecker = new CellTowerChecker();

	private class CellTowerChecker implements Runnable {

		@Override
		public void run() {
			if (lastData == null) {
				lastData = getCurrData();
			} else {
				currData = getCurrData();
				if (isDataChanged()) sendData();
			}
			getHandler().postDelayed(this, TimeUtil.secondsToMillis(checkInterval));
		}
	}
	
	@Override
	protected void onStart() {
		Log.d(SCDCKeys.LogKeys.DEB, "[CellTowerProbe] onStart");
		super.onStart();
		sendData(getGson().toJsonTree(getData()).getAsJsonObject());
		onContinue();
	}

	protected void onContinue() {
//        Log.d(LogUtil.TAG, "[CellTowerProbe] onContinue");
		getHandler().post(cellTowerChecker);
	}

	protected void onPause() {
//        Log.d(LogUtil.TAG, "[CellTowerProbe] onPause");
		getHandler().removeCallbacks(cellTowerChecker);
	}

	@Override
	protected void onStop() {
		Log.d(SCDCKeys.LogKeys.DEB, "[CellTowerProbe] onStop");
		onPause();
		super.onStop();
	}

	@Override
	protected JsonObject getCurrData() {
		Log.d(SCDCKeys.LogKeys.DEB, "[CellTowerProbe] getCurrData");
		JsonObject data = getGson().toJsonTree(getData()).getAsJsonObject();
		data.addProperty(TIMESTAMP, TimeUtil.getTimestamp());
		return data;
	}
	
	private Bundle getData() {
		TelephonyManager manager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
		CellLocation location = manager.getCellLocation();
		Bundle data = new Bundle();
		if (location instanceof GsmCellLocation) {
			GsmCellLocation gsmLocation = (GsmCellLocation) location;
			gsmLocation.fillInNotifierBundle(data);
			data.putInt(TYPE, TelephonyManager.PHONE_TYPE_GSM);
		} else if (location instanceof CdmaCellLocation) {
			CdmaCellLocation cdmaLocation = (CdmaCellLocation) location;
			cdmaLocation.fillInNotifierBundle(data);
			data.putInt(TYPE, TelephonyManager.PHONE_TYPE_CDMA);
		} else {
			data.putInt(TYPE, TelephonyManager.PHONE_TYPE_NONE);
		}
		return data;
	}
	
}
