package com.example.rafaelvaler.embarcadost3;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;

import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StatFs;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.BasicContainer;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static android.R.attr.path;
import static android.R.attr.subtitleTextAppearance;

public class Main2Activity extends Activity implements SurfaceHolder.Callback, SensorEventListener {
    private MediaRecorder recorder;
    private SurfaceHolder surfaceHolder;
    private CamcorderProfile camcorderProfile;
    private Camera camera;
    boolean recording = false;
    boolean usecamera = true;
    boolean previewRunning = false;

    // accelerometer variables
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;

    // sound level variables
    private static final int sampleRate = 8000;
    private AudioRecord audio;
    private int bufferSize;
    private double lastLevel = 0;
    private Thread soundThread;
    private static final int SAMPLE_DELAY = 75;

    SurfaceView surfaceView;
    Button btnStart, btnStop;
    File root;
    File file;
    Boolean isSDPresent;
    SimpleDateFormat simpleDateFormat;
    String timeStamp;

    String videoOnePath;
    String videoTwoPath;
    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "embarcadosApp");

    private long UPDATE_INTERVAL = 10000; // in Milliseconds
    private long DELAY_INTERVAL = 10000; // in Milliseconds
    private MyTimerTask myTask;

    private ProgressDialog mProgressDialog;

    private int recorderIndex = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main2);


        initComs();
        actionListener();

        videoOnePath = mediaStorageDir.getPath() + "video_temp_" + 0 + ".mp4";
        videoTwoPath = mediaStorageDir.getPath() + "video_temp_" + 1 + ".mp4";

        // accelerator assignments
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);

        // sound assignments
        try {
            bufferSize = AudioRecord
                    .getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);
        } catch (Exception e) {
            android.util.Log.e("TrackingFlow", "Exception", e);
        }
    }

    private void updateRecordingButton() {
        if(recording) {
            btnStop.setText("Save Moment");
        } else {
            btnStop.setText("Start Monitoring");
        }
    }

    private void initComs() {
        simpleDateFormat = new SimpleDateFormat("ddMMyyyyhhmmss");
        timeStamp = simpleDateFormat.format(new Date());
        camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        surfaceView = (SurfaceView) findViewById(R.id.preview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        btnStop = (Button) findViewById(R.id.btn_stop);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        isSDPresent = android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);

    }

    public static float megabytesAvailable(File f) {
        StatFs stat = new StatFs(f.getPath());
        long bytesAvailable = (long) stat.getBlockSize()
                * (long) stat.getAvailableBlocks();
        return bytesAvailable / (1024.f * 1024.f);
    }

    private void actionListener() {

        // TODO: merge two videos

        btnStop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                final Context context = v.getContext();

                if(recording == true) {

                    new AsyncTask<Void, Void, Void>() {

                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                            mProgressDialog = ProgressDialog.show(context, "Saving Video", "");
                            mProgressDialog.setCanceledOnTouchOutside(false);
                        }

                        @Override
                        protected Void doInBackground( Void... params ) {
                            recording = false;
                            try{
                                recorder.stop();
                            }catch(RuntimeException stopException){
                                //handle cleanup here
                            }
                            String teste = mergeVideos_2();
                            return null;
                        }

                        @Override
                        protected void onPostExecute( Void result ) {
                            if (mProgressDialog != null) {
                                mProgressDialog.dismiss();
                            }
                            updateRecordingButton();
                        }
                    }.execute();

                } else {
                    recording = true;
                    recorder.start();
                    startService();
                    updateRecordingButton();
                }
            }
        });
    }

    private ArrayList<String> video_urls = null;

    private String mergeVideos_2() {

        Movie mp4_1 = null;
        Movie mp4_2 = null;
        Movie[] inMovies;

        // create "T3Embarcados" folder to put merged video and make it visible in Pictures folder
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "T3Embarcados");

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("T3Embarcados", "failed to create directory");
                return null;
            }
        }
        // fix
        mediaStorageDir.setExecutable(true);
        mediaStorageDir.setReadable(true);
        mediaStorageDir.setWritable(true);

        try {
            // check if mp4 files exists
            File fileVerification = new File(videoOnePath);
            if (fileVerification.exists()) {
                mp4_1 = MovieCreator.build(videoOnePath);
            }
            fileVerification =  new File(videoTwoPath);
            if (fileVerification.exists()) {
                mp4_2 = MovieCreator.build(videoTwoPath);
            }

            if(mp4_1 != null && mp4_2 != null) {
                inMovies = new Movie[]{mp4_1, mp4_2};
            }else {
                inMovies = new Movie[]{mp4_1};
            }
            List<Track> videoTracks = new LinkedList<Track>();
            List<Track> audioTracks = new LinkedList<Track>();
            for (Movie m : inMovies) {
                for (Track t : m.getTracks()) {
                    if (t.getHandler().equals("soun")) {
                        audioTracks.add(t);
                    }
                    if (t.getHandler().equals("vide")) {
                        videoTracks.add(t);
                    }
                }
            }

            Movie result = new Movie();

            if (audioTracks.size() > 0) {
                result.addTrack(new AppendTrack(audioTracks
                        .toArray(new Track[audioTracks.size()])));
            }
            if (videoTracks.size() > 0) {
                result.addTrack(new AppendTrack(videoTracks
                        .toArray(new Track[videoTracks.size()])));
            }

            final Container container = new DefaultMp4Builder().build(result);

            FileChannel fc = new RandomAccessFile(String.format(mediaStorageDir.getPath()+"/output_"+timeStamp+".mp4"), "rw").getChannel();

            // initiate media scan and put the new things into the path array to
            // make the scanner aware of the location and the files you want to see
            MediaScannerConnection.scanFile(this, new String[] {mediaStorageDir.toString(), mediaStorageDir.toString()+"/output_"+timeStamp+".mp4"}, null, null);

            container.writeContainer(fc);
            fc.close();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String mFileName = Environment.getExternalStorageDirectory()
                .getAbsolutePath();
        mFileName += "/output_test.mp4";



        return mFileName;
    }

    private void mergeVideos() throws IOException{
        String mp4_1 = videoOnePath;
        String mp4_2 = videoTwoPath;

        Movie[] inMovies = new Movie[]{
                MovieCreator.build(mp4_1),
                MovieCreator.build(mp4_2)
        };


        MovieCreator mc = new MovieCreator();


        InputStream inputStream = getClass().getResourceAsStream("/count-english-audio.mp4");
        Movie video = mc.build(Channels.newChannel(getClass().getResourceAsStream("/count-english-audio.mp4")).toString());

        List<Track> videoTracks = video.getTracks();
        video.setTracks(new LinkedList());

        for (Track videoTrack : videoTracks) {
            video.addTrack(new AppendTrack(videoTrack, videoTrack));
        }

        IsoFile out = (IsoFile) new DefaultMp4Builder().build(video);

        FileChannel fc = new RandomAccessFile(String.format("output.mp4"), "rw").getChannel();
        fc.position(0);
        out.getBox(fc);
        fc.close();
    }

    private void prepareRecorder() {
        recorder = new MediaRecorder();
        recorder.setPreviewDisplay(surfaceHolder.getSurface());
        if (usecamera) {
            camera.unlock();
            recorder.setCamera(camera);
        }
        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

        recorder.setProfile(camcorderProfile);

        String filePath = mediaStorageDir.getPath() + "video_temp_" + recorderIndex + ".mp4";
        File output = new File(filePath);

        recorder.setOutputFile(output.getPath());

        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        System.out.println("onsurfacecreated");

        if (usecamera) {
            camera = Camera.open();

            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
                previewRunning = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        System.out.println("onsurface changed");

        if (!recording && usecamera) {
            if (previewRunning) {
                camera.stopPreview();
            }

            try {
                Camera.Parameters p = camera.getParameters();

                p.setPreviewSize(camcorderProfile.videoFrameWidth,
                        camcorderProfile.videoFrameHeight);
                p.setPreviewFrameRate(camcorderProfile.videoFrameRate);

                camera.setParameters(p);

                camera.setPreviewDisplay(holder);
                camera.startPreview();
                previewRunning = true;
            } catch (IOException e) {
                e.printStackTrace();
            }

            prepareRecorder();

//            if (!recording) {
//                recording = true;
//                recorder.start();
//            }
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (recording) {
            recorder.stop();
            recording = false;
        }
        recorder.release();
        if (usecamera) {
            previewRunning = false;
            // camera.lock();
            camera.release();
        }
        finish();
    }

    private void startService() { // prevent to two threads execute at same time

        if(myTask != null)
            return;

        myTask = new MyTimerTask();
        Timer myTimer = new Timer();

        myTimer.schedule(myTask, DELAY_INTERVAL, UPDATE_INTERVAL);
    }

    private class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            if (recording) {
                recorderIndex = (recorderIndex == 0) ? 1 : 0;
                recorder.stop();

                try {
                    camera.reconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                prepareRecorder();
                recorder.start();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    System.out.println("shake mooov");
                    if(recording)
                        btnStop.callOnClick();
                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);

        soundThread.interrupt();
        soundThread = null;
        try {
            if (audio != null) {
                audio.stop();
                audio.release();
                audio = null;
            }
        } catch (Exception e) {e.printStackTrace();}
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        audio = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        audio.startRecording();
        soundThread = new Thread(new Runnable() {
            public void run() {
                while(soundThread != null && !soundThread.isInterrupted()){
                    //thread sleep for a the approximate sampling time
                    try{
                        Thread.sleep(SAMPLE_DELAY);
                    }catch(InterruptedException ie){
                        ie.printStackTrace();
                    }
                    //After this call we can get the last value assigned to the lastLevel variable
                    readAudioBuffer();

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            // TODO: ajustar nivel de acordo com transito real
                            if(lastLevel > 270){
                                System.out.println("level >270");
                                if(recording)
                                    btnStop.callOnClick();
                            }
                        }
                    });
                }
            }
        });
        soundThread.start();

    }

    /**
     * Functionality that gets the sound level out of the sample
     */
    private void readAudioBuffer() {

        try {
            short[] buffer = new short[bufferSize];

            int bufferReadResult = 1;

            if (audio != null) {

                // Sense the voice...
                bufferReadResult = audio.read(buffer, 0, bufferSize);
                double sumLevel = 0;
                for (int i = 0; i < bufferReadResult; i++) {
                    sumLevel += buffer[i];
                }
                lastLevel = Math.abs((sumLevel / bufferReadResult));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

