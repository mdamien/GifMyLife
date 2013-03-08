package org.cuju.gifmylife;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final File IMG_DIR = new File(
			Environment
					.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
			"GIFMyLife");

	public static final int PICTURE_REQUEST_CODE = 100;

	private ProgressDialog mProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final Button button = (Button) findViewById(R.id.take_picture_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent takePictureIntent = new Intent(
						MediaStore.ACTION_IMAGE_CAPTURE);
				File f;
				try {
					f = createImageFile();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
						Uri.fromFile(f));
				startActivityForResult(takePictureIntent, PICTURE_REQUEST_CODE);
			}
		});
		final Button button2 = (Button) findViewById(R.id.create_gif_button);
		button2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				makeGIF();
			}
		});
		
		final Button button3 = (Button) findViewById(R.id.view_gif_button);
		button3.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse("file://" +IMG_DIR+"/output.gif"), "image/*");
				startActivity(intent);
			}
		});
		IMG_DIR.mkdirs();
		
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(this)
		        .setSmallIcon(R.drawable.notif)
		        .setContentTitle("Take today picture")
		        .setContentText("For your awesome life GIF");
		
		NotificationManager mNotificationManager =
		    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		mNotificationManager.notify(1, mBuilder.build()); 
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (requestCode == PICTURE_REQUEST_CODE && resultCode != 0) {
			Toast.makeText(this, "Photo saved", Toast.LENGTH_SHORT).show();
		}
	}

	private File createImageFile() throws IOException {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		String imageFileName = "GML_" + timeStamp + ".jpg";
		File image = new File(IMG_DIR, imageFileName);
		return image;
	}

	protected void makeGIF() {
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setTitle("Making your awesome GIF");
		mProgressDialog.setIndeterminate(false);
		mProgressDialog.setMax(100);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.show();
		GIFEncodeTask task = new GIFEncodeTask();
		task.execute("");
	}

	private class GIFEncodeTask extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... arg0) {
			AnimatedGifEncoder e = new AnimatedGifEncoder();
			try {
				e.start(new FileOutputStream(new File(IMG_DIR, "output.gif")));
			} catch (FileNotFoundException e1) {
				throw new RuntimeException(e1);
			}
			e.setDelay(500); // 1 frame per sec
			File[] files = IMG_DIR.listFiles();
			int c = 0;
			for (File f : files) {
				c += 1;
				publishProgress((int) ((float) c / files.length * 100f));
				if (f.getName().contains(".jpg")) {
					Log.d("D", f.getName());

					BitmapFactory.Options bmOptions = new BitmapFactory.Options();
					bmOptions.inJustDecodeBounds = true;
					BitmapFactory.decodeFile(f.getAbsolutePath(), bmOptions);
					int photoW = bmOptions.outWidth;
					int photoH = bmOptions.outHeight;

					int targetW = 20;
					int targetH = targetW * photoH / photoW;

					int scaleFactor = photoW / targetW;

					// Decode the image file into a Bitmap sized to fill the
					// View
					bmOptions.inJustDecodeBounds = false;
					bmOptions.inSampleSize = scaleFactor;
					bmOptions.inPurgeable = true;

					Bitmap bitmap = BitmapFactory.decodeFile(
							f.getAbsolutePath(), bmOptions);

					e.addFrame(BitmapFactory.decodeFile(f.getAbsolutePath()));
				}
			}
			e.finish();
			publishProgress(-1);
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			mProgressDialog.setProgress(values[0]);
		}
		@Override
		protected void onPostExecute(String result) {
			mProgressDialog.dismiss();
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.parse("file://" +IMG_DIR+"/output.gif"), "image/*");
			startActivity(intent);
		}
	}
}
