package kr.ac.snu.imlab.scdc.adapter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Locale;

import kr.ac.snu.imlab.scdc.R;
import kr.ac.snu.imlab.scdc.activity.LaunchActivity;
import kr.ac.snu.imlab.scdc.entry.LabelEntry;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.Config;
import kr.ac.snu.imlab.scdc.service.storage.SCDCDatabaseHelper.SensorIdInfo;
import kr.ac.snu.imlab.scdc.util.SharedPrefsHandler;
import kr.ac.snu.imlab.scdc.util.TimeUtil;

public class BaseAdapterData extends BaseAdapter {

  protected static final String TAG = "BaseAdapterData";

  Context mContext = null;
  ArrayList<SensorIdInfo> mData = null;
  LayoutInflater mLayoutInflater = null;
//  SharedPrefsHandler spHandler = null;

  Handler handler;

  private SimpleDateFormat dataFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

  public BaseAdapterData(Context context, ArrayList<SensorIdInfo> data) {
    this.mContext = context;
    this.mData = data;
    this.mLayoutInflater = LayoutInflater.from(this.mContext);
//    this.spHandler = SharedPrefsHandler.getInstance(this.mContext,
//                        Config.SCDC_PREFS, Context.MODE_PRIVATE);
  }

  @Override
  public int getCount() {
    return this.mData.size();
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public SensorIdInfo getItem(int position) {
    return this.mData.get(position);
  }

  class ViewHolder {
    TextView sensorIdTextView;
    TextView aloneOrTogetherTextView;
    TextView labelTextView;
    TextView startTimeTextView;
    TextView endTimeTextView;
    LinearLayout seekBarLayout;
//    Button dataSaveButton;
    Button dataDeleteButton;
  }

  @Override
  public View getView(final int position, View convertView, ViewGroup parent) {
    View itemLayout = convertView;
    final ViewHolder viewHolder;


    if (itemLayout == null) {
      itemLayout = mLayoutInflater.inflate(R.layout.data_list_view_item_layout, null);
      viewHolder = new ViewHolder();
      viewHolder.sensorIdTextView = (TextView)itemLayout.findViewById(R.id.sensor_id_tv);
      viewHolder.aloneOrTogetherTextView = (TextView)itemLayout.findViewById(R.id.alone_or_together_tv);
      viewHolder.labelTextView = (TextView)itemLayout.findViewById(R.id.label_tv);
      viewHolder.startTimeTextView = (TextView)itemLayout.findViewById(R.id.start_time_tv);
      viewHolder.endTimeTextView = (TextView)itemLayout.findViewById(R.id.end_time_tv);

      viewHolder.seekBarLayout = (LinearLayout) itemLayout.findViewById(R.id.seekbar_layout);
//      viewHolder.dataSaveButton = (Button) itemLayout.findViewById(R.id.data_save_button);
      viewHolder.dataDeleteButton = (Button) itemLayout.findViewById(R.id.data_delete_button);
      itemLayout.setTag(viewHolder);
    } else {
      viewHolder = (ViewHolder)itemLayout.getTag();
    }

    itemLayout.setOnLongClickListener(new OnLongClickListener() {
      @Override
      public boolean onLongClick(View v) {
        return false;
      }
    });

    SensorIdInfo info = mData.get(position);

    String sensorIdStr = String.valueOf(info.sensorId);
    String startTimeStr = dataFormat.format(info.firstTS*1000);
    String endTimeStr = dataFormat.format(info.lastTS*1000);
    String togetherStr =info.firstTogether;
    String labelStr = info.firstLabel;

    Log.d(SCDCKeys.LogKeys.DEBB, sensorIdStr+" "+startTimeStr+" "+endTimeStr+" "+togetherStr+" "+labelStr);

//  FIXME:  android.content.res.Resources$NotFoundException: String resource ID #0x1b
    viewHolder.sensorIdTextView.setText(sensorIdStr);
    viewHolder.aloneOrTogetherTextView.setText(togetherStr);
    viewHolder.labelTextView.setText(labelStr);
    viewHolder.startTimeTextView.setText(startTimeStr+mContext.getString(R.string.data_start));
    viewHolder.endTimeTextView.setText(endTimeStr+mContext.getString(R.string.data_end));

    handler = new Handler();

//    viewHolder.dataSaveButton.setOnClickListener(new View.OnClickListener() {
//      @Override
//      public void onClick(View v) {
//
//        // when clicked, save the change and refresh
//
//      }
//    });

    viewHolder.dataDeleteButton.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View v) {

        // when clicked, delete this data and refresh

      }
    });

    itemLayout.setClickable(true);
    return itemLayout;
  }



  // below are copied from old version

  protected void notify(int mId, String title, String message,
                           String alert) {

    // Create a new notification builder
    NotificationCompat.Builder builder =
       new NotificationCompat.Builder(mContext)
              .setAutoCancel(false)
              .setContentIntent(getPendingIntent(mId))
              .setContentTitle(title)
              .setContentText(message)
              .setTicker(alert)
              // .setDefaults(Notification.DEFAULT_ALL)
              .setSmallIcon(R.mipmap.ic_launcher)
              .setOngoing(true)
              .setWhen(System.currentTimeMillis());

    @SuppressWarnings("deprecation")
    Notification notification = builder.getNotification();
    NotificationManager notificationMgr = (NotificationManager)mContext.
                          getSystemService(Context.NOTIFICATION_SERVICE);
    notificationMgr.notify(mId, notification);

  }

  protected void cancelNotify(int mId) {
    NotificationManager notificationMgr =
            (NotificationManager)mContext.
                    getSystemService(Context.NOTIFICATION_SERVICE);
    notificationMgr.cancel(mId);
  }

  protected void cancelNotifyAll() {
    NotificationManager notificationMgr =
            (NotificationManager)mContext.
                    getSystemService(Context.NOTIFICATION_SERVICE);
    notificationMgr.cancelAll();
  }

  PendingIntent getPendingIntent(int id) {
    Intent intent = new Intent(mContext, LaunchActivity.class);
    return PendingIntent.getActivity(mContext, id, intent, 0);
  }

}