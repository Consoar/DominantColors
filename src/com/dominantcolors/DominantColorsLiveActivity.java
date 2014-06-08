package com.dominantcolors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.dominantcolors.DominantColorsTask.ColorsListener;

public class DominantColorsLiveActivity extends Activity implements
		SurfaceHolder.Callback, Camera.PreviewCallback, ColorsListener {

	private Camera mCamera;
	private LinearLayout mColorHolder;
	private boolean isComputing = false;
	private int numColors;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.live);
		numColors = getIntent().getIntExtra("numColors", 3);
		mColorHolder = (LinearLayout) findViewById(R.id.live_color_holder);
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.live_surface);
		surfaceView.getHolder().addCallback(this);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mCamera = Camera.open();
		if (mCamera != null) {
			try {
				Camera.Parameters parameters = mCamera.getParameters();
				if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
					parameters.set("orientation", "portrait");
					mCamera.setDisplayOrientation(90);
				} else {
					parameters.set("orientation", "landscape");
					mCamera.setDisplayOrientation(0);
				}
				mCamera.setParameters(parameters);
				mCamera.setPreviewDisplay(holder);
				mCamera.setPreviewCallback(this);
				mCamera.startPreview();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				mCamera.release();
				e.printStackTrace();
			}
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		// TODO Auto-generated method stub
		synchronized (this) {
			if (!isComputing) {
				isComputing = true;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				Camera.Parameters parameters = camera.getParameters();
				Size size = parameters.getPreviewSize();
				YuvImage image = new YuvImage(data,
						parameters.getPreviewFormat(), size.width, size.height,
						null);
				image.compressToJpeg(new Rect(0, 0, size.width, size.height),
						80, baos);
				byte[] jdata = baos.toByteArray();
				Bitmap bitmap = BitmapFactory.decodeByteArray(jdata, 0,
						jdata.length);
				new DominantColorsTask(this,numColors).execute(bitmap);
			}
		}
	}

	@Override
	public void onPreExecute() {
		// TODO do nothing
	}

	@Override
	public void onPostExecute(DominantColor[] colors) {
		// TODO Auto-generated method stub
		mColorHolder.removeAllViews();
		for (final DominantColor color : colors) {
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
					LayoutParams.MATCH_PARENT);
			params.weight = color.percentage;
			ImageView iv = new ImageView(this);
			iv.setBackgroundColor(color.color);
			if (mColorHolder != null)
				mColorHolder.addView(iv, params);
		}
		synchronized (this) {
			isComputing = false;
		}
	}

}
