package com.example.jbutler.mymou;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.*;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class TaskManager extends Activity implements Thread.UncaughtExceptionHandler, View.OnClickListener {
    // Debug
    public static String TAG = "MyMouTaskManager";
    private static TextView textView;

    private static int taskId;  // Unique string prefixed to all log entries
    public static String TAG_FRAGMENT = "taskfrag";

    public static int faceRecogPrediction = -1;
    private static int monkeyButtonPressed = -1;
    private static boolean faceRecogRunning = false;
    private static int trialCounter = 0;
    public static RewardSystem rewardSystem;

    private static PreferencesManager preferencesManager;
    private static FolderManager folderManager;
    private static FaceRecog faceRecog;
    private static ArrayList<String> trialData;
    public static String photoTimestamp;
    private static Handler logHandler;
    private static HandlerThread logThread;
    private static FragmentManager fragmentManager;
    private static FragmentTransaction fragmentTransaction;
    private static Context mContext;
    private static Activity activity;

    // Aync handlers used to posting delayed task events
    private static Handler h0 = new Handler();  // Task trial_timer
    private static Handler h1 = new Handler();  // Prepare for new trial
    private static Handler h2 = new Handler();  // Timeout go cues
    private static Handler h3 = new Handler();  // Daily timer

    // Predetermined locations where cues can appear on screen, calculated by UtilsTask.calculateCueLocations()
    private static Point[] possible_cue_locs;

    // Timeouts for wrong choices by subject
    private static int timeoutWrongGoCuePressed = 300;  // Timeout for not pressing their own Go cue

    // Timer to reset task if subject stops halfway through a trial
    private static int time = 0;  // Time from last press - used for idle timeout if it reaches maxTrialDuration
    private static boolean timerRunning;  // Signals if trial_timer currently active

      // Event codes for data logging
    private static String ec_idletimeout = "3";

    // Task objects
    private static Button[] cues_Go = new Button[8]; // Go cues to start a trial
    private static Button[] cues_Reward = new Button[4];  // Reward cues for the different reward options

    // Boolean to signal if task should be active or not (e.g. overnight it is set to true)
    public static boolean shutdown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_all_tasks);

        assignObjects();
        loadAndApplySettings();
        positionGoCues();
        setOnClickListeners();
        disableAllCues();

        initialiseRewardSystem();
        loadCamera();
        loadtask();

        initialiseScreenSettings();
        initialiseAutoRestartHandler();

        if (preferencesManager.facerecog) {
            // Load facerecog off the main thread as takes a while
            Thread t = new Thread(new Runnable() {
                public void run() {
                    faceRecog = new FaceRecog();
                }
            });
            t.start();
        }

        initialiseLogHandler();

        //only lock if we aren't in testing mode
        if (!MainMenu.testingMode) {
            this.startLockTask();
        }

        PrepareForNewTrial(0);

        // Normally the reward system handles this as it has to wait for bluetooth connection
        if (!preferencesManager.bluetooth) {
            enableApp(true);
        }

        dailyTimer();

    }

    private void initialiseAutoRestartHandler() {
            if (preferencesManager.restartoncrash) {
         Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
             @Override
             public void uncaughtException(Thread thread, Throwable throwable) {
                 logHandler.post(new CrashReport(throwable));
                rewardSystem.quitBt();
                 restartApp();
             }
         });
     }

    }

    private void loadtask() {
        taskId = getIntent().getIntExtra("tasktoload", -1);
        if (taskId == -1) {
            new Exception("No task specified");
        }

    }

    private void disableExtraGoCues() {
        // Disable go cues for extra monkeys
        Button[] cues_excluded = Arrays.copyOfRange(cues_Go, preferencesManager.num_monkeys, cues_Go.length);
        UtilsTask.toggleCues(cues_excluded, false);

        // Shorten list to number needed
        cues_Go = Arrays.copyOf(cues_Go, preferencesManager.num_monkeys);
    }

    private void initialiseLogHandler() {
        logThread = new HandlerThread("LogBackground");
        logThread.start();
        logHandler = new Handler(logThread.getLooper());
    }

    private void initialiseRewardSystem() {
        boolean successfullyEstablished = false;
        rewardSystem.quitBt();
        rewardSystem = new RewardSystem(this, this);
        if (rewardSystem.bluetoothConnection | !preferencesManager.bluetooth) {
            successfullyEstablished = enableApp(true);
        }

        // Repeat if either couldn't connect or couldn't enable app
        if (!successfullyEstablished){
            Handler handlerOne = new Handler();
            handlerOne.postDelayed(new Runnable() {
                @Override
                public void run() {
                    initialiseRewardSystem();
                }
            }, 5000);
        }
    }

    public static void startTrial(int monkId) {
        logEvent("Trial started for monkey "+monkId);

        if(!timerRunning) {
            trial_timer();
        }

        Bundle bundle = new Bundle();
        bundle.putInt("currMonk", monkId);
        if (taskId == 0) {
            TaskExample fragment = new TaskExample();
            fragment.setArguments(bundle);
            fragmentTransaction.add(R.id.task_container, fragment, TAG_FRAGMENT);
        } else if (taskId == 1) {
             TaskFromPaper fragment = new TaskFromPaper();
            fragment.setArguments(bundle);
            fragmentTransaction.add(R.id.task_container, fragment, TAG_FRAGMENT);
        } else if (taskId == 2) {
            TaskObjectDiscrim fragment = new TaskObjectDiscrim();
            fragment.setArguments(bundle);
            fragmentTransaction.add(R.id.task_container, fragment, TAG_FRAGMENT);
        }else{
            new Exception("No valid task specified");
        }
        commitFragment();

    }

    // Automatically restart static fragmentTransaction so it is always available to use
    private static void commitFragment() {
        fragmentTransaction.commit();
        fragmentTransaction = fragmentManager.beginTransaction();
    }

    private void loadCamera() {
        if (!preferencesManager.camera) {
            return;
        }

        Log.d(TAG, "Loading camera fragment");
        CameraMain cM = new CameraMain();
        fragmentTransaction.add(R.id.task_container, cM);
        commitFragment();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        logHandler.post(new CrashReport(throwable));
        rewardSystem.quitBt();
        restartApp();
    }

    private void restartApp() {
        Intent intent=new Intent(getApplicationContext(), TaskManager.class);
        intent.putExtra("restart",true);
        intent.putExtra("tasktoload", taskId);
        final PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, pendingIntent);
        System.exit(2);
    }


    public static void setFaceRecogPrediction(int[] intArray) {

        if (faceRecog != null) {

            faceRecogPrediction = faceRecog.idImage(intArray);

            if (faceRecogPrediction == monkeyButtonPressed) {

                // If monkey clicked it's designated button
                Log.d(TAG, "Monkey pressed correct cue");
                resultMonkeyPressedTheirCue(true);

            } else {

                // If monkey clicked wrong button
                Log.d(TAG, "Monkey pressed wrong cue");
                resultMonkeyPressedTheirCue(false);

            }

        } else {

            Log.d(TAG, "Error: FaceRecog not instantiated");

        }

        // Release facerecog as now ready to process another image
        faceRecogRunning = false;
    }

    public static void commitTrialData(String overallTrialOutcome) {
        if (trialData != null) {
            // End of trial so increment trial counter
            if (dateHasChanged()) {
                trialCounter = 0;
            } else {
                trialCounter++;
            }

            // Append all static variables to each line of trial data and write to file
            int length = trialData.size();
            for (int i = 0; i < length; i++) {
                String s = trialData.get(i);
                // Prefix variables that were constant throughout trial (trial result, which monkey, etc)
                s = taskId +"," + trialCounter +"," + faceRecogPrediction + "," + overallTrialOutcome + "," + s;
                logHandler.post(new LogEvent(s));
                Log.d(TAG, "commitTrialData: " + s);
            }

            // Place photo in correct monkey's folder
            if (preferencesManager.facerecog) {
                // Find name of photo and old/new location
                String photo_name =  folderManager.getBaseDate() + "_" + photoTimestamp + ".jpg";
                File original_photo = new File(folderManager.getImageFolder(), photo_name);
                File new_photo = new File (folderManager.getMonkeyFolder(faceRecogPrediction), photo_name);

                // Copy from originalphoto to newphoto
                boolean copy_successful = true;
                FileChannel inputStream = null;
                FileChannel outputStream = null;
                try {
                    inputStream = new FileInputStream(original_photo).getChannel();
                    outputStream = new FileOutputStream(new_photo).getChannel();
                    outputStream.transferFrom(inputStream, 0, inputStream.size());
                } catch (IOException e) {
                    copy_successful = false;
                    e.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                            outputStream.close();
                        } catch (IOException e) {
                            copy_successful = false;
                            e.printStackTrace();
                        }
                    }
                }

                // Erase original photo if copy successful
                if (copy_successful) {
                    original_photo.delete();
                }

            }

        }

        // And now clear the list ready for the next trial
        trialData = new ArrayList<String>();

    }

    private void initialiseScreenSettings() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    // Recursive function to track time and switch app off when it hits a certain time
    // TODO: Add to settings menu on and off times
    public static void dailyTimer() {
        Log.d(TAG, "dailyTimer called");
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        int AMPM = c.get(Calendar.AM_PM);

        if (shutdown) {  // If shutdown and waiting to start up in the morning
            if (hour >= 7 && AMPM == Calendar.AM) {
                Log.d(TAG, "dailyTimer enabling app");
                enableApp(true);
                shutdown = false;
            }
        } else {  // If active and waiting to shutdown
            if (hour >= 7 && AMPM == Calendar.PM) {
                Log.d(TAG, "dailyTimer disabling app");
                enableApp(false);
                shutdown = true;
            }
        }

        h3.postDelayed(new Runnable() {
            @Override
            public void run() {
                dailyTimer();
            }
        }, 60000);
    }

    public static boolean enableApp(boolean bool) {
        Log.d(TAG, "Enabling app"+bool);
        setBrightness(bool);
        View foregroundBlack = activity.findViewById(R.id.foregroundblack);
        if (foregroundBlack != null) {
            UtilsTask.toggleView(foregroundBlack, !bool);  // This is inverted as foreground object disables app
            return true;
        } else {
            Log.d(TAG, "foregroundBlack object not instantiated");
            return false;
        }
    }

    public static void setBrightness(boolean bool) {
        if (Settings.System.canWrite(mContext)) {
            int brightness;
            if (bool) {
                brightness = 255;
            } else {
                brightness = 0;
            }
            ContentResolver cResolver = mContext.getContentResolver();
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
        }
    }

    public static void logEvent(String data) {
        // Show (human) user on screen what is happening during the task
        textView.setText(data);

        // Store data for logging at end of trial
        String timestamp = folderManager.getTimestamp();
        String msg = photoTimestamp + "," + timestamp + "," + data;
        trialData.add(msg);
    }

    // Takes selfie and checks to see if it matches with which monkey it should be
    public static boolean checkMonkey(int monkId) {
        if (preferencesManager.facerecog && faceRecogRunning) {
            // Previous face recog still running
            return false;
        }

        boolean photoTaken = takePhoto();

        if (photoTaken && preferencesManager.facerecog) {
            // Photo taken, now we wait for faceRecog to return prediction
            faceRecogRunning = true;
            monkeyButtonPressed = monkId;
        }

        return photoTaken;

    }

    public static boolean takePhoto() {
        if (preferencesManager.camera) {
            Log.d(TAG, "takePhoto() called");
            photoTimestamp = folderManager.getTimestamp();
            boolean photoTaken = CameraMain.captureStillPicture(photoTimestamp);
            return photoTaken;
        } else {
            return true;
        }
    }

    //Checks if todays date is the same as the last time function was called
    public static boolean dateHasChanged() {
        String todaysDate = folderManager.getBaseDate();
        SharedPreferences sharedPref = activity.getPreferences(mContext.MODE_PRIVATE);
        String lastRecordedDate = sharedPref.getString("lastdate", "null");
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("lastdate", todaysDate);
        editor.commit();
        if (todaysDate.equals(lastRecordedDate)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy() called");
        cancelHandlers();
        rewardSystem.quitBt();
        quitThreads();
        this.stopLockTask();
    }

    private void quitThreads() {
        try {
            logThread.quitSafely();
            logThread.join();
            logThread = null;
            logHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Allow user to exit task if testing mode is enabled
        if (MainMenu.testingMode) {
            super.onBackPressed();
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        return false;
    }

    private void loadAndApplySettings() {
        preferencesManager = new PreferencesManager(this);

        // Face recog
        if (preferencesManager.facerecog) {
            folderManager = new FolderManager(preferencesManager.num_monkeys);
        } else {
            folderManager = new FolderManager();
        }

        // Colours
        findViewById(R.id.task_container).setBackgroundColor(preferencesManager.taskbackground);
        for (int i=0; i<preferencesManager.num_monkeys; i++) {
            cues_Go[i].setBackgroundColor(preferencesManager.colours_gocues[i]);
        }

        disableExtraGoCues();

    }

    private void assignObjects() {
        // Global variables
        activity = this;
        mContext = getApplicationContext();
        possible_cue_locs = new UtilsTask().getPossibleCueLocs(this);
        trialData = new ArrayList<String>();
        fragmentManager = getFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();

        // Layout views
        for (int i=0; i<cues_Go.length; i++) {
            cues_Go[i] = UtilsTask.addCue(i, preferencesManager.colours_gocues[i], this, this, findViewById(R.id.task_container));
        }
        cues_Reward[0]  = findViewById(R.id.buttonRewardZero);
        cues_Reward[1]  = findViewById(R.id.buttonRewardOne);
        cues_Reward[2]  = findViewById(R.id.buttonRewardTwo);
        cues_Reward[3]  = findViewById(R.id.buttonRewardThree);
        textView = findViewById(R.id.tvLog);
    }

     private void setOnClickListeners() {
         UtilsSystem.setOnClickListenerLoop(cues_Reward, this);
         UtilsSystem.setOnClickListenerLoop(cues_Go, this);
    }


    @Override
    public void onClick(View view) {

        // Always disable all cues after a press as monkeys love to bash repeatedly
        disableAllCues();

        // Reset task trial_timer (used for idle timeout and calculating reaction times if desired)
        resetTimer();

        // Make screen bright
        setBrightness(true);

        // Now decide what to do based on what button pressed
        boolean rewardcuepressed = false;
        switch (view.getId()) {
            case R.id.buttonRewardZero:
                deliverReward(0);
            case R.id.buttonRewardOne:
                deliverReward(1);
            case R.id.buttonRewardTwo:
                deliverReward(2);
            case R.id.buttonRewardThree:
                deliverReward(3);
            default:
                checkMonkeyPressedTheirCue(view.getId());
        }
    }

     // Each monkey has it's own start cue. At start of each trial make sure the monkey pressed it's own cue using
    // the facial recognition
    private static void checkMonkeyPressedTheirCue(int monkId) {

        // Take selfie
        boolean photoTaken = checkMonkey(monkId);

        if (preferencesManager.facerecog) {

            if (photoTaken) {

                // If photo successfully taken then do nothing as wait for faceRecog to return prediction
                // setFaceRecogPrediction will ultimately call resultMonkeyPressedTheirCue
                logEvent("FaceRecog started..");

            } else {

                // Photo not taken as camera/faceRecog wasn't ready so reset go cues to let them press again
                UtilsTask.toggleCues(cues_Go, true);

            }

        } else {

            if (photoTaken) {
                // If photo successfully taken then start the trial
                startTrial(monkId);
            } else {
                // Photo not taken as camera wasn't ready so reset go cues to let them press again
                UtilsTask.toggleCues(cues_Go, true);
            }

        }

    }

    public static void resultMonkeyPressedTheirCue(boolean correctCuePressed) {

        // Have to put this on UI thread as it's called from faceRecog which is off main thread
        activity.runOnUiThread(new Runnable() {
            public void run()
            {
                if (correctCuePressed) {
                   startTrial(faceRecogPrediction);
                } else {
                    MonkeyPressedWrongGoCue();
                }
            }
        });
    }

    // Wrong Go cue selected so give short timeout
    public static void MonkeyPressedWrongGoCue() {
        logEvent("Monkey pressed wrong go cue..");
        commitTrialData(preferencesManager.ec_wrong_gocue_pressed);
        // Switch on red background
        activity.findViewById(R.id.background_main).setBackgroundColor(preferencesManager.timeoutbackground);

        // Switch off red background after certain delay
        h2.postDelayed(new Runnable() {
            @Override
            public void run() {
                UtilsTask.toggleCues(cues_Go, true);
                activity.findViewById(R.id.background_main).setBackgroundColor(preferencesManager.taskbackground);
            }
        }, timeoutWrongGoCuePressed);
    }

    public static void trialEnded(String result) {
        logEvent("Trial ended, result = "+result);

        // Kill task and task timer
        fragmentTransaction.remove(fragmentManager.findFragmentByTag(TAG_FRAGMENT));
        commitFragment();
        h0.removeCallbacksAndMessages(null);

        if (result == preferencesManager.ec_correct_trial) {
            correctTrial();
        } else {
            incorrectTrial(result);
        }
    }

    private static void incorrectTrial(String result) {
        logEvent("Feedback: Error trial");
        activity.findViewById(R.id.background_main).setBackgroundColor(preferencesManager.timeoutbackground);
        endOfTrial(result, preferencesManager.timeoutduration);
    }

    private static void correctTrial() {
        logEvent("Correct trial: Reward choice");
        activity.findViewById(R.id.background_main).setBackgroundColor(preferencesManager.rewardbackground);
        UtilsTask.randomlyPositionCues(cues_Reward, possible_cue_locs);
        UtilsTask.toggleCues(cues_Reward, true);
    }

    private void deliverReward(int juiceChoice) {
        logEvent("Delivering "+preferencesManager.rewardduration+"ms reward on channel "+juiceChoice);
        rewardSystem.activateChannel(juiceChoice, preferencesManager.rewardduration);
        endOfTrial(preferencesManager.ec_correct_trial, preferencesManager.rewardduration + 500);
    }


    private static void endOfTrial(String outcome, int newTrialDelay) {
        commitTrialData(outcome);

        PrepareForNewTrial(newTrialDelay);
    }


    private static void disableAllCues() {
        UtilsTask.toggleCues(cues_Reward, false);
        UtilsTask.toggleCues(cues_Go, false);
    }

    // Go cues are in static location to make it easier for monkeys to press their own cue
    private void positionGoCues() {
        int[] go_locs = {14, 9, 1, 22, 6, 17, 18, 5};
        for (int i_monk = 0; i_monk < preferencesManager.num_monkeys; i_monk++) {
            cues_Go[i_monk].setX(possible_cue_locs[go_locs[i_monk]].x);
            cues_Go[i_monk].setY(possible_cue_locs[go_locs[i_monk]].y);
        }
    }

    // Recursive function to track task time
    public void resetTimer() {
        time = 0;
    }

    private static void trial_timer() {
        time += 1000;

        h0.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (time > preferencesManager.responseduration) {
                    time = 0;
                    timerRunning = false;

                    idleTimeout();

                } else {
                    trial_timer();
                    timerRunning = true;
                }
            }
        }, 1000);
    }

    private static void idleTimeout() {
        logEvent("Error stage: Idle timeout");
        disableAllCues();
        activity.findViewById(R.id.background_main).setBackgroundColor(preferencesManager.timeoutbackground);
        //endOfTrial(preferencesManager.ec_trial_timeout, preferencesManager.timeoutduration);  // TODO: Switch this to trialEnded (which is static)
        trialEnded(ec_idletimeout);
    }

    private static void PrepareForNewTrial(int delay) {
        setBrightness(true);
    h1.postDelayed(new Runnable() {
            @Override
            public void run() {
                activity.findViewById(R.id.background_main).setBackgroundColor(preferencesManager.taskbackground);
                UtilsTask.toggleCues(cues_Go, true);
                textView.setText("Initiation Stage");
            }
        }, delay);
    }

    private void cancelHandlers() {
        h0.removeCallbacksAndMessages(null);
        h1.removeCallbacksAndMessages(null);
        h2.removeCallbacksAndMessages(null);
        h3.removeCallbacksAndMessages(null);
    }


}
