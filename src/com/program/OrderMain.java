package com.program;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.mysoqi.onlineorder.R;
import com.pubclass.PubClass;

public class OrderMain extends Activity {
	private Context mContext;
	private PubClass pubClass;
	private Resources mResos;
	private String strPkg;
	private ProgressDialog mProgDialog;
	private HashMap<String, String> aryUser;

	// 商品相關參數設定
	private JSONObject jobjAllPd; // 全部大類的商品資料
	private String[] aryPdCategory = { "S", "C", "N", "H", "P", "B" }; // 商品分類代碼
	private List<JSONObject> listCart; // 購物資料, JSONObject=> 'id', 'qty'
	private int maxQty = 99;
	private String strPdCategory; // 目前點選商品的大類
	private String strQtyType; // 目前數量view的性質, 新增或是更新
	private int intAmountPrice = 0, intAmountPv = 0;

	// 彈出視窗相關
	private View mPopViewPd; // 彈出視窗的 main view, 商品
	private AlertDialog mDialogViewPd; // 彈出視窗的 AlertDialog, 商品
	private AlertDialog.Builder mBuilderPd; // DailogView 的 Builder, 商品

	private View mPopViewQty; // 彈出視窗的 main view, 數量選擇
	private AlertDialog mDialogViewQty; // 彈出視窗的 AlertDialog, 數量選擇

	// ListView 相關, 商品, 購物車
	private Integer positionPd = -1, positionCart = -1, positionQty = 0;
	private ListView lvPd, lvCart;

	// layout field
	private TextView tvAmountPrice, tvAmountPv;
	
	// 會員資料, 'description', 'tel_h', 'tel_h', 'm_phone', 'office_code'
	private JSONObject jobjMember; 

	// 會員資料, 根據營業處別顯示客服電話
	private LinearLayout lyMemberData;
	private TextView tvMemberName, tvCsttel;	
	
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
		setContentView(R.layout.order_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);		

		Intent mIntent = this.getIntent();
		
		// 取得前頁面傳入的'商品資料', 會員資料		
		try {
			jobjAllPd = new JSONObject(mIntent.getStringExtra("jobjPd"));
			jobjMember = new JSONObject(mIntent.getStringExtra("jobjMember"));
		} catch (Exception e) {
			((Activity) mContext).finish();

			return;
		}
		
		// 2015/04/17
		aryUser = pubClass.getUserData();
		//aryUser.put("acc", mIntent.getStringExtra("acc"));
		//aryUser.put("psd", mIntent.getStringExtra("psd"));		

		// 進度與提示 DialogBox
		mProgDialog = new ProgressDialog(mContext);
		mProgDialog.setTitle(mContext.getString(R.string.confirm_dialbox));
		mProgDialog.setMessage(mContext.getString(R.string.datatransfering));
		mProgDialog.setCancelable(false);

		// 其他參數設定
		listCart = new ArrayList<JSONObject>();

		// 彈出視窗 Builder 建立
		mBuilderPd = new AlertDialog.Builder(mContext);
		mBuilderPd.setCancelable(false);
		
		// Layout property 設定
		tvAmountPrice = (TextView) this.findViewById(R.id.tvAmountPrice);
		tvAmountPv = (TextView) this.findViewById(R.id.tvAmountPv);
		lvCart = (ListView) this.findViewById(R.id.lvCart);
		tvCsttel = (TextView) this.findViewById(R.id.tvCsttel);
		tvMemberName = (TextView) this.findViewById(R.id.tvMemberName);
		lyMemberData = (LinearLayout) this.findViewById(R.id.lyMemberData);
		
		// 會員資料, 根據營業處別顯示客服電話
		try {
			tvCsttel.setText(mResos.getIdentifier("csttelmsg_" + jobjMember.optString("office_code"), "string", strPkg));
			tvMemberName.setText(jobjMember.optString("description"));
		} catch (Exception e) {
			lyMemberData.setVisibility(View.GONE);
		}

		// top NavyBar View
		LinearLayout vTopNavy = (LinearLayout) pubClass
				.getTopNavy(getString(R.string.appfullname));
		((Button) vTopNavy.findViewById(R.id.btnGoBackPage)).setText(mContext
				.getString(R.string.logout));
		((LinearLayout) findViewById(R.id.lyTopNavy)).addView(vTopNavy);

