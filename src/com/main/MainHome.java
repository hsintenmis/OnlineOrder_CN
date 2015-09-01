package com.main;

import java.io.IOException;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.mysoqi.onlineorder.R;
import com.pubclass.PubClass;
import com.pubclass.UpdateAppFromFile;

/**
 * 美利線上訂購系統，連線美利 Server (delphi)
 */
public class MainHome extends Activity {
	private Context mContext;
	private PubClass pubClass;
	private Resources mResos;
	private String strPkg;

	// HTTP 連線相關
	private HttpClient mHttpclient;
	private HashMap<String, String> aryUser;
	private ProgressDialog mProgDialog;
	private AlertDialog mDialog;

	// Layout property 設定
	private Button btnLogin;
	private EditText edLoginAcc, edLoginPsd;
	private CheckBox chkSave;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set property value
		mContext = this;
		pubClass = new PubClass(mContext);
		pubClass.hideWindowHeadStatu(this.getWindow());
		mResos = mContext.getResources();
		strPkg = mContext.getPackageName();

		pubClass.hideWindowHeadStatu(this.getWindow());
		setContentView(R.layout.main_home);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mHttpclient = pubClass.getHTTPClient();
		aryUser = pubClass.getUserData();

		// 版本文字
		((TextView) findViewById(R.id.tvVersion)).setText(String.format(
				mContext.getString(R.string.format_versionmsg),
				(pubClass.getVersionData()).get("name")));

		// Layout property 設定
		btnLogin = ((Button) findViewById(R.id.btnLogin));
		btnLogin.setEnabled(false);
		edLoginAcc = ((EditText) findViewById(R.id.edLoginAcc));
		edLoginPsd = ((EditText) findViewById(R.id.edLoginPsd));
		chkSave = ((CheckBox) findViewById(R.id.chkSave));

		// 進度與提示 DialogBox
		mProgDialog = new ProgressDialog(mContext);
		mProgDialog.setTitle(mContext.getString(R.string.userloginverify));
		mProgDialog.setMessage(mContext.getString(R.string.datatransfering));
		mProgDialog.setCancelable(false);

