package com.greenesse.mytracksplugin;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.google.android.apps.mytracks.services.ITrackRecordingService;

import android.app.IntentService;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;

public class BroadcastExecutiveService extends Service {
	private ITrackRecordingService mytracksService;
	private Intent myTracksServiceIntent;

	private BlockingQueue<Intent> tasks = new ArrayBlockingQueue<Intent>(2);

	private ServiceConnection connection = new ServiceConnection() {		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i("MyTracksPlugin", "Service disconnected.");
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i("MyTracksPlugin", "Service connected.");
			mytracksService = ITrackRecordingService.Stub.asInterface(service);
			processTasks();
		}
	};
	
	private void processTasks() {
		Intent intent;
		try {
			intent = tasks.take();
			while (intent != null) {
				try {
					if ("start".equals(intent.getAction())) {
						start();
					}
					else if ("stop".equals(intent.getAction())) {
						stop();
					}
				}
				catch (RemoteException e) {
					tasks.add(intent);
					startAndBindMyTracksService();
					return;
				}

				intent = tasks.poll();

				if (intent == null) {
					Log.i("BES queue", "Work queue is empty; quitting.");
					stopSelf();
					return;
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) tasks.add(intent);
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();

    	myTracksServiceIntent = new Intent();
    	myTracksServiceIntent.setComponent(new ComponentName(
    			getString(R.string.mytracks_service_package),
                getString(R.string.mytracks_service_class)));

		startAndBindMyTracksService();
	}

	private void startAndBindMyTracksService() {
		startService(myTracksServiceIntent);
        if (!bindService(myTracksServiceIntent, connection, 0)) {
        	Log.e("MyTracksPlugin", "Couldn't bind to service.");
        }
	}

	@Override
	public void onDestroy() {
		unbindService(connection);
		super.onDestroy();
	}
	
    public void start() throws RemoteException {
		mytracksService.startNewTrack();
    }
    
    public void stop() throws RemoteException {
		mytracksService.endCurrentTrack();
		stopService(myTracksServiceIntent);
    }
}