		// 本頁面資料送出 submit button
		Button btnSubmit = (Button) this.findViewById(R.id.btnSubmit);
		btnSubmit.setOnClickListener(new lstnDataSave());

		// 商品大類 Button click 設定
		for (String strKey : aryPdCategory) {
			Button btnPd = (Button) this.findViewById(mResos.getIdentifier(
					"btnPd_" + strKey, "id", strPkg));
			btnPd.setOnClickListener(new lstnPdCategory(strKey) {

			});
		}
		
		// 清空購物車 button
		Button btnEmpty = (Button) this.findViewById(R.id.btnEmpty);
		btnEmpty.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				listCart = new ArrayList<JSONObject>();
				positionPd = -1;
				positionCart = -1;
				positionQty = 0;
				setListViewCart();
				
				PubClass.xxDump(mContext, mContext.getString(R.string.emptycartmsg));
			}			
		});		
	}

	/**
	 * 商品大類 Button click listener
	 */
	private class lstnPdCategory implements OnClickListener {
		private String strCategory;

		public lstnPdCategory(String str) {
			strCategory = str;
		}

		@Override
		public void onClick(View v) {
			// 彈出商品選擇視窗
			strPdCategory = strCategory;

			// 無商品
			try {
				if (jobjAllPd.getJSONArray(strPdCategory).length() > 0) {
					showPopWindowView();
					return;
				}
			} catch (Exception e) {
			}

			PubClass.xxDump(mContext, mContext.getString(R.string.nopd));
		}
	}

	/**
	 * 商品彈出視窗
	 * 
	 * @param : strPdCategory
	 */
	private void showPopWindowView() {
		// 設定 popView 的 field
		mPopViewPd = (LinearLayout) LayoutInflater.from(mContext).inflate(
				R.layout.view_poppdlist, null, false);
		lvPd = (ListView) mPopViewPd.findViewById(R.id.lvPd);
		TextView tvTitle = (TextView) mPopViewPd
				.findViewById(R.id.tvPdCategory);
		tvTitle.setText(pubClass.getLang("pdcategory_" + strPdCategory));

		setListViewPd();

		// 關閉 button
		Button btnClose = (Button) mPopViewPd.findViewById(R.id.btnClose);
		btnClose.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mDialogViewPd.dismiss();
			}
		});

		// 將 Builder 設訂到 popView (AlertDialogView)並顯示
		mBuilderPd.setView(mPopViewPd);
		mDialogViewPd = mBuilderPd.create();
		mDialogViewPd.show();

		return;
	}

	/**
	 * 設定 '商品' ListView
	 */
	private void setListViewPd() {
		// 設定 ListView adpat
		JSONArray jaryData;
		try {
			jaryData = jobjAllPd.getJSONArray(strPdCategory);
		} catch (Exception e) {
			lvPd.setAdapter(null);
			lvPd.setOnItemClickListener(null);
			return;
		}

		JSONObject jobjItem = null;
		List<HashMap<String, String>> listAdapter = new ArrayList<HashMap<String, String>>();

		for (int loopi = 0; loopi < jaryData.length(); loopi++) {
			jobjItem = jaryData.optJSONObject(loopi);

			// 設定對應的 Map
			HashMap<String, String> item = new HashMap<String, String>();
			item.put("name", jobjItem.optString("description"));
			item.put("id", jobjItem.optString("id"));
			item.put("price",
					PubClass.formatPrice(jobjItem.optString("price_mem"), 1));
			item.put("price_member",
					PubClass.formatPrice(jobjItem.optString("price_dir"), 1));
			item.put("price_retail",
					PubClass.formatPrice(jobjItem.optString("price"), 1));
			item.put("pv", jobjItem.optString("pv"));

			// Item的 Adapter 加入 map對應資料
			listAdapter.add(item);
		}

		// 產生 SimpleAdapter
		SimpleAdapter adptPd = new SimpleAdapter(mContext, listAdapter,
				R.layout.item_pd, new String[] { "name", "id", "price",
						"price_member", "price_retail", "pv" }, new int[] {
						R.id.tvPdName, R.id.tvPdId, R.id.tvPdPrice,
						R.id.tvPdPrice_member, R.id.tvPdPrice_retail,
						R.id.tvPdPv });

		lvPd.setAdapter(adptPd);

		// 商品的 Item onClick listener
		lvPd.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				// 設定 Item Select background
				view.setSelected(true);

				positionPd = position;
				positionQty = 0;

				// 檢查購物車是否有已經選擇的商品
				if (listCart.size() > 0) {
					String strPdId = jobjAllPd.optJSONArray(strPdCategory)
							.optJSONObject(positionPd).optString("id");
					for (int i = 0; i < listCart.size(); i++) {
						if (strPdId.equalsIgnoreCase(listCart.get(i)
								.optJSONObject("pd").optString("id"))) {

							PubClass.xxDump(mContext, mContext
									.getString(R.string.cartpdexistsmsg));
							return;
						}
					}
				}

				strQtyType = "add";

				/** 商品的 Item 點取, 彈出數量選擇視窗 */
				// 設定 popView 的 field
				mPopViewQty = (LinearLayout) LayoutInflater.from(mContext)
						.inflate(R.layout.view_poppdqty, null, false);

				// 設定數量 ListView
				ListView lvQty = (ListView) mPopViewQty
						.findViewById(R.id.lvQty);
				setListViewQty(lvQty);

				Button btnRemove = (Button) mPopViewQty
						.findViewById(R.id.btnRemove);
				btnRemove.setVisibility(View.GONE);

				// 取消 button
				Button btnCancel = (Button) mPopViewQty
						.findViewById(R.id.btnCancel);
				btnCancel.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mDialogViewQty.dismiss();
					}
				});

				// 數量確認 button
				Button btnQtySubmit = (Button) mPopViewQty
						.findViewById(R.id.btnQtySubmit);
				btnQtySubmit.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						// 取得目前選擇的數量 value
						String strQty = String.valueOf(positionQty + 1);

						/** 商品加入 購物車 JSONArray */
						// ListCart 資料新增, type='add'
						try {
							JSONObject jobjSelPd = jobjAllPd.getJSONArray(
									strPdCategory).getJSONObject(positionPd);
							if (strQtyType.equalsIgnoreCase("add")) {
								JSONObject jobjCart = new JSONObject();
								jobjCart.put("pd", jobjSelPd);
								jobjCart.put("qty", strQty);

								// 重新整理 listCart, 新的商品最前面
								int nums = listCart.size();
								if (nums < 1) {
									listCart.add(jobjCart);
								} else {
									listCart.add(listCart.get(nums - 1));
									for (int loopi = nums - 1; loopi >= 1; loopi--) {
										listCart.set(loopi,
												listCart.get(loopi - 1));
									}
									listCart.set(0, jobjCart);
								}
							}
							// ListCart 更新數量資料 type='update'
							else if (strQtyType.equalsIgnoreCase("update")) {
								(listCart.get(positionCart)).put("qty", strQty);
							}
						} catch (Exception e) {
						}

						// ListView Cart 資料重新 reload
						setListViewCart();

						// 全部的 popWindow close
						mDialogViewQty.dismiss();
						PubClass.xxDump(mContext,
								mContext.getString(R.string.addcompleted));
					}
				});

				// 將 Builder 設訂到 popView (AlertDialogView)並顯示
				AlertDialog.Builder mBuilderQty = new AlertDialog.Builder(
						mContext);
				mBuilderQty.setCancelable(false);
				mBuilderQty.setView(mPopViewQty);
				mDialogViewQty = mBuilderQty.create();
				mDialogViewQty.show();

				return;
			}
		});
	}

	/**
	 * 設定 '數量' ListView
	 * 
	 * @param mListView
	 *            : 數量的 ListView
	 */
	private void setListViewQty(ListView mListView) {
		// 設定商品其他資料
		TextView tvPdName = (TextView) mPopViewQty.findViewById(R.id.tvPdName);
		TextView tvPrice = (TextView) mPopViewQty.findViewById(R.id.tvPrice);
		TextView tvPriceSub = (TextView) mPopViewQty
				.findViewById(R.id.tvPriceSub);
		TextView tvPv = (TextView) mPopViewQty.findViewById(R.id.tvPv);
		TextView tvPvSub = (TextView) mPopViewQty.findViewById(R.id.tvPvSub);
		TextView tvCurrQty = (TextView) mPopViewQty.findViewById(R.id.tvCurrQty);

		try {
			int Qty = positionQty + 1;
			JSONObject jobjCurrPd;
			if (strQtyType.equalsIgnoreCase("add")) {
				jobjCurrPd = jobjAllPd.getJSONArray(strPdCategory)
						.getJSONObject(positionPd);
			}
			else {
				jobjCurrPd = listCart.get(positionCart).getJSONObject("pd");
			}
			
			tvPdName.setText(jobjCurrPd.getString("description"));
			tvPrice.setText(jobjCurrPd.getString("price_mem"));			
			tvPv.setText(jobjCurrPd.getString("pv"));			
			
			int subPrice = Integer.valueOf(jobjCurrPd.optString("price_mem")) * Qty;
			int subPv = Integer.valueOf(jobjCurrPd.optString("pv"))	* Qty;
			tvPriceSub.setText(String.valueOf(subPrice));			
			tvPvSub.setText(String.valueOf(subPv));
			tvCurrQty.setText(String.valueOf(Qty));
		} catch (Exception e) {
		}

		List<HashMap<String, String>> listAdapter = new ArrayList<HashMap<String, String>>();
		for (int loopi = 1; loopi <= maxQty; loopi++) {

			// 設定對應的 Map
			HashMap<String, String> item = new HashMap<String, String>();
			item.put("qty", String.valueOf(loopi));

			// Item的 Adapter 加入 map對應資料
			listAdapter.add(item);
		}

		// 產生 SimpleAdapter
		SimpleAdapter adptQty = new SimpleAdapter(mContext, listAdapter,
				R.layout.item_qty, new String[] { "qty" },
				new int[] { R.id.tvQty });

		mListView.setAdapter(adptQty);
		mListView.setSelection(positionQty);

		// 數量 ListView Item onClick listener
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				positionQty = position;

				try {
					// 取得目前選擇的商品 JSONObject
					JSONObject jobjSelPd;
					if (strQtyType.equalsIgnoreCase("add")) {
						jobjSelPd = jobjAllPd.getJSONArray(strPdCategory)
								.getJSONObject(positionPd);
					}
					else {
						jobjSelPd = listCart.get(positionCart).getJSONObject("pd");
					}

					// 單價, PV 合計處理
					int subPrice = Integer.valueOf(jobjSelPd
							.optString("price_mem")) * (position + 1);
					int subPv = Integer.valueOf(jobjSelPd.optString("pv"))
							* (position + 1);
					((TextView) mPopViewQty.findViewById(R.id.tvPriceSub))
							.setText(String.valueOf(subPrice));
					((TextView) mPopViewQty.findViewById(R.id.tvPvSub))
							.setText(String.valueOf(subPv));

					((TextView) mPopViewQty.findViewById(R.id.tvCurrQty))
							.setText(String.valueOf((position + 1)));

				} catch (Exception e) {
				}
			}
		});
	}

	/**
	 * 設定 '購物車' ListView
	 */
	private void setListViewCart() {
		if (listCart.size() < 1) {
			lvCart.setAdapter(null);
			lvCart.setOnItemClickListener(null);
			tvAmountPrice.setText("0");
			tvAmountPv.setText("0");
		}

		List<HashMap<String, String>> listAdapter = new ArrayList<HashMap<String, String>>();
		JSONObject jobjItem = new JSONObject();
		String strQty;
		int intQty = 0;
		int intSubPrice = 0, intSubPv = 0;
		intAmountPrice = 0;
		intAmountPv = 0;

		for (int loopi = 0; loopi < listCart.size(); loopi++) {
			jobjItem = listCart.get(loopi).optJSONObject("pd");
			strQty = listCart.get(loopi).optString("qty");

			// 設定對應的 Map
			HashMap<String, String> item = new HashMap<String, String>();
			item.put("name", jobjItem.optString("description"));
			item.put("id", jobjItem.optString("id"));
			item.put("price",
					PubClass.formatPrice(jobjItem.optString("price_mem"), 1));
			item.put("price_member",
					PubClass.formatPrice(jobjItem.optString("price_dir"), 1));
			item.put("price_retail",
					PubClass.formatPrice(jobjItem.optString("price"), 1));
			item.put("pv", jobjItem.optString("pv"));

			// 重新計算加總金額
			intQty = Integer.valueOf(strQty);
			intSubPrice = intQty
					* (Integer.valueOf(jobjItem.optString("price_mem")));
			intSubPv = intQty * (Integer.valueOf(jobjItem.optString("pv")));

			intAmountPrice += intSubPrice;
			intAmountPv += intSubPv;

			item.put("qty", strQty);
			item.put("subprice", String.valueOf(intSubPrice));
			item.put("subpv", String.valueOf(intSubPv));

			// Item的 Adapter 加入 map對應資料
			listAdapter.add(item);
		}

		// 設定本頁面，出貨總金額與PV
		tvAmountPrice.setText(String.valueOf(intAmountPrice));
		tvAmountPv.setText(String.valueOf(intAmountPv));

		// 產生 SimpleAdapter
		SimpleAdapter adptCart = new SimpleAdapter(mContext, listAdapter,
				R.layout.item_cart, new String[] { "name", "id", "price",
						"price_member", "price_retail", "pv", "qty",
						"subprice", "subpv" }, new int[] { R.id.tvPdName,
						R.id.tvPdId, R.id.tvPdPrice, R.id.tvPdPrice_member,
						R.id.tvPdPrice_retail, R.id.tvPdPv, R.id.tvPdQty,
						R.id.tvPdPriceSub, R.id.tvPdPvSub });

		lvCart.setAdapter(adptCart);
		lvCart.setSelection(positionCart);

		// ListView cart Item 的 click listener
		lvCart.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				positionCart = position;
				strQtyType = "update";

				// 設定該商品以選擇的數量 position
				try {
					positionQty = Integer.valueOf(listCart.get(positionCart)
							.getString("qty")) - 1;
				} catch (Exception e) {
				}

				/** 商品的 Item 點取, 彈出數量選擇視窗 */
				// 設定 popView 的 field
				mPopViewQty = (LinearLayout) LayoutInflater.from(mContext)
						.inflate(R.layout.view_poppdqty, null, false);
				ListView lvQty = (ListView) mPopViewQty
						.findViewById(R.id.lvQty);
				setListViewQty(lvQty);

				// 取消 button
				Button btnCancel = (Button) mPopViewQty
						.findViewById(R.id.btnCancel);
				btnCancel.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mDialogViewQty.dismiss();
					}
				});

				// 移除商品 button
				Button btnRemove = (Button) mPopViewQty
						.findViewById(R.id.btnRemove);
				btnRemove.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						// ListCart 移除指定 position 資料, lvCart reload
						List<JSONObject> listNewData = new ArrayList<JSONObject>();

						for (int i = 0; i < listCart.size(); i++) {
							if (positionCart != i)
								listNewData.add(listCart.get(i));
						}

						listCart = listNewData;

						strQtyType = "remove";
						setListViewCart();

						mDialogViewQty.dismiss();
					}
				});

				// 數量確認 button
				Button btnQtySubmit = (Button) mPopViewQty
						.findViewById(R.id.btnQtySubmit);
				btnQtySubmit.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						// 取得目前選擇的數量 value
						String strQty = String.valueOf(positionQty + 1);

						// ListCart 資料更新增, type='update'
						try {
							(listCart.get(positionCart)).put("qty", strQty);
						} catch (Exception e) {
						}

						// ListView Cart 資料重新 reload
						setListViewCart();

						// 全部的 popWindow close
						mDialogViewQty.dismiss();
						// mDialogViewPd.dismiss();
					}
				});

				// 將 Builder 設訂到 popView (AlertDialogView)並顯示
				AlertDialog.Builder mBuilderQty = new AlertDialog.Builder(
						mContext);
				mBuilderQty.setCancelable(false);
				mBuilderQty.setView(mPopViewQty);
				mDialogViewQty = mBuilderQty.create();
				mDialogViewQty.show();

				return;
			}
		});
	}

	/**
	 * 本頁面資料儲存 Submit button click listener
	 */
	private class lstnDataSave implements OnClickListener {
		@Override
		public void onClick(View v) {
			// 本頁面輸入的資料檢查, 購物車有無商品
			if (listCart.size() < 1) {
				PubClass.xxDump(mContext,
						mContext.getString(R.string.err_cartnopdmsg));
				return;
			}

			// 重新產生購物車商品，只需要 id, qty
			JSONArray jaryCart = new JSONArray();

			try {
				for (int i = 0; i < listCart.size(); i++) {
					JSONObject jobjPd = new JSONObject();
					jobjPd.put("id", listCart.get(i).getJSONObject("pd")
							.optString("id"));
					jobjPd.put("qty", listCart.get(i).getString("qty"));
					jaryCart.put(i, jobjPd);
				}
			} catch (Exception e) {
				PubClass.xxDump(mContext,
						mContext.getString(R.string.err_sysdataio));

				((Activity) mContext).finish();
				return;
			}

			// 彈出確認視窗
			AlertDialog.Builder mBuilder = new AlertDialog.Builder(mContext);
			mBuilder.setTitle(R.string.systemnotify).setCancelable(false);
			mBuilder.setMessage(R.string.mksuresendordermsg);

			lstnSaveConfirm mLstn = new lstnSaveConfirm(jaryCart.toString());
			mBuilder.setPositiveButton(R.string.submit, mLstn);
			mBuilder.setNegativeButton(R.string.cancel, mLstn);

			AlertDialog mDialog = mBuilder.create();
			mDialog.show();
		}
	}

	/**
	 * 資料存檔確認 彈出視窗
	 */
	private class lstnSaveConfirm implements DialogInterface.OnClickListener {
		private String strJson;

		/**
		 * 
		 * @param str
		 *            : 已經整理好的 商品 ('id', 'qty') JSONArray string
		 */
		public lstnSaveConfirm(String str) {
			strJson = str;
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			dialog.cancel();

			// 取消 button
			if (which == DialogInterface.BUTTON_NEGATIVE) {
				return;
			}

			PubClass.xxLog(strJson);

			// 產生 _REQUEST 的 JSON string, HTTP 連線傳送資料
			HashMap<String, String> postData = new HashMap<String, String>();
			postData.put("id", aryUser.get("acc"));
			postData.put("pw", aryUser.get("psd"));
			postData.put("module", "onlineorder");
			postData.put("pd", strJson);

			// PubClass.xxLog(strArg0);
			new _HttpConnSend().execute(postData);

			return;
		}
	}

	/**
	 * Class, HTTP 連線
	 */
	private class _HttpConnSend extends
			AsyncTask<HashMap<String, String>, Void, String> {

		protected void onPreExecute() {
			mProgDialog.show();
		}

		protected String doInBackground(HashMap<String, String>... mPostData) {
			HttpPost httppost = new HttpPost(PubClass.D_WEBURL);
			HttpClient mHttpclient = pubClass.getHTTPClient();

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
			} catch (Exception e) {
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
	 * ary('result'=>boolean, msg=>'errmsg' or NULL)
	 * 
	 * @param strRespon
	 */
	private void chkRespon(String strRespon) {
		mProgDialog.dismiss();

		// 解析回傳的 JSON String to JSON array data
		JSONObject jobjRoot = null;

		try {
			jobjRoot = new JSONObject(strRespon);
		} catch (Exception e) {
			PubClass.xxDump(mContext,
					mContext.getString(R.string.err_onlineodrtransfer));

			return;
		}

		// 認證結果
		String strMsg = pubClass.getLang(jobjRoot.optString("msg"));
		if (jobjRoot.optBoolean("result") != true) {
			if (strMsg.length() < 1)
				strMsg = mContext.getString(R.string.loginfailure);

			PubClass.xxDump(mContext, strMsg);

			return;
		}

		// 相關資料清除
		listCart = new ArrayList<JSONObject>();
		setListViewCart();

		// 回傳成功訊息
		String strDialogMsg = String.format(
				mContext.getString(R.string.onlinesuccessmsg),
				jobjRoot.optString("msg"));

		AlertDialog.Builder mBuilder = new AlertDialog.Builder(mContext);
		mBuilder.setPositiveButton(R.string.confirmisee,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();

						return;
					}
				});

		AlertDialog mDialog = mBuilder.create();
		mDialog.setTitle(R.string.confirm_dialbox);
		mDialog.setCancelable(false);
		mDialog.setMessage(strDialogMsg);
		mDialog.show();

		return;
	}

}