		// 提示或錯誤訊息 DialogBox
		AlertDialog.Builder mBuilder = new AlertDialog.Builder(mContext);
		mBuilder.setPositiveButton(R.string.confirmisee,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// ((Activity) mContext).finish();
						dialog.dismiss();

						return;
					}
				});
		mDialog = mBuilder.create();
		mDialog.setTitle(R.string.confirm_dialbox);
		mDialog.setCancelable(false);

		// 檢查網路連線
		boolean haveInternet = pubClass.haveInternet();
		if (!haveInternet) {
			PubClass.xxDump(mContext, mContext.getString(R.string.err_internet));
			this.finish();

			return;
		}

		// 版更新檢查
		UpdateAppFromFile mUpdataApp = new UpdateAppFromFile(mContext,
				PubClass.URL_UPDATE, PubClass.D_UPDATEFILENAME_APP,
				PubClass.D_UPDATEFILENAME_VERCODE, mHandler);
		mUpdataApp.start();

		// 設定本頁 Layout Button Listener
		this.setPageButtonLstn();

		// 登入帳號密碼, 網站語系, 檢查Preference的 saveLogin
		if (aryUser.get("saveLogin").equalsIgnoreCase("Y")) {
			edLoginAcc.setText(aryUser.get("acc"));
			edLoginPsd.setText(aryUser.get("psd"));
			chkSave.setChecked(true);
		}
	}

	/**
	 * 設定本頁 Layout Button Listener
	 */
	@SuppressWarnings("unchecked")
	private void setPageButtonLstn() {
		// 登入
		btnLogin.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String strAcc = edLoginAcc.getText().toString();
				String strPsd = edLoginPsd.getText().toString();
				String strErrMsg = "";

				// 檢查輸入資料
				if (strAcc.length() < 1 || strPsd.length() < 1)
					strErrMsg = mContext.getString(R.string.loginfailure);

				if (strErrMsg != "") {
					mDialog.setMessage(strErrMsg);
					mDialog.show();

					return;
				}

				// 執行 HTTP 連線, 設定http post資料
				HashMap<String, String> postData = new HashMap<String, String>();
				postData.put("id", strAcc.toUpperCase());
				postData.put("pw", strPsd);
				postData.put("module", "onlineproduct");

				new _HttpConn().execute(postData);

				return;
			}
		});

		// 離開系統 Button Listener
		((Button) findViewById(R.id.btnExit))
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						((Activity) mContext).finish();

						return;
					}
				});
	}

	/**
	 * Class, 登入 HTTP 連線, 連線目標網址為<br>
	 */
	private class _HttpConn extends
			AsyncTask<HashMap<String, String>, Void, String> {

		private HttpPost httppost = new HttpPost(PubClass.D_WEBURL);

		protected void onPreExecute() {
			mProgDialog.show();
		}

		protected String doInBackground(HashMap<String, String>... mPostData) {
			try {
				httppost.setEntity(new UrlEncodedFormEntity(pubClass
						.getPostData(mPostData[0])));

				// HTTP 連線,取得回傳 String
				HttpResponse httpResopn = mHttpclient.execute(httppost);

				// 取出回應字串
				String strRespon = EntityUtils.toString(httpResopn.getEntity());

				if (strRespon != null) {
					return strRespon;
				}

			} catch (ClientProtocolException e) {
			} catch (IOException e) {
			} catch (NullPointerException e) {
			}

			return null;
		}

		protected void onPostExecute(String strRespon) {
			chkRespon(strRespon);
		}
	}

	/**
	 * 檢查user 帳號密碼,分析伺服器回傳的資料
	 * <P>
	 * 回傳資料為 JSONString, 格式如. <br>
	 * ary('result'=>boolean, id='LoginID', msg=>'errmsg' or NULL)
	 * 
	 * @param strRespon
	 */
	private void chkRespon(String strRespon) {
		mProgDialog.dismiss();
		String strErrMsg = "";

		// 解析回傳的 JSON String to JSON array data
		JSONObject jobjContent = null;
		JSONObject jobjPd = null;
		JSONObject jobjMember = null;

		try {
			JSONObject jobjRoot = new JSONObject(strRespon);
			jobjContent = jobjRoot.getJSONObject("content");

			// USER 認證檢查
			if (!jobjRoot.getBoolean("result")) {
				strErrMsg = pubClass.getLang(jobjRoot.getString("msg"));

				if (strErrMsg.length() < 1 || strErrMsg == "")
					strErrMsg = this.getString(R.string.loginfailure);
			} else {
				// 取得 PD array, member
				jobjPd = new JSONObject();
				jobjPd.put("C", jobjContent.getJSONArray("C"));
				jobjPd.put("N", jobjContent.getJSONArray("N"));
				jobjPd.put("S", jobjContent.getJSONArray("S"));
				jobjMember = jobjContent.getJSONArray("member").getJSONObject(0);
			}

		} catch (Exception e) {
			strErrMsg = this.getString(R.string.err_systemdatamaintain);
		}

		if (strErrMsg != "" || jobjPd == null) {
			PubClass.xxDump(mContext, strErrMsg);

			return;
		}

		// 特殊分類商品，可能為 null
		try {
			jobjPd.put("P", jobjContent.optJSONArray("P"));
		} catch(Exception e) {			
		}
		
		try {
			jobjPd.put("H", jobjContent.optJSONArray("H"));
		} catch(Exception e) {			
		}

		try {
			jobjPd.put("B", jobjContent.optJSONArray("B"));
		} catch(Exception e) {			
		}		

		// 取得 SharedPreferences, 將登入資料寫入
		String strAcc, strPsd, strSave;
		strAcc = strPsd = "";
		strSave = "N";
		strAcc = ((String) edLoginAcc.getText().toString()).toUpperCase();
		strPsd = edLoginPsd.getText().toString();
		strSave = (chkSave.isChecked()) ? "Y" : "N";

		SharedPreferences perfData = this.getSharedPreferences(
				PubClass.D_PREFERNAME, 0);
		SharedPreferences.Editor editor = perfData.edit();
		editor.putString("acc", strAcc);
		editor.putString("psd", strPsd);
		editor.putString("saveLogin", strSave);
		editor.commit();

		// 輸入欄位清空
		edLoginAcc.setText("");
		edLoginPsd.setText("");

		// 開啟新 Activity
		Intent mIntent = new Intent();
		mIntent.setClass(mContext, com.program.OrderMain.class);
		mIntent.putExtra("jobjPd", jobjPd.toString());

		mIntent.putExtra("acc", strAcc);
		mIntent.putExtra("psd", strPsd);

		// 2015/05 新增 member jsonObject,
		// member[0] => 'description', 'tel_h', 'tel_h', 'm_phone',
		// 'office_code'
		mIntent.putExtra("jobjMember", jobjMember.toString());

		mContext.startActivity(mIntent);

		((Activity) mContext).finish();

		return;
	}

	/**
	 * Handler, Parent/Child 程序參數傳遞界面, 用於APP線上更新程序
	 */
	private final Handler mHandler = new Handler() {
		// 接收傳來的識別標記再做相關處理
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			// APP版本更新程序，版本相同不需要更新，直接執行其他程序
			case 1001:
				// 登入 Button Enable
				btnLogin.setEnabled(true);
				break;

			// 各種問題，本系統停止
			case 9001:
				((Activity) mContext).finish();
				break;
			}
		}
	};

	/**
	 * 本Class開啟新的Activity finished後,子Class回傳資料至本 class<BR>
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
			this.finish();
		}
	}

}