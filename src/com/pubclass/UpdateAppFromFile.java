package com.pubclass;

import com.mysoqi.onlineorder.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.view.WindowManager;

/**
 * APP 更新, 採用 HTTP 連線下載檔案到 '公用的目錄', 再執行安裝程序
 * <P>
 * 下載更新檔, 下載時顯示進度框, 完成後自動安裝 APP
 */
public class UpdateAppFromFile {
	private Context mContext;
	private PubClass pubClass;
	private ProgressDialog mProgDialog;

	private String strUrl, strFilename_apk, strFilename_ver;
	private String strDownloadFilePath; // 公用的下載目錄路徑, ex. /mtn/sdcard/Download
	private Boolean isStopDownload = false; // 停止下載辨識標記, 下載dialogbox使用
	
	// 本專案使用
	private Handler mHandler;
	
	/**
	 * APP 檢查版本, 若版本不同下載更新檔並自動安裝
	 * 
	 * @param context
	 *            : Context
	 * @param url
	 *            : APP程式檔與版本文字檔下載網址, ex. http://downloa/data/
	 * @param filename_apk
	 *            : APP apk 檔名, ex. APPName.apk
	 * 
	 * @param filename_ver
	 *            : APP apk 檔名, ex. APPName.apk
	 *            
	 * @param handler 
	 * 				: 傳遞參數給 parent，由parent 執行相關程序
	 * 
	 */
	public UpdateAppFromFile(Context context, String url, String filename_apk,
			String filename_ver, Handler handler) {
		mContext = context;
		pubClass = new PubClass(mContext);
		mHandler = handler;

		// 進度與提示 DialogBox
		mProgDialog = new ProgressDialog(mContext);
		mProgDialog.setTitle(mContext.getString(R.string.systemnotify));
		mProgDialog.setMessage(mContext.getString(R.string.datatransfering));
		mProgDialog.setCancelable(false);

		strUrl = url;
		strFilename_apk = filename_apk;
		strFilename_ver = filename_ver;
		strDownloadFilePath = (Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
				.toString()
				+ "/";
	}

	/**
	 * 開始執行本 class 各項程序
	 */
	public void start() {
		new connChkVersion().execute();
	}

	/**
	 * Class, HTTP 連線檢查版本 code
	 */
	private class connChkVersion extends AsyncTask<Void, Void, Boolean> {

		protected void onPreExecute() {
		}

		protected Boolean doInBackground(Void... arg) {

			// 取得下載網址的版本
			HttpGet httppost = new HttpGet(strUrl + strFilename_ver);
			DefaultHttpClient httpclient = new DefaultHttpClient();

			HttpResponse response;
			try {
				String strCode;
				response = httpclient.execute(httppost);
				HttpEntity ht = response.getEntity();
				BufferedHttpEntity buf = new BufferedHttpEntity(ht);
				InputStream is = buf.getContent();
				BufferedReader r = new BufferedReader(new InputStreamReader(is));
				strCode = r.readLine();

				// 版本不同, 需要更新
				if (!strCode.equals("0")
						&& !strCode
								.equals(pubClass.getVersionData().get("ver"))) {
					return true;
				}

			} catch (Exception e) {
			}

			return false;
		}

		protected void onPostExecute(Boolean bolRS) {
			chkNeedUpdateApp(bolRS);
		}
	}

	/**
	 * 檢查是否需要下載 APP 程式檔案並自動更新
	 * 
	 * @param bolRS
	 */
	private void chkNeedUpdateApp(Boolean bolRS) {
		if (bolRS) {
			// DialogBox Listener, 點取確定下載更新
			DialogInterface.OnClickListener mListener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					arg0.dismiss();

					// 開始執行 Download 下載程序
					new connDownloadFile().execute();
				}
			};

			// DialogBox, 顯示是否要更新
			AlertDialog.Builder mBuilder = new AlertDialog.Builder(mContext);

			mBuilder.setTitle(R.string.systemnotify).setMessage(
					R.string.foundnewversionmsg);
			mBuilder.setPositiveButton(R.string.string_yes, mListener);
			mBuilder.setCancelable(false);

			// 取消更新
			mBuilder.setNegativeButton(R.string.string_no,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							
							mHandler.obtainMessage(9001, 0, 0, null).sendToTarget();
							return;
						}
					});

			AlertDialog mDialog = mBuilder.create();
			mDialog.show();
		}
		
		// 傳遞參數給 parent, 版本相同執行相關程序
		else {
			mHandler.obtainMessage(1001, 0, 0, null).sendToTarget();
		}
	}

	/**
	 * Class, HTTP 連線下載更新檔
	 */
	private class connDownloadFile extends AsyncTask<Void, Integer, Boolean> {

		protected void onPreExecute() {
			// Dialog 初始設定
			mProgDialog.setProgress(0);
			// mProgDialog.setMax(listFiles.size()); // 設定要下載的檔案總數目
			mProgDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgDialog.setCancelable(false);
			// mProgDialog.setIndeterminate(false);

			mProgDialog.setMessage(mContext
					.getString(R.string.filedownloadplzwait));
			mProgDialog.setTitle(R.string.systemnotify);

			mProgDialog.setProgressNumberFormat("%d KB / %d KB");

			// 設定停止 Listener
			mProgDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
					mContext.getString(R.string.cancel),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							isStopDownload = true;
							dialog.dismiss();
						}
					});

			mProgDialog.show();		
		}

		protected Boolean doInBackground(Void... arg) {
			InputStream input = null;
			OutputStream output = null;
			HttpURLConnection connection = null;
			Integer[] intFilesize = { 0, 0 };

			try {
				URL url = new URL(strUrl + strFilename_apk);
				connection = (HttpURLConnection) url.openConnection();
				connection.connect();

				// 判別連線是否成功
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
					return false;

				// 建立 Stream, 接收資料並存檔
				int fileLength = connection.getContentLength();

				input = connection.getInputStream();
				output = new FileOutputStream(strDownloadFilePath
						+ strFilename_apk);
				byte data[] = new byte[1024];
				long total = 0;
				int count;

				// 計算已下載 filesize (KB)
				while ((count = input.read(data)) != -1) {
					if (isCancelled() || isStopDownload) {
						input.close();

						return false;
					}
					total += count;

					if (fileLength > 0) {
						// publishProgress((int) (total * 100 / fileLength));
						intFilesize[0] = (int) (total / 1024);
						intFilesize[1] = (int) (fileLength / 1024);
						publishProgress(intFilesize);
					}
					output.write(data, 0, count);
				}
			} catch (Exception e) {
				return false;
			} finally {
				try {
					if (output != null)
						output.close();
					if (input != null)
						input.close();
				} catch (IOException ignored) {
				}

				if (connection != null)
					connection.disconnect();
			}

			return true;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			mProgDialog.setMax(values[1]);
			mProgDialog.setProgress(values[0]);
		}

		protected void onPostExecute(Boolean bolRS) {
			chkRespon(bolRS);
		}
	}

	/**
	 * 檢查版本code value, 比對是否需要下載安裝新程式
	 * 
	 * @param strRespon
	 */
	private void chkRespon(Boolean bolRS) {
		mProgDialog.dismiss();

		// 下載檔案失敗
		if (!bolRS) {
			mHandler.obtainMessage(9001, 0, 0, null).sendToTarget();
			return;
		}

		// 需要更新且更新檔已存檔,開始安裝
		File mApkFile = new File(strDownloadFilePath + strFilename_apk);
		Intent mIntent = new Intent(Intent.ACTION_VIEW);
		mIntent.setDataAndType(Uri.fromFile(mApkFile),
				"application/vnd.android.package-archive");
		mContext.startActivity(mIntent);

		// 網頁下載後USER自行安裝APP程式
		// UpdateFromHTTP();

		mHandler.obtainMessage(9001, 0, 0, null).sendToTarget();
		return;
	}
}