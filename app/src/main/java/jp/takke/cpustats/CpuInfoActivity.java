package jp.takke.cpustats;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/* http://android-er.blogspot.in/2009/09/read-android-cpu-info.html
 * */
public class CpuInfoActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cpuinfo);

		TextView CPUinfo = (TextView) findViewById(R.id.CPUinfo);
		CPUinfo.setText(ReadCPUinfo());
	}

	private String ReadCPUinfo() {
		ProcessBuilder cmd;
		String result = "";

		try {
			String[] args = { "/system/bin/cat", "/proc/cpuinfo" };
			cmd = new ProcessBuilder(args);

			Process process = cmd.start();
			InputStream in = process.getInputStream();
			byte[] re = new byte[1024];
			while (in.read(re) != -1) {
				System.out.println(new String(re));
				result = result + new String(re);
			}
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return result;
	}
}