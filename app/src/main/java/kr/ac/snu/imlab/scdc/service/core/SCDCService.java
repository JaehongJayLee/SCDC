package kr.ac.snu.imlab.scdc.service.core;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.gson.JsonObject;

import java.io.File;

import edu.mit.media.funf.pipeline.Pipeline;
import kr.ac.snu.imlab.scdc.activity.LaunchActivity;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.LogKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.Config;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.SCDCServiceKeys;
import kr.ac.snu.imlab.scdc.service.probe.InsensitiveProbe;
import kr.ac.snu.imlab.scdc.util.SharedPrefsHandler;
import kr.ac.snu.imlab.scdc.R;

/**
 * Created by kilho on 16. 2. 19.
 */
public class SCDCService extends Service {

  protected static final String TAG = "SCDCService";

  private SCDCManager scdcManager;
  private SCDCPipeline pipeline;
  private SharedPrefsHandler spHandler;

  private ServiceConnection scdcManagerConn = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      Log.d(SCDCKeys.LogKeys.DEB, TAG+".onServiceConnected()");
      scdcManager = ((SCDCManager.LocalBinder) service).getManager();
//      scdcManager.reload();
      pipeline = (SCDCPipeline) scdcManager.getRegisteredPipeline
              (SCDCKeys.Config.PIPELINE_NAME);
      scdcManager.enablePipeline(pipeline.getName());

      Log.d(LogKeys.DEBUG, TAG+".scdcManagerConn" +
              ".onServiceConnected(): pipeline.getName()=" +
              pipeline.getName() + ", pipeline.isEnabled()=" + pipeline.isEnabled() +
              ", pipeline.getDatabaseHelper()=" + pipeline.getDatabaseHelper());
//      pipeline.setDataReceivedListener(LaunchActivity.this);
      // Update probe schedules of pipeline
      Log.d(LogKeys.DEBUG, TAG+".scdcManagerConn.onServiceConnected(): "
              + "spHandler.isActiveLabelOn()=" +
              spHandler.isActiveLabelOn());
//      changeConfig(spHandler.isActiveLabelOn());

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      Log.d(SCDCKeys.LogKeys.DEB, TAG+".onServiceDisconnected()");
      Log.d(LogKeys.DEBUG, TAG+".scdcManagerConn.onServiceDisconnected() called");
      scdcManager = null;
      pipeline = null;
    }
  };

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d(SCDCKeys.LogKeys.DEB, TAG+".onStartCommand()");
    super.onStartCommand(intent, flags, startId);

    spHandler = SharedPrefsHandler.getInstance(this,
            Config.SCDC_PREFS, Context.MODE_PRIVATE);

    bindService(new Intent(this, SCDCManager.class),
                scdcManagerConn, BIND_AUTO_CREATE);
    startForeground(SCDCServiceKeys.SCDC_NOTIFICATION_ID, makeNotification());

    return Service.START_STICKY;
  }

  private Notification makeNotification() {
    Intent intent = new Intent(this, LaunchActivity.class);
    PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent,
                                            PendingIntent.FLAG_UPDATE_CURRENT);
    return new NotificationCompat.Builder(this)
            .setContentTitle("SCDC Service")
            .setContentText("Running SCDC Service")
            .setSmallIcon(R.drawable.red_icon)
            .setContentIntent(pIntent)
            .build();
  }

  @Override
  public void onDestroy() {
    Log.d(SCDCKeys.LogKeys.DEB, TAG+".onDestroy()");
    super.onDestroy();
    Log.d(LogKeys.DEBUG, TAG+".onDestroy() called");

    for (Object probeObject : scdcManager.getProbeFactory().getCached()) {
      if (probeObject instanceof InsensitiveProbe) {
        Log.d(SCDCKeys.LogKeys.DEB, TAG+".onDestroy(): call sendFinalData()");
        ((InsensitiveProbe) probeObject).sendFinalData();
      }
    }

    stopForeground(true);
    unbindService(scdcManagerConn);
  }

  public class LocalBinder extends Binder {
    public SCDCService getService() {
      return SCDCService.this;
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.d(SCDCKeys.LogKeys.DEB, TAG+".onBind()");
    return new LocalBinder();
  }

  public long getDBSize() {
    if (pipeline != null) {
      if (pipeline.getDatabaseHelper() == null) {
        pipeline.reloadDbHelper(scdcManager);
      }

      // Query the pipeline db for the count of rows in the data table
      SQLiteDatabase db = pipeline.getDb();
      final long dbSize = new File(db.getPath()).length();  // in bytes
      return dbSize;
    } else {
      return 0L;    // FIXME
    }
  }

  public boolean saveAndReload(String pipelineName, JsonObject newConfig) {
    if (scdcManager != null) {
      Log.d(LogKeys.DEBUG, TAG+".saveAndReload(" + pipelineName + ", newConfig): call scdcManager.saveAndReload()");
      return scdcManager.saveAndReload(pipelineName, newConfig);
    } else {
      return false;
    }
  }

  public Pipeline getRegisteredPipeline(String name) {
    return scdcManager.getRegisteredPipeline(name);
  }

  public JsonObject getPipelineConfig(String name) {
    return scdcManager.getPipelineConfig(name);
  }
}
