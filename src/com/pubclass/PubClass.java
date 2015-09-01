/*
 * general Public Class, use for all project
 * 
 *  GCM API key : AIzaSyDBgNHxDIrq4MFt7p8jnG6PpKtsAHov6IQ
 *  SHA1 : 0F:2D:C2:38:66:27:AF:21:A8:16:68:EA:F3:15:7A:CB:67:D1:57:DC
 *  GooglePlay APP name : com.agenttw 
 * 
 * @date : 2012/02/25
 */

package com.pubclass;

import com.mysoqi.onlineorder.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 常用自訂的 public class<br>
 * general Public Class, use for all project
 */
public class PubClass {
	/** Server 網路連線資料, 路徑+版本 , 與伺服器目錄名稱一致, 版本有更新要修改 */
	public static final String D_WEBURL = "http://218.22.25.203:82/";
	//public static final String D_WEBURL = "http://192.168.110.160:82/";
	
	/**
	 * APP Package name, 外部/內部 裝置儲存資料的目錄<BR>
	 * 直接以 'getExternalFilesDir(null)' 偵測 [資料目錄]<br>
	 * ex. /storage/sdcard0/Android/data/package/files
	 */
	public String D_APPDATAPATH;

	/** 版本更新,資料下載 相關資料 */
	public static final String URL_UPDATE = "http://pub.mysoqi.com/onlineorder_cn/";
	public static final String D_UPDATEFILENAME_VERCODE = "vercode.txt";
	public static final String D_UPDATEFILENAME_APP = "onlineordercn.apk";	

	// 資料檔的實際目錄位置, APP程式內部路徑, ex. data/data/PkgName/
	public String D_PATH_SYSDATA;

	/* 公用的變數或常數 */
	public static final String TAG = "myLOG";

	/* 日期,時間 */
	public int todayYY, todayMM, todayDD, todayHH, todayMIN, todaySS;
	public String[] aryWeekName, aryMonthShortName;

	/** 個人簡單資料檔名稱,以SharedPreferences處理 */
	public static String D_PREFERNAME = "com.onlineordercn_preferences";

	/** 頁面抬頭名稱 */
	public String strPageTitle = "";

	/** 數字格式化 obj */
	private static DecimalFormat objDF;

	/** Screen 長, 寬 */
	public int screenWidth;
	public int screenHeight;

	// common property
	private Context mContext;
	private Resources mResos;
	private String strPkgName;

	/**
	 * general Public Class, use for all project
	 */
	public PubClass(Context context) {
		super();

		// init property
		D_APPDATAPATH = context.getExternalFilesDir(null) + "/";
		mContext = context;
		mResos = mContext.getResources();
		strPkgName = mContext.getPackageName();

		// 設定日期
		Calendar calendar = Calendar.getInstance();
		todayYY = calendar.get(Calendar.YEAR);
		todayMM = calendar.get(Calendar.MONTH) + 1;
		todayDD = calendar.get(Calendar.DAY_OF_MONTH);
		todayHH = calendar.get(Calendar.HOUR_OF_DAY);
		todayMIN = calendar.get(Calendar.MINUTE);
		todaySS = calendar.get(Calendar.SECOND);

		aryWeekName = mContext.getResources().getStringArray(
				R.array.weekname_short);
		aryMonthShortName = mContext.getResources().getStringArray(
				R.array.monthname_short);

		// 字串格式化物件, 僅限本 class 'formatPrice' 使用
		objDF = new DecimalFormat("###,###,###,###,##0.00");

		// 偵測螢幕長寬
		DisplayMetrics displaymetrics = new DisplayMetrics();
		((Activity) mContext).getWindowManager().getDefaultDisplay()
				.getMetrics(displaymetrics);
		screenWidth = displaymetrics.widthPixels;
		screenHeight = displaymetrics.heightPixels;

		// 資料檔實際位置
		D_PATH_SYSDATA = getAppPath() + "/files";
	}

	/**
	 * 動態產生 今天日期 map data, 數值均為 Integer
	 * 
	 * @return Map<String, Integer>
	 */
	public Map<String, Integer> GetToday() {
		Map<String, Integer> mapToday = new HashMap<String, Integer>();
		Calendar calendar = Calendar.getInstance();

		mapToday.put("YY", calendar.get(Calendar.YEAR));
		mapToday.put("MM", calendar.get(Calendar.MONTH) + 1);
		mapToday.put("DD", calendar.get(Calendar.DAY_OF_MONTH));
		mapToday.put("hh", calendar.get(Calendar.HOUR_OF_DAY));
		mapToday.put("mm", calendar.get(Calendar.MINUTE));
		mapToday.put("ss", calendar.get(Calendar.SECOND));

		return mapToday;
	}

