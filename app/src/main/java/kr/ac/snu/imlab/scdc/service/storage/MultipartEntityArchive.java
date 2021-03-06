package kr.ac.snu.imlab.scdc.service.storage;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.storage.HttpArchive;
import edu.mit.media.funf.util.IOUtil;
import edu.mit.media.funf.util.LogUtil;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * Created by kilho on 15. 7. 28.
 */
public class MultipartEntityArchive extends HttpArchive {

  @Configurable
  private String url;

  @Configurable
  private boolean wifiOnly = false;

  private Context context;
  private Activity activity;

  @SuppressWarnings("unused")
  private String mimeType;

  private AlertDialog mAlertDialog;

  private boolean uploadSucceed;

  public MultipartEntityArchive() {

  }

  public MultipartEntityArchive(Context context, final String uploadUrl,
                                Activity callingActivity) {
    this(context, uploadUrl, "application/x-binary", callingActivity);
  }

  public MultipartEntityArchive(Context context, final String uploadUrl,
                final String mimeType, Activity callingActivity) {
    this.context = context;
    this.url = uploadUrl;
    this.mimeType = mimeType;
    this.activity = callingActivity;
  }

  public void setContext(Context context) {
    this.context = context;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public boolean isAvailable() {
    assert context != null;
    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
    if (!wifiOnly && netInfo != null && netInfo.isConnectedOrConnecting()) {
      return true;
    } else if (wifiOnly) {
      State wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
      if (State.CONNECTED.equals(wifiInfo) || State.CONNECTING.equals(wifiInfo)) {
        return true;
      }
    }
    return false;
  }

  public String getId() {
    return url;
  }


  public boolean add(File file) {
    if (activity != null) {
      return IOUtil.isValidUrl(url) ? uploadFile(activity, file, url) : false;
    }

    return false;
  }

  /**
   * Copied (and slightly modified) from Friends and Family
   * @param file
   * @param uploadurl
   * @return
   */
  public boolean uploadFile(final Activity activity,
                            final File file, final String uploadurl) {

    OnClickListener retryUploadListener = new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        try {
          uploadSucceed =
            new BackgroundUploader(activity, uploadurl, file).execute().get();
        } catch (Exception e) {
          uploadSucceed = false;
        }
      }
    };

    try {
      uploadSucceed =
        new BackgroundUploader(activity, uploadurl, file).execute().get();
    } catch (Exception e) {
      uploadSucceed = false;
    }

    while (!uploadSucceed) {
      AlertDialog.Builder alert = new AlertDialog.Builder(activity);
      String message = "Upload failed. Retry?";
      mAlertDialog = alert.setTitle("Warning")
              .setMessage(message)
              .setPositiveButton("YES", retryUploadListener)
              .show();
    }

    file.delete();

    return uploadSucceed;
  }


  /**
   * @author Kilho Kim
   * @description Background uploader class
   * @reference http://delimitry.blogspot.in/2011/08/android-upload-progress.html
   */
  private class BackgroundUploader extends AsyncTask<Void, Integer,
            Boolean> implements DialogInterface.OnCancelListener {

    private Activity activity;
    private ProgressDialog progressDialog;
    private String uploadurl;
    private File file;

    public BackgroundUploader(Activity activity, String uploadurl, File file) {
      this.activity = activity;
      this.uploadurl = uploadurl;
      this.file = file;
    }

    @Override
    protected void onPreExecute() {
      progressDialog = new ProgressDialog(activity);
      progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      progressDialog.setMessage("Uploading...");
      progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  cancel(true);
                  dialog.dismiss();
                }
              });
      progressDialog.setMax((int)file.length());
      progressDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... v) {
      boolean isSuccess = true;

      HttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost(uploadurl);
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

      FileBody fb = new FileBody(file);

      builder.addPart("uploadedfile", fb);
      final HttpEntity yourEntity = builder.build();


      class ProgressiveEntity implements HttpEntity {
        int progress = 0;   // initialize progress

        @Override
        public void consumeContent() throws IOException {
          yourEntity.consumeContent();
        }
        @Override
        public InputStream getContent() throws IOException,
                IllegalStateException {
          return yourEntity.getContent();
        }
        @Override
        public Header getContentEncoding() {
          return yourEntity.getContentEncoding();
        }
        @Override
        public long getContentLength() {
          return yourEntity.getContentLength();
        }
        @Override
        public Header getContentType() {
          return yourEntity.getContentType();
        }
        @Override
        public boolean isChunked() {
          return yourEntity.isChunked();
        }
        @Override
        public boolean isRepeatable() {
          return yourEntity.isRepeatable();
        }
        @Override
        public boolean isStreaming() {
          return yourEntity.isStreaming();
        } // CONSIDER put a _real_ delegator into here!

        @Override
        public void writeTo(OutputStream outstream) throws IOException {

          class ProxyOutputStream extends FilterOutputStream {
            /**
             * @author Stephen Colebourne
             */

            public ProxyOutputStream(OutputStream proxy) {
              super(proxy);
            }
            public void write(int idx) throws IOException {
              out.write(idx);
            }
            public void write(byte[] bts) throws IOException {
              out.write(bts);
            }
            public void write(byte[] bts, int st, int end) throws IOException {
              out.write(bts, st, end);
            }
            public void flush() throws IOException {
              out.flush();
            }
            public void close() throws IOException {
              out.close();
            }
          } // CONSIDER import this class (and risk more Jar File Hell)

          class ProgressiveOutputStream extends ProxyOutputStream {
            public ProgressiveOutputStream(OutputStream proxy) {
              super(proxy);
            }
            public void write(byte[] bts, int st, int end) throws IOException {

              // FIXME  Put your progress bar stuff here!
              progress += end;
              publishProgress(progress);    // update progress bar

              out.write(bts, st, end);
            }
          }

          yourEntity.writeTo(new ProgressiveOutputStream(outstream));
        }

      };
      try {
        ProgressiveEntity myEntity = new ProgressiveEntity();

        post.setEntity(myEntity);
        HttpResponse response = client.execute(post);

        Log.w("DEBUG", "MultipartEntityArchive/ response=" +
                getContent(response));
      } catch (Exception e) {
        Log.e("FNF", "MultipartEntityArchive/ Connection error", e);
        isSuccess = false;
      }

      return isSuccess;
    }

      @Override
    protected void onProgressUpdate(Integer... progress) {
      progressDialog.setProgress((int)(progress[0]));
      // Toast.makeText(activity, (int) (progress[0]),
      // Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPostExecute(Boolean isSuccess) {
      progressDialog.dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
      cancel(true);
      dialog.dismiss();
    }

  }

  public static String getContent(HttpResponse response) throws IOException {
    BufferedReader rd = new BufferedReader(
            new InputStreamReader(response.getEntity().getContent()));
    String body = "";
    String content = "";

    while ((body = rd.readLine()) != null)
    {
      content += body + "\n";
    }
    return content.trim();
  }



}
