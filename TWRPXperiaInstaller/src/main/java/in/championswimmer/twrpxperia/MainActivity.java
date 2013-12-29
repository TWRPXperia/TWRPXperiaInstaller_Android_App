package in.championswimmer.twrpxperia;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {

    public static String DEVICE_NAME;
    public static String FOTA_PATH = "fotakernel";
    public static String TAG = "TWRPXperia";
    public static String STORAGE_DIRECTORY = Environment.getExternalStorageDirectory().getPath() + "/TWRPXperia/";
    public boolean supported = false;

    public boolean success;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        DEVICE_NAME = setDEVICE_NAME(Build.DEVICE);
        File dir = new File(STORAGE_DIRECTORY+"valid.txt");
        if(!dir.exists()) dir.mkdirs();
        Log.d(TAG, "device name : " + DEVICE_NAME);
    }

/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
*/
    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }
    public void showSuccess (boolean b, String message) {
        Toast t = Toast.makeText(getApplicationContext(), "Operation not completed !!", Toast.LENGTH_LONG);
        if (b) {
            t.setText(message);
        }
        t.show();
    }


    public void backupFota(View v) {
        success = true;
        String[] cmds = {
                "mkdir -p /sdcard/TWRPXperia",
                "dd if=/dev/block/platform/msm_sdcc.1/by-name/" + FOTA_PATH +" " +
                        "of=/sdcard/TWRPXperia/fotakernel.img"};
        Log.d(TAG, cmds[1]);
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            success = false;
            e.printStackTrace();
        }
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        for (String tmpCmd : cmds) {
            try {
                os.writeBytes(tmpCmd+"\n");
            } catch (IOException e) {
                success = false;
                e.printStackTrace();
            }
        }
        try {
            os.writeBytes("exit\n");
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }
        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                File check = new File(STORAGE_DIRECTORY+"fotakernel.img");
                if (((check.length()/1024)/1024)<5) success = false;
                showSuccess(success, "FOTAKernel backed up to "+STORAGE_DIRECTORY+"fotakernel.img");
            }
        }, 3000);

    }

    public void downloadTWRP (View v) {

        String url = "http://android.championswimmer.in/twrp/" + DEVICE_NAME + "/recovery.img";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("TWRP recovery for "+DEVICE_NAME);
        request.setTitle("recovery.img");
// in order for this if to run, you must use the android 3.2 to compile your app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        File f = new File(STORAGE_DIRECTORY+"recovery.img");
        Log.d(TAG, "filepath = " + f.getPath());
        if (f.exists()) f.delete();
        request.setDestinationUri(Uri.parse("file://" +STORAGE_DIRECTORY+"recovery.img"));

// get download service and enqueue file
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
        Toast t = Toast.makeText(getApplicationContext(), "file will be downloaded to \n "+STORAGE_DIRECTORY+"recovery.img", Toast.LENGTH_LONG);
        t.show();
    }

    public void flashTWRP (View v) {
        success = true;
        File twrp = new File(STORAGE_DIRECTORY+"recovery.img");
        if (!twrp.exists()) {
            success = false;
            Toast a = Toast.makeText(getApplicationContext(),
                    "recovery image does not exist. please download first",
                    Toast.LENGTH_LONG);
            a.show();
        }
        if(((twrp.length()/1024)/1024)<5) {
            success = false;
            Toast a = Toast.makeText(getApplicationContext(),
                    "Download recovery again. The downloaded file is too small in size to be valid",
                    Toast.LENGTH_LONG);
            a.show();

        }
        String[] cmds = {
                "dd if=/sdcard/TWRPXperia/recovery.img" +
                        " of=/dev/block/platform/msm_sdcc.1/by-name/" + FOTA_PATH +" "};
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            success = false;
            e.printStackTrace();
        }
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        for (String tmpCmd : cmds) {
            try {
                os.writeBytes(tmpCmd+"\n");
            } catch (IOException e) {
                success = false;
                e.printStackTrace();
            }
        }
        try {
            os.writeBytes("exit\n");
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                showSuccess(success, "recovery image has been flashed to FOTAKernel");
            }
        }, 3000);

    }

    public void restoreFOTA (View v) {
        boolean success = true;
        File fota = new File(STORAGE_DIRECTORY+"fotakernel.img");
        if (!fota.exists()) {
            success = false;
            Toast a = Toast.makeText(getApplicationContext(),
                    "FOTAKernel backup does not exist. Have you backed up earlier ?",
                    Toast.LENGTH_LONG);
            a.show();
        }
        String[] cmds = {
                "dd if="+fota.getPath()+
                        " of=/dev/block/platform/msm_sdcc.1/by-name/" + FOTA_PATH +" "};
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            success = false;
            e.printStackTrace();
        }
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        for (String tmpCmd : cmds) {
            try {
                os.writeBytes(tmpCmd+"\n");
            } catch (IOException e) {
                success = false;
                e.printStackTrace();
            }
        }
        try {
            os.writeBytes("exit\n");
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        if (success) {
            Toast t = Toast.makeText(getApplicationContext(), "bollocks", Toast.LENGTH_LONG);
            t.setText("Backup image has been flashed back to FOTAKernel partition");
            t.show();
        }

    }

    public void emptyFOTA(View v) {
        boolean success = true;
        File fota = new File("/dev/zero");
        String[] cmds = {
                "dd if=" + fota.getPath() +
                        " of=/dev/block/platform/msm_sdcc.1/by-name/" + FOTA_PATH + " "};
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            success = false;
            e.printStackTrace();
        }
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        for (String tmpCmd : cmds) {
            try {
                os.writeBytes(tmpCmd + "\n");
            } catch (IOException e) {
                success = false;
                e.printStackTrace();
            }
        }
        try {
            os.writeBytes("exit\n");
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        if (success) {
            Toast t = Toast.makeText(getApplicationContext(), "bollocks", Toast.LENGTH_LONG);
            t.setText("FOTAKernel partition has been emptied");
            t.show();
        }

    }

    public String setDEVICE_NAME (String deviceName) {

        String[] deviceProp = getResources().getStringArray(R.array.supported_device_prop);
        String[] deviceCode = getResources().getStringArray(R.array.supported_device_codename);
        String[] fotaPath   = getResources().getStringArray(R.array.fota_path);
        int i;
        for (i = 0; i < deviceCode.length; i++) {
            if (deviceProp[i].equalsIgnoreCase(deviceName)) {
                deviceName = deviceCode[i];
                FOTA_PATH = fotaPath[i];
                supported = true;
                break;
            }
        }
        if (!supported) {
            new AlertDialog.Builder(this)
                    .setTitle("NOT SUPPORTED")
                    .setMessage("Your device "+ deviceName +" is not supported.\n" +
                            "Please contact developer")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    })
                    .show();
        }
        return deviceName;

    }

}