	/**
	 * 螢幕 顯示指定文字
	 */
	public static void xxDump(Context context, String text) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show();
	}

	/**
	 * Log message, tag name => myTAG
	 * 
	 * @param strMsg
	 */
	public static void xxLog(String strMsg) {
		Log.v(TAG, strMsg);
	}

	/**
	 * 隱藏視窗標題列與狀態列
	 */
	public void hideWindowHeadStatu(android.view.Window mWindow) {
		// 隱藏狀態列
		/*
		 * mWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		 * WindowManager.LayoutParams.FLAG_FULLSCREEN);
		 */
		// 隱藏標題列
		((Activity) mContext)
				.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
	}

	/**
	 * 檢測外部SD卡狀態
	 * 
	 * @return int : 0 => R/W, 1 => READONLY, -1 => unknown error or not
	 *         available
	 */
	public int statuSDCard() {
		int intRS = -1;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			intRS = 0;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			intRS = 1;
		}
		return intRS;
	}

	/**
	 * 回傳程式版本訊息資料
	 * 
	 * @return HashMap<String, String> : <br>
	 *         ex. ary('name' => ver1.0, 'ver' => '2')
	 */
	public HashMap<String, String> getVersionData() {
		HashMap<String, String> mdata = new HashMap<String, String>();
		mdata.put("name", "0.1");
		mdata.put("ver", "1");

		try {
			PackageInfo pkgInfo = mContext.getPackageManager().getPackageInfo(
					mContext.getPackageName(), 0);
			mdata.put("name", pkgInfo.versionName);
			mdata.put("ver", String.valueOf(pkgInfo.versionCode));
			mdata.put("package", String.valueOf(pkgInfo.packageName));
		} catch (NameNotFoundException e) {
		}

		return mdata;
	}

	/**
	 * 取得目前 App程式所在的位置 (real path)
	 * 
	 * return String
	 */
	public String getAppPath() {
		PackageManager m = mContext.getPackageManager();
		String s = mContext.getPackageName();
		try {
			PackageInfo p = m.getPackageInfo(s, 0);
			s = p.applicationInfo.dataDir;
		} catch (NameNotFoundException e) {
		}

		return s;
	}

	/**
	 * 語系檔輸出對應字串
	 * 
	 * @param strStr
	 *            : String code
	 * 
	 * @return String
	 */
	public String getLang(String strStr) {
		try {
			return mResos.getString(mResos.getIdentifier(strStr, "string",
					strPkgName));
		} catch (NullPointerException e) {
		} catch (NotFoundException e) {
		}

		return "";
	}

	/* 以下為本 project 使用 */

	/**
	 * 產生 HTTP client
	 * 
	 * @return HttpClient
	 */
	public HttpClient getHTTPClient() {

		// set connect timeout
		int timeoutConnection = 5000;
		int timeoutSo = 10000;
		int timeoutGeneral = 300000;

		/* 設置 http 基本參數 */
		HttpParams params = new BasicHttpParams();
		params.setParameter("charset", "UTF-8");

		// 連接超時
		// HttpConnectionParams.setConnectionTimeout(params, timeoutConnection);

		// 請求超時
		// HttpConnectionParams.setSoTimeout(params, timeoutSo);

		// 從連接池中取連接的超時時間
		// ConnManagerParams.setTimeout(params, timeoutGeneral);

		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setUseExpectContinue(params, true);

		SchemeRegistry schReg = new SchemeRegistry();
		schReg.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		schReg.register(new Scheme("https",
				SSLSocketFactory.getSocketFactory(), 443));

		ClientConnectionManager conMgr = new ThreadSafeClientConnManager(
				params, schReg);

		// Error expection
		try {
			return new DefaultHttpClient(conMgr, params);
		} catch (Exception e) {
			return null;
		}

		// return new DefaultHttpClient();
	}

	/**
	 * 產生 HTTPclient POST 的連線參數
	 * 
	 * @param postData
	 *            : <br>
	 *            'acc' => 登入帳號, 'psd' => 登入密碼, <br>
	 *            'page' => page name, 'act' => action name<br>
	 * 
	 * @return List : HTTP post data
	 */
	public List<NameValuePair> getPostData(HashMap<String, String> postData) {
		List<NameValuePair> mPostdata = new ArrayList<NameValuePair>(
				postData.size());
		for (Entry<String, String> aryData : postData.entrySet()) {
			mPostdata.add(new BasicNameValuePair(aryData.getKey(), aryData
					.getValue()));
		}
		return mPostdata;
	}

	/**
	 * 將字串金額格式化, ex. 123,456,789.01
	 * 
	 * @param price
	 *            : ex. 123456789.1
	 * @param int : 原金額 / flag, ex: 1, 若要易讀為'萬元', flag=4
	 * 
	 * @return String
	 */
	public static String formatPrice(String price, int flag) {
		try {
			double mPrice = Double.valueOf(price);
			mPrice = mPrice / flag;

			String strVal = String.valueOf(objDF.format(mPrice));
			// return strVal;

			// 替換字元 '.00' 為空白
			return strVal.replace(".00", "");

		} catch (Exception e) {
			return price;
		}
	}

	/**
	 * 回傳本project 的 SharedPreferences
	 * 
	 * @return : SharedPreferences
	 */
	public SharedPreferences getPref() {
		return mContext.getSharedPreferences(D_PREFERNAME, 0);
	}

	/**
	 * 從SharedPreferences檔案中,取得個人簡單的資料
	 * 
	 * @return HashMap<String, String> :<br>
	 *         ex. acc => 帳號, psd => 密碼, md5id => MD5(帳號),<br>
	 *         saveLogin => 'false', caldate => '20120912',<br>
	 */
	public HashMap<String, String> getUserData() {
		HashMap<String, String> mData = new HashMap<String, String>();

		SharedPreferences perfData = mContext.getSharedPreferences(
				D_PREFERNAME, 0);
		mData.put("acc", perfData.getString("acc", ""));
		mData.put("psd", perfData.getString("psd", ""));
		mData.put("saveLogin",perfData.getString("saveLogin", "N"));

		return mData;
	}

	/**
	 * 檢查網路是否連線
	 * 
	 * @return boolean
	 */
	public boolean haveInternet() {
		boolean result = false;
		ConnectivityManager connManager = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = connManager.getActiveNetworkInfo();

		if (info == null || !info.isConnected()) {
			result = false;
		} else {
			if (!info.isAvailable()) {
				result = false;
			} else {
				result = true;
			}
		}

		return result;
	}

	/**
	 * 動態關閉虛擬鍵盤
	 */
	public void HideKeyboard() {
		InputMethodManager inputManager = (InputMethodManager) mContext
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(((Activity) mContext)
				.getCurrentFocus().getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);
	}

	/**
	 * 取得每頁頂端的 NavyBar View
	 * 
	 * @param strTitle
	 *            : page Title
	 * 
	 * @return View (LinearLayout)
	 */
	public View getTopNavy(String strTitle) {
		// 取得 ItemView
		LinearLayout mView = (LinearLayout) LayoutInflater.from(mContext)
				.inflate(R.layout.view_topnavy, null, false);
		((TextView) mView.findViewById(R.id.tvPageTitle)).setText(strTitle);

		// 回上頁Button
		Button mBtn = (Button) mView.findViewById(R.id.btnGoBackPage);
		mBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				((Activity) mContext).finish();
			}
		});

		return mView;
	}

	/**
	 * 傳入的 DialogBox 執行 close
	 * 
	 * @param dialog
	 *            : DialogBox
	 */
	public lstnDialogBoxClose closeDialogBox(AlertDialog dialog) {
		return new lstnDialogBoxClose(dialog);
	}

	/**
	 * Class, DialogBox 關閉<br>
	 * 通常由 Dialogbox View 的 Button 'close' 執行
	 */
	private class lstnDialogBoxClose implements OnClickListener {
		private AlertDialog mDialog;

		public lstnDialogBoxClose(AlertDialog dialog) {
			mDialog = dialog;
		}

		@Override
		public void onClick(View v) {
			mDialog.dismiss();
		}
	}

	/**
	 * JSON Array 移除指定的 Elements
	 * 
	 * @param idx
	 *            : position 要移除的第幾筆資料
	 * @param from
	 *            : JSONArray object
	 * 
	 * @return : JSONArray
	 */
	public static JSONArray removeJSONArrayElement(final int idx,
			final JSONArray from) {
		final List<JSONObject> objs = JSONasList(from);
		objs.remove(idx);

		final JSONArray ja = new JSONArray();
		for (final JSONObject obj : objs) {
			ja.put(obj);
		}

		return ja;
	}

	/**
	 * removeJSONArrayElement 內使用
	 * 
	 * @param ja
	 *            : JSONArray object
	 * @return : LIST Array data
	 */
	private static List<JSONObject> JSONasList(final JSONArray ja) {
		final int len = ja.length();
		final ArrayList<JSONObject> result = new ArrayList<JSONObject>(len);
		for (int i = 0; i < len; i++) {
			final JSONObject obj = ja.optJSONObject(i);
			if (obj != null) {
				result.add(obj);
			}
		}

		return result;
	}
	
	/**
	 * 日期格式化為 YYYY/MM/DD 13:01
	 * 
	 * @return String
	 */
	public String formatYYMMDD(String strVal, int intVal) {
		String strYYMMDD = strVal.substring(0, 4) + "/"
				+ strVal.substring(4, 6) + "/" + strVal.substring(6, 8);
		
		if (intVal <=8) {
			return strYYMMDD;
		}
		
		try {
			String strHHmm = strVal.substring(8, 10) + ":"
					+ strVal.substring(10, 12);
			return strYYMMDD + " " + strHHmm;
		} catch (Exception e) {			
		}
		
		return strYYMMDD;
	}

}