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
package kr.ac.snu.imlab.scdc.service.probe;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.SensorKeys;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;

@Schedule.DefaultSchedule(interval= InsensitiveSensorProbe.DEFAULT_PERIOD, duration= InsensitiveSensorProbe.DEFAULT_DURATION)
public abstract class InsensitiveSensorProbe extends InsensitiveProbe implements ContinuousProbe, SensorKeys {

	public static final double DEFAULT_PERIOD = 61;
	public static final double DEFAULT_DURATION = 60;

	@Configurable
	private String sensorDelay = SENSOR_DELAY_GAME;

	public static final String
		SENSOR_DELAY_FASTEST = "FASTEST",
		SENSOR_DELAY_GAME = "GAME",
		SENSOR_DELAY_UI = "UI",
		SENSOR_DELAY_NORMAL = "NORMAL";

	private SensorManager sensorManager;
	private Sensor sensor;
	private SensorEventListener sensorListener;

	@Override
	protected void onEnable() {
		super.onEnable();
		sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(getSensorType());
		final String[] valueNames = getValueNames();
		sensorListener = new SensorEventListener() {

			@Override
			public void onSensorChanged(SensorEvent event) {
				BigDecimal currTimestamp = TimeUtil.getTimestamp();
				JsonObject data = new JsonObject();
        // FIXME: TIMESTAMP for all SensitiveSensorsProbe's
				data.addProperty(TIMESTAMP, currTimestamp);
				data.addProperty(ACCURACY, event.accuracy);
				int valuesLength = Math.min(event.values.length, valueNames.length);
				for (int i = 0; i < valuesLength; i++) {
					String valueName = valueNames[i];
					data.addProperty(valueName, event.values[i]);
				}
				if (lastData == null) {
					lastData = data;
				} else {
					currData = data;
					if (isDataChanged()) sendData();
				}
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
	}

	@Override
	protected void onStart() {
		super.onStart();
		getSensorManager().registerListener(sensorListener,sensor, getSensorDelay(sensorDelay));
	}

	@Override
	protected void onStop() {
		getSensorManager().unregisterListener(sensorListener);
	}

	protected SensorManager getSensorManager() {
		if (sensorManager == null) {
			sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
		}
		return sensorManager;
	}

	protected int getSensorDelay(String specifiedSensorDelay) {
		int sensorDelay = -1;
		JsonElement el =  getGson().toJsonTree(specifiedSensorDelay);
		if (!el.isJsonNull()) {
			try {
				int sensorDelayInt = el.getAsInt();
				if (sensorDelayInt == SensorManager.SENSOR_DELAY_FASTEST
						|| sensorDelayInt == SensorManager.SENSOR_DELAY_GAME
						|| sensorDelayInt == SensorManager.SENSOR_DELAY_UI
						|| sensorDelayInt == SensorManager.SENSOR_DELAY_NORMAL) {
					sensorDelay = sensorDelayInt;
				}
			} catch (NumberFormatException e) {
			} catch (ClassCastException e) {
			} catch (IllegalStateException e) {
			}
		}

		if (sensorDelay < 0) {
			try {
				String sensorDelayString = el.getAsString().toUpperCase().replace("SENSOR_DELAY_", "");
				if (SENSOR_DELAY_FASTEST.equals(sensorDelayString)) {
					sensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
				} else if (SENSOR_DELAY_GAME.equals(sensorDelayString)) {
					sensorDelay = SensorManager.SENSOR_DELAY_GAME;
				} else if (SENSOR_DELAY_UI.equals(sensorDelayString)) {
					sensorDelay = SensorManager.SENSOR_DELAY_UI;
				} else if (SENSOR_DELAY_NORMAL.equals(sensorDelayString)) {
					sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
				}
			} catch (ClassCastException cce) {
				Log.w(LogUtil.TAG, "Unknown sensor delay value: " + specifiedSensorDelay);
			}
		}

		if (sensorDelay < 0) {
			sensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
		}

		return sensorDelay;
	}

	public abstract int getSensorType();
	public abstract String[] getValueNames();
}
