package com.example.rafaelvaler.embarcadost3;

import android.app.Activity;
import android.content.pm.ActivityInfo;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
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

public class Main2Activity extends Activity implements SurfaceHolder.Callback {
    private MediaRecorder recorder;
    private SurfaceHolder surfaceHolder;
    private CamcorderProfile camcorderProfile;
    private Camera camera;
    boolean recording = false;
    boolean usecamera = true;
    boolean previewRunning = false;
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

        // onCreate is being called twice (dont know why), this prevents from starting two timers
        if (savedInstanceState != null) {
            startService();
        }
    }

    private void initComs() {
        simpleDateFormat = new SimpleDateFormat("ddMMyyyyhhmmss");
        timeStamp = simpleDateFormat.format(new Date());
        camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
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
//                if (recording) {
//                    recorder.stop();
//                    if (usecamera) {
//                        try {
//                            camera.reconnect();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    // recorder.release();
//                    recording = false;
//                    // Let's prepareRecorder so we can record again
//                    prepareRecorder();
//                    recording = true;
//                    recorder.start();
//                }

                recording = false;
                recorder.stop();
                String teste = mergeVideos_2();
                Log.w("teste", teste);
//                try {
//                    mergeVideos();
//                } catch (IOException e){
//                    System.out.print(e);
//                }

            }
        });
    }

    private ArrayList<String> video_urls = null;

    private String mergeVideos_2() {
        try {
            String mp4_1 = videoOnePath;
            String mp4_2 = videoTwoPath;

            Movie[] inMovies = new Movie[]{
                    MovieCreator.build(mp4_1),
                    MovieCreator.build(mp4_2)
            };

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

            FileChannel fc = new RandomAccessFile(String.format(Environment.getExternalStorageDirectory().getAbsolutePath()+"/output.mp4"), "rw").getChannel();
            container.writeContainer(fc);
            fc.close();

//            BasicContainer out = (BasicContainer) new DefaultMp4Builder()
//                    .build(result);
//
//            @SuppressWarnings("resource")
//            FileChannel fc = new RandomAccessFile(String.format(Environment.getExternalStorageDirectory() + "/wishbyvideo.mp4"), "rw").getChannel();
//            out.writeContainer(fc);
//            fc.close();
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

            if (!recording) {
                recording = true;
                recorder.start();
            }
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
}

