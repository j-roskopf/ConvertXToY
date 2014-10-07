package com.roskopf.cata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.UnsupportedEncodingException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.roskopf.cata.wav.WavFileHelper;

@SuppressLint("NewApi")
public class MainActivity extends ActionBarActivity {

	// private final String API_KEY =
	// "DTDmcd4rirbgFTHX-m9JMbtyn-QuWWy_OT0zOuxA11KBWkgxR5Q7fQFLrmMcbnRs25YX17c-g8UaLCD9_07QoA";
	private final String API_KEY = "DTDmcd4rirbgFTHX-m9JMbtyn-QuWWy_OT0zOuxA11KBWkgxR5Q7fQFLrmMcbnRs25YX17c-g8UaLCD9_07QoA";

	Button selectFileButton;
	TextView fileName;
	Spinner convertSpinner;
	Button convert;
	TextView error;
	TextView status;
	TextView savedFilePath;

	boolean mReturningWithResult;

	String path;

	Context mContext;

	Uri selectedUri;

	private static final int REQUEST_CHOOSER = 1234;

	com.roskopf.cata.process.Process currentProcess;

	String inputFormat;
	String outputFormat;
	String name;

	final int ACTIVITY_CHOOSE_FILE = 1;

	AQuery aq;
	// Displays progress
	private ProgressDialog mPrgDialog;

	// Holds the posts
	private List<String> convertToFileList;

	// Call back object for AQuery
	AjaxCallback<JSONArray> mCallBack;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mContext = this;
		aq = new AQuery(this);
		currentProcess = new com.roskopf.cata.process.Process();

		convertToFileList = new ArrayList<String>();
		savedFilePath = (TextView) findViewById(R.id.savedFilePath);
		status = (TextView) findViewById(R.id.status);
		fileName = (TextView) findViewById(R.id.fileName);
		error = (TextView) findViewById(R.id.error);
		convertSpinner = (Spinner) findViewById(R.id.convertSpinner);
		convert = (Button) this.findViewById(R.id.convertAndSave);
		convert.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Send call to get user information

				outputFormat = convertSpinner.getSelectedItem().toString();

				String urlToCall = "https://api.cloudconvert.org/process";

				AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>() {

					@Override
					public void callback(String url, JSONObject response,
							AjaxStatus status) {

						if (response != null) {

							try {
								currentProcess.setURL(response.getString("url"));
								currentProcess.setId(response.getString("id"));
								currentProcess.setHost(response
										.getString("host"));
								currentProcess.setExpires(response
										.getString("expires"));
								currentProcess.setMaxtime(response
										.getString("maxtime"));
								currentProcess.setMinutes(response
										.getString("minutes"));
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							start();

						} else {
							Toast.makeText(getApplicationContext(),
									"There was a conversion error",
									Toast.LENGTH_SHORT).show();
						}

					}
				};

				cb.method(AQuery.METHOD_POST);
				cb.url(urlToCall).type(JSONObject.class);

				Map<String, Object> params = new HashMap<String, Object>();

				params.put("apikey", API_KEY);
				params.put("inputformat", inputFormat);
				params.put("outputformat", outputFormat);

				cb.params(params);

				mPrgDialog = new ProgressDialog(mContext);
				mPrgDialog.setCancelable(false);
				mPrgDialog.setTitle("Starting conversion process");
				aq.progress(mPrgDialog).ajax(cb);

			}
		});
		selectFileButton = (Button) this.findViewById(R.id.selectFile);
		selectFileButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				// Create the ACTION_GET_CONTENT Intent
				Intent getContentIntent = FileUtils.createGetContentIntent();

				Intent intent = Intent.createChooser(getContentIntent,
						"Select a file");
				startActivityForResult(intent, REQUEST_CHOOSER);
			}
		});
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		if (mReturningWithResult) {
			// Commit your transactions here.
		}
		// Reset the boolean flag back to false for next time.
		mReturningWithResult = false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CHOOSER: {
			if (resultCode == RESULT_OK) {

				super.onActivityResult(requestCode, resultCode, data);
				mReturningWithResult = true;

				savedFilePath.setText("");
				status.setText("");
				error.setText("");

				selectedUri = data.getData();

				if (selectedUri != null) {



					Cursor returnCursor = getContentResolver().query(
							selectedUri, null, null, null, null);

					/*
					 * Get the column indexes of the data in the Cursor, move to
					 * the first row in the Cursor, get the data, and display
					 * it.
					 */

					if (returnCursor != null) {

						convert.setEnabled(true);

						int nameIndex = returnCursor
								.getColumnIndex(OpenableColumns.DISPLAY_NAME);
						returnCursor.moveToFirst();

						name = returnCursor.getString(nameIndex);
						
						ContentResolver cR = this.getContentResolver();
						MimeTypeMap mime = MimeTypeMap.getSingleton();
						inputFormat = mime.getExtensionFromMimeType(cR
								.getType(selectedUri));
						if(inputFormat == null)
						{
							inputFormat = name.substring(name.lastIndexOf(".")+1);
						}
						Log.d("yeah", "it's not null null");


						Log.d("naame", name);

						// Get the File path from the Uri
						path = FileUtils.getPath(this, selectedUri);

						fileName.setText(path);

					}

					if (path == null
							&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
						error.setVisibility(View.VISIBLE);
						error.setText("Sorry, it looks like you're on KitKat and trying to get a file from the sd card, which KitKat does not allow. Moving the file to internal storage will fix this problem");
						convert.setEnabled(false);

					} else if (path == null
							&& Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
						error.setVisibility(View.VISIBLE);
						fileName.setText("Unable to determine location of file. Sorry about that.");
						convert.setEnabled(false);
					} else if (path != null) {
						error.setVisibility(View.INVISIBLE);
					}

					String urlToCall = "https://api.cloudconvert.org/conversiontypes?inputformat="
							+ inputFormat;

					Log.d("checking input format", inputFormat);

					// Send call to get user information
					AjaxCallback<JSONArray> cb = new AjaxCallback<JSONArray>();
					cb.url(urlToCall).type(JSONArray.class)
							.weakHandler(this, "jsonCallback");

					mPrgDialog = new ProgressDialog(this);
					mPrgDialog.setCancelable(false);
					mPrgDialog.setTitle("Gathering conversion types");
					// Display Progress Dialog Bar by invoking progress method
					aq.progress(mPrgDialog).ajax(cb);
				}

			}
		}
		}

	}

	public void jsonCallback(String url, JSONArray json, AjaxStatus status)
			throws JSONException, ParseException {
		if (json != null) {
			convertToFileList = new ArrayList<String>();
			for (int i = 0; i < json.length(); i++) {
				JSONObject currentObject = json.getJSONObject(i);
				if (!currentObject.getString("outputformat").equals("wma")) {
					convertToFileList.add(currentObject
							.getString("outputformat"));
				}
			}

			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					R.layout.spinner_text_view, convertToFileList);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			convertSpinner.setAdapter(adapter);
		} else {
			Toast.makeText(this, "There was a network error",
					Toast.LENGTH_SHORT).show();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		return super.onOptionsItemSelected(item);
	} 

	private void start() {
		new LongOperation().execute();

	}

	private byte[] convertFileToByte(File file) { 
		byte[] b = new byte[(int) file.length()];
		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			fileInputStream.read(b);

		} catch (FileNotFoundException e) {
			System.out.println("File Not Found.");
			e.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Error Reading The File.");
			e1.printStackTrace();
		}

		return b;
	}

	private class LongOperation extends AsyncTask<String, String, String> {

		private ProgressDialog prgs;

		@Override
		protected String doInBackground(String... params1) {
			publishProgress("Preparing file");

			String urlToCall = "https://api.cloudconvert.org/convert?apikey="
					+ API_KEY + "&input=upload&download=inline&inputformat="
					+ inputFormat + "&outputformat=" + outputFormat;

			// Log.d("checking the path", path);
			File fileToSend = new File(path);
			byte[] b = convertFileToByte(fileToSend);
			// Log.d("file", path);
			FileInputStream fileInputStream1 = null;
			try {
				fileInputStream1 = new FileInputStream(fileToSend);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			InputStream fileInputStream = fileInputStream1; // Your file stream
			String fileName = name + "." + inputFormat;
			String fileKey = "file";
			// HashMap<String, String> headerparts = mHeaderParts; //Other
			// header
			// parts that you need to send along.

			HttpClient httpClient = new DefaultHttpClient();

			List<NameValuePair> params = new ArrayList<NameValuePair>();

			params.add(new BasicNameValuePair("download", "true"));
			params.add(new BasicNameValuePair("wait", "true"));
			params.add(new BasicNameValuePair("outputformat", outputFormat));
			params.add(new BasicNameValuePair("apikey", API_KEY));
			params.add(new BasicNameValuePair("inputformat", inputFormat));
			params.add(new BasicNameValuePair("download", "true"));

			HttpPost httpPost = new HttpPost(urlToCall);

			try {
				httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			publishProgress("Uploading file");
			SimpleMultipartEntity entity = new SimpleMultipartEntity();
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "multipart/form-data; boundary="
					+ entity.getBoundary());
			entity.writeFirstBoundaryIfNeeds();

			entity.addPart(fileKey, fileName, fileInputStream);
			entity.writeLastBoundaryIfNeeds();
			httpPost.setEntity(entity);

			try {
				publishProgress("Converting file");
				HttpResponse response = httpClient.execute(httpPost);

				publishProgress("Downloading file");
				ByteArrayOutputStream baos = new ByteArrayOutputStream(
						Integer.parseInt(response.getEntity()
								.getContentLength() + ""));

				response.getEntity().writeTo(baos);

				byte[] bytes = baos.toByteArray();
				if (outputFormat.equals("wav")) {
					WavFileHelper wvh = new WavFileHelper(path.substring(0,
							path.lastIndexOf(".")) + "." + outputFormat, bytes);
				} else if (outputFormat.equals("mp3")) {
					// Log.d("making sure i'm in mp3",bytes.length+"");
					// convertByteToFile(bytes);
					String currentPath = path.substring(0,
							path.lastIndexOf("/"));
					FileOutputStream fos = new FileOutputStream(currentPath
							+ "/" + stripExtension(name) + "." + outputFormat);
					fos.write(bytes);
					fos.close();

				} else {
					convertByteToFile(bytes);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return "Executed";
		}

		@Override
		protected void onPostExecute(String result) {
			prgs.dismiss();
			status.setText("Success! Saved to:");
			savedFilePath.setText(path.substring(0, path.lastIndexOf("."))
					+ "." + outputFormat);
			// Log.d("plz","maa");
		}

		@Override
		protected void onPreExecute() {

			prgs = new ProgressDialog(mContext);
			prgs.setCancelable(false);
			prgs.setTitle("Uploading, Converting, Downloading");
			prgs.show();
		}

		@Override
		protected void onProgressUpdate(String... values) {
			// Log.d("progress",values[0]+"");
			prgs.setMessage(values[0]);
		}
	}

	private void convertByteToFile(byte[] b) {

		FileOutputStream fop = null;
		// Log.d("checking this1",path);
		// Log.d("checking this12",path.substring(0,path.lastIndexOf("/")));
		String currentPath = path.substring(0, path.lastIndexOf("/"));
		// Log.d("currentPath",currentPath);

		File file;
		try {
			// Log.d("saving as this schtuff",currentPath+"/"+stripExtension(name)+"."+outputFormat);
			file = new File(currentPath + "/" + stripExtension(name) + "."
					+ outputFormat);
			fop = new FileOutputStream(file, true);
			fop.write(b);
			fop.flush();
			fop.close();
		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			try {
				if (fop != null) {
					fop.close();
				}
			} catch (IOException e) {
				e.printStackTrace();

			}
		}
	}

	public String stripExtension(String str) {
		// Handle null case specially.

		if (str == null)
			return null;

		// Get position of last '.'.

		int pos = str.lastIndexOf(".");

		// If there wasn't any '.' just return the string as is.

		if (pos == -1)
			return str;

		// Otherwise return the string, up to the dot.

		return str.substring(0, pos);
	}

}
