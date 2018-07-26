package com.github.hiteshsondhi88.sampleffmpeg;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.ObjectGraph;

public class HomeActivity extends Activity implements View.OnClickListener {

	private static final String TAG = HomeActivity.class.getSimpleName();

	@Inject
	FFmpeg ffmpeg;

	@BindView(R.id.command)
	EditText commandEditText;

	@BindView(R.id.command_output)
	LinearLayout outputLayout;

	@BindView(R.id.run_command)
	Button runButton;

	@BindView(R.id.stop_command)
	Button stopButton;

	private ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home_activity);
		ButterKnife.bind(this);
		ObjectGraph.create(new DaggerDependencyModule(this)).inject(this);

		loadFFMpegBinary();
		initUI();
	}

	private void initUI() {
		runButton.setOnClickListener(this);
		stopButton.setOnClickListener(this);

		progressDialog = new ProgressDialog(this);
		progressDialog.setTitle(null);
	}

	private void loadFFMpegBinary() {
		try {
			ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
				@Override
				public void onFailure() {
					showUnsupportedExceptionDialog();
				}
			});
		} catch (FFmpegNotSupportedException e) {
			showUnsupportedExceptionDialog();
		}
	}

	private void execFFmpegBinary(final String[] command) {
		final List<String> stringList = Arrays.asList(command);
		try {
			ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
				@Override
				public void onFailure(String s) {
					addTextViewToLayout("FAILED with output : " + stringList.toString());
				}

				@Override
				public void onSuccess(String s) {
					addTextViewToLayout("SUCCESS with output : " + s);
				}

				@Override
				public void onProgress(String s) {
					Log.d(TAG, "On progress command : ffmpeg " + stringList.toString());
					addTextViewToLayout("progress : " + s);
					progressDialog.setMessage("Processing\n" + s);
				}

				@Override
				public void onStart() {
					outputLayout.removeAllViews();

					Log.d(TAG, "Started command : ffmpeg " + stringList.toString());
					progressDialog.setMessage("Processing...");
					progressDialog.show();
				}

				@Override
				public void onFinish() {
					Log.d(TAG, "Finished command : ffmpeg " + stringList.toString());
					progressDialog.dismiss();
				}
			});
		} catch (FFmpegCommandAlreadyRunningException e) {
			// do nothing for now
			Log.w(TAG, "FFmpegCommandAlreadyRunningException");
		}
	}

	@Override
	protected void onPause() {
		stop();

		super.onPause();
	}

	private void stop() {
		if (ffmpeg.isFFmpegCommandRunning()) {
			Log.i(TAG, "Kill running process");
			ffmpeg.cancel();
		}
	}

	private void addTextViewToLayout(String text) {
		TextView textView = new TextView(HomeActivity.this);
		textView.setText(text);
		outputLayout.addView(textView);
	}

	private void showUnsupportedExceptionDialog() {
		new AlertDialog.Builder(HomeActivity.this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(getString(R.string.device_not_supported))
				.setMessage(getString(R.string.device_not_supported_message))
				.setCancelable(false)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						HomeActivity.this.finish();
					}
				})
				.create()
				.show();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.run_command:
				String cmd = commandEditText.getText().toString();
				String[] command = cmd.split(" ");
				if (command.length != 0) {
					execFFmpegBinary(command);
				} else {
					Toast.makeText(HomeActivity.this, getString(R.string.empty_command_toast), Toast.LENGTH_LONG).show();
				}
				break;
			case R.id.stop_command:
				stop();
				break;
		}
	}
}
