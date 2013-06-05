/*
 * Copyright (c) 2013 Robert Nyholm. All rights reserved.
 */

package ax.ha.it.smsalarm;

import java.io.IOException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.IBinder;
import android.os.Vibrator;

/**
 * This class is responsible for all sound and vibration handling. This means
 * playing tones, vibrate and so on, depending on application and phone
 * settings.<br>
 * <b><i>NoiseHandler is a singleton.</i></b>
 * 
 * @author Robert Nyholm <robert.nyholm@aland.net>
 * @version 2.1
 * @since 2.0
 * @date 2013-05-01
 */
public class NoiseHandler extends Service {
	// Singleton instance of this class
	private static NoiseHandler INSTANCE;

	// Log tag
	private String LOG_TAG = "NoiseHandler";

	// Variable used to log messages
	private LogHandler logger;

	// Declarations of different objects needed by makeNoise
	MediaPlayer mPlayer = new MediaPlayer(); // MediaPlayer object
	final AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE); // AudioManager used to get and set different volume levels
	Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE); // Instance of Vibrator from context
	AssetFileDescriptor afd = null; // AssetFileDescriptor to get mp3 file
	final int currentMediaVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC); // Store current media volume
	final int currentRingVolume = am.getStreamVolume(AudioManager.STREAM_RING); // Store current ring volume
	final int maxRingVolume = am.getStreamMaxVolume(AudioManager.STREAM_RING); // Store max ring volume
	float alarmVolume = 0; // To store calculated alarm volume in
	int toBePlayed; // Variable indicating how many times message tone should be played
	// Custom vibration pattern
	long[] pattern = { 0, 5000, 500, 5000, 500, 5000, 500, 5000 };
	private boolean playToneTwice = false;
	boolean useSoundSettings = true;

	@Override
	public void onCreate() {
		super.onCreate();

		// Log information
		this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "MediaPlayer initalized, Audio Service initalized, Vibrator initalized, AssetFileDescriptor initalized. Current MediaVolume stored, current RingVolume stored. Vibration pattern variables is set.");

		// Set media volume to max level
		am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

		// Log information
		this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Device's media volume has been set to max");

		// Calculate correct alarm volume, depending on current ring volume
		if (currentRingVolume > 0) {
			alarmVolume = (float) currentRingVolume / maxRingVolume;
		} else {
			alarmVolume = 0;
		}

		// Log information
		this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Correct alarm volume has been calculated");

		// Unresolve correct tone depending on id
		try {
			this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Try to unresolve message tone");
			afd = this.getAssets().openFd("tones/alarm/" + msgToneLookup(this, 2) + ".mp3");
		} catch (IOException e) {
			// IOException occurred, trace and log it
			e.printStackTrace();
			this.logger.logCatTxt(this.logger.getERROR(), this.LOG_TAG + ":makeNoise()", "An IOException occurred while unresolving message tone from id" + e);
		}

		// Set data source for mPlayer, common both for debug and ordinary mode
		try {
			mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
			this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Data source has been set for Media Player");
		} catch (IllegalArgumentException e) {
			// IllegalArgumentException occurred, trace and log it
			e.printStackTrace();
			this.logger.logCatTxt(this.logger.getERROR(), this.LOG_TAG + ":makeNoise()", "An IllegalArgumentException occurred while setting data source for media player" + e);
		} catch (IllegalStateException e) {
			// IllegalStateException occurred, trace and log it
			e.printStackTrace();
			this.logger.logCatTxt(this.logger.getERROR(), this.LOG_TAG + ":makeNoise()", "An IllegalStateException occurred while setting data source for media player" + e);
		} catch (IOException e) {
			// IOException occurred, trace and log it
			e.printStackTrace();
			this.logger.logCatTxt(this.logger.getERROR(), this.LOG_TAG + ":makeNoise()", "An IOException occurred while setting data source for media player" + e);
		}

		// Prepare mPlayer, also common for debug and ordinary mode
		try {
			mPlayer.prepare();
			this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Media Player prepared");
		} catch (IllegalStateException e) {
			// IllegalStateException occurred, trace and log it
			e.printStackTrace();
			this.logger.logCatTxt(this.logger.getERROR(), this.LOG_TAG + ":makeNoise()", "An IllegalStateException occurred while preparing media player" + e);
		} catch (IOException e) {
			// IOException occurred, trace and log it
			e.printStackTrace();
			this.logger.logCatTxt(this.logger.getERROR(), this.LOG_TAG + ":makeNoise()", "An IOException occurred while preparing media player" + e);
		}

		// If false then just play message tone once else twice
		if (!playToneTwice) {
			toBePlayed = 1;
			// Log information
			this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Message tone will be played once, if device not in RINGER_MODE_SILENT  or application is set to not consider device's sound settings");
		} else {
			toBePlayed = 2;
			// Log information
			this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Message tone will be played twice, if device not in RINGER_MODE_SILENT or application is set to not consider device's sound settings");
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		if (!mPlayer.isPlaying()) {
//			new Thread(new Runnable() {
//				public void run() {
					/*
					 * If application use systems sound settings, check if phone
					 * is in normal, silent or vibration mode else don't check
					 * phones status and play tone and vibrate even if phone is
					 * in silent or vibrate mode
					 */
					if (useSoundSettings) {
						// Log information
						logger.logCatTxt(logger.getINFO(), LOG_TAG + ":makeNoise()", "Application is set to take into account device's sound settings");

						// Decide if phone are in normal, vibrate or silent
						// state and take
						// action
						switch (am.getRingerMode()) {
						case AudioManager.RINGER_MODE_SILENT:
							// Do nothing except log information
							logger.logCatTxt(logger.getINFO(), LOG_TAG + ":makeNoise()", "Device is in RINGER_MODE_SILENT, don't vibrate or play message tone");
							break;
						case AudioManager.RINGER_MODE_VIBRATE:
							// Vibrate, -1 = no repeat
							v.vibrate(pattern, -1);

							logger.logCatTxt(logger.getINFO(), LOG_TAG + ":makeNoise()", "Device is in RINGER_MODE_VIBRATE, just vibrate");
							break;
						case AudioManager.RINGER_MODE_NORMAL:
							// Set correct volume to mediaplayer
							mPlayer.setVolume(alarmVolume, alarmVolume);

							// Log information
							logger.logCatTxt(logger.getINFO(), LOG_TAG + ":makeNoise()", "Correct volume has been set to media player from previously calculated alarm volume");

							// Vibrate, -1 = no repeat
							v.vibrate(pattern, -1);
							// Start play message tone
							mPlayer.start();

							logger.logCatTxt(logger.getINFO(), LOG_TAG + ":makeNoise()", "Device is in RINGER_MODE_NORMAL, vibrate and play message tone");
							break;
						default: // <--Unsupported RINGER_MODE
									// Do nothing except log information
							logger.logCatTxt(logger.getERROR(), LOG_TAG + ":makeNoise()", "Device is in a UNSUPPORTED ringer mode, can't decide what to do");
						}
					} else { // If not take into account OS sound setting,
								// always ring at highest volume and vibrate
						// Vibrate, -1 = no repeat
						v.vibrate(pattern, -1);
						// Start play message tone
						mPlayer.start();

						// Log information
						logger.logCatTxt(logger.getINFO(), LOG_TAG + ":makeNoise()", "Application is set to don't take into account device's sound settings. Play message tone at max volume and vibrate");
					}

					// Listen to completion, in other words when media player
					// has finished
					// and reset media volume and media player
					mPlayer.setOnCompletionListener(new OnCompletionListener() {
						// Counter variable to count number of times played, we
						// have already played the message tone once
						int timesPlayed = 1;

						/**
						 * Listener to listen when message tone has finished
						 * playing.
						 */
						@Override
						public void onCompletion(MediaPlayer mPlayer) {
							// If message tone havn't been played enough times,
							// else release mediaplayer
							if (timesPlayed < toBePlayed) {
								// Add to counter
								timesPlayed++;
								// Seek to beginning of message tone
								mPlayer.seekTo(0);
								// Start play message tone
								mPlayer.start();
							} else {
								am.setStreamVolume(AudioManager.STREAM_MUSIC, currentMediaVolume, 0);
								mPlayer.release();
								// Log information
								logger.logCatTxt(logger.getINFO(), LOG_TAG + ":makeNoise().MediaPlayer.onCompletion()", "Media player have been released and all sound levels have been restored");
								
								stopSelf();
							}
						}
					});
				}
//			}).start();
//		}
		
		return START_NOT_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if(mPlayer.isPlaying()) {
			mPlayer.stop();
		}
		mPlayer.release();
	}

//	/**
//	 * Private constructor, is private due to it's singleton pattern.
//	 */
//	private NoiseHandler() {
//		// Get instance of logger
//		this.logger = LogHandler.getInstance();
//
//		// Log information
//		this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":NoiseHandler()", "New instance of NoiseHandler created");
//	}
//
//	/**
//	 * Method to get the singleton instance of this class.
//	 * 
//	 * @return Singleton instance of NoiseHandler
//	 */
//	public static NoiseHandler getInstance() {
//		// If instance of this object is null create a new one
//		if (INSTANCE == null) {
//			INSTANCE = new NoiseHandler();
//		}
//
//		return INSTANCE;
//	}

	/**
	 * Method to play message tone and vibrate, depending on application
	 * settings. This method also takes in account the operating systems sound
	 * settings, this depends on a input parameter.
	 * 
	 * @param context
	 *            Context
	 * @param id
	 *            ToneId as Integer
	 * @param useSoundSettings
	 *            If this method should take consideration to the device's sound
	 *            settings as Boolean
	 * @param playToneTwice
	 *            Indication whether message tone should be played once or
	 *            twice, this is the same for vibration also
	 * 
	 * @exception IllegalArgumentException
	 *                Can occur when setting data source for media player or
	 *                preparing media player
	 * @exception IllegalStateException
	 *                Can occur when setting data source for media player or
	 *                preparing media player
	 * @exception IOException
	 *                Can occur when setting data source for media player or
	 *                preparing media player. Also when resolving message tone
	 *                id
	 */
	public void makeNoise(Context context, int id, boolean useSoundSettings, boolean playToneTwice) {
		// Log information
		this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Preparing to play message tone and vibrate");

		// Declarations of different objects needed by makeNoise
		MediaPlayer mPlayer = new MediaPlayer(); // MediaPlayer object
		final AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE); // AudioManager
																								// used
																								// to
																								// get
																								// and
																								// set
																								// different
																								// volume
																								// levels
		Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE); // Instance
																					// of
																					// Vibrator
																					// from
																					// context
		AssetFileDescriptor afd = null; // AssetFileDescriptor to get mp3 file
		final int currentMediaVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC); // Store
																						// current
																						// media
																						// volume
		final int currentRingVolume = am.getStreamVolume(AudioManager.STREAM_RING); // Store
																					// current
																					// ring
																					// volume
		final int maxRingVolume = am.getStreamMaxVolume(AudioManager.STREAM_RING); // Store
																					// max
																					// ring
																					// volume
		float alarmVolume = 0; // To store calculated alarm volume in
		final int toBePlayed; // Variable indicating how many times message tone
								// should be played

		/*
		 * SOS morsecode pattern from
		 * http://android.konreu.com/developer-how-to/
		 * vibration-examples-for-android-phone-development/
		 */
		// int dot = 500; // Length of a Morse Code "dot" in milliseconds
		// int dash = 800; // Length of a Morse Code "dash" in milliseconds
		// int short_gap = 200; // Length of Gap Between dots/dashes
		// int medium_gap = 500; // Length of Gap Between Letters
		// int long_gap = 1000; // Length of Gap Between Words
		// long[] pattern = { 0, // Start immediately
		// dot, short_gap, dot, short_gap, dot, // s
		// medium_gap, dash, short_gap, dash, short_gap, dash, // o
		// medium_gap, dot, short_gap, dot, short_gap, dot, // s
		// long_gap };

		// Custom vibration pattern
		long[] pattern = { 0, 5000, 500, 5000, 500, 5000, 500, 5000 };

		// Log information
		this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "MediaPlayer initalized, Audio Service initalized, Vibrator initalized, AssetFileDescriptor initalized. Current MediaVolume stored, current RingVolume stored. Vibration pattern variables is set.");

		// Set media volume to max level
		am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

		// Log information
		this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Device's media volume has been set to max");

		// Calculate correct alarm volume, depending on current ring volume
		if (currentRingVolume > 0) {
			alarmVolume = (float) currentRingVolume / maxRingVolume;
		} else {
			alarmVolume = 0;
		}

		// Log information
		this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Correct alarm volume has been calculated");

		// Unresolve correct tone depending on id
		try {
			this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Try to unresolve message tone");
			afd = context.getAssets().openFd("tones/alarm/" + msgToneLookup(context, id) + ".mp3");
		} catch (IOException e) {
			// IOException occurred, trace and log it
			e.printStackTrace();
			this.logger.logCatTxt(this.logger.getERROR(), this.LOG_TAG + ":makeNoise()", "An IOException occurred while unresolving message tone from id" + e);
		}

		// Set data source for mPlayer, common both for debug and ordinary mode
		try {
			mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
			this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Data source has been set for Media Player");
		} catch (IllegalArgumentException e) {
			// IllegalArgumentException occurred, trace and log it
			e.printStackTrace();
			this.logger.logCatTxt(this.logger.getERROR(), this.LOG_TAG + ":makeNoise()", "An IllegalArgumentException occurred while setting data source for media player" + e);
		} catch (IllegalStateException e) {
			// IllegalStateException occurred, trace and log it
			e.printStackTrace();
			this.logger.logCatTxt(this.logger.getERROR(), this.LOG_TAG + ":makeNoise()", "An IllegalStateException occurred while setting data source for media player" + e);
		} catch (IOException e) {
			// IOException occurred, trace and log it
			e.printStackTrace();
			this.logger.logCatTxt(this.logger.getERROR(), this.LOG_TAG + ":makeNoise()", "An IOException occurred while setting data source for media player" + e);
		}

		// Prepare mPlayer, also common for debug and ordinary mode
		try {
			mPlayer.prepare();
			this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Media Player prepared");
		} catch (IllegalStateException e) {
			// IllegalStateException occurred, trace and log it
			e.printStackTrace();
			this.logger.logCatTxt(this.logger.getERROR(), this.LOG_TAG + ":makeNoise()", "An IllegalStateException occurred while preparing media player" + e);
		} catch (IOException e) {
			// IOException occurred, trace and log it
			e.printStackTrace();
			this.logger.logCatTxt(this.logger.getERROR(), this.LOG_TAG + ":makeNoise()", "An IOException occurred while preparing media player" + e);
		}

		// If false then just play message tone once else twice
		if (!playToneTwice) {
			toBePlayed = 1;
			// Log information
			this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Message tone will be played once, if device not in RINGER_MODE_SILENT  or application is set to not consider device's sound settings");
		} else {
			toBePlayed = 2;
			// Log information
			this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Message tone will be played twice, if device not in RINGER_MODE_SILENT or application is set to not consider device's sound settings");
		}

		/*
		 * If application use systems sound settings, check if phone is in
		 * normal, silent or vibration mode else don't check phones status and
		 * play tone and vibrate even if phone is in silent or vibrate mode
		 */
		if (useSoundSettings) {
			// Log information
			this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Application is set to take into account device's sound settings");

			// Decide if phone are in normal, vibrate or silent state and take
			// action
			switch (am.getRingerMode()) {
			case AudioManager.RINGER_MODE_SILENT:
				// Do nothing except log information
				this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Device is in RINGER_MODE_SILENT, don't vibrate or play message tone");
				break;
			case AudioManager.RINGER_MODE_VIBRATE:
				// Vibrate, -1 = no repeat
				v.vibrate(pattern, -1);

				this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Device is in RINGER_MODE_VIBRATE, just vibrate");
				break;
			case AudioManager.RINGER_MODE_NORMAL:
				// Set correct volume to mediaplayer
				mPlayer.setVolume(alarmVolume, alarmVolume);

				// Log information
				this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Correct volume has been set to media player from previously calculated alarm volume");

				// Vibrate, -1 = no repeat
				v.vibrate(pattern, -1);
				// Start play message tone
				mPlayer.start();

				this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Device is in RINGER_MODE_NORMAL, vibrate and play message tone");
				break;
			default: // <--Unsupported RINGER_MODE
						// Do nothing except log information
				this.logger.logCatTxt(this.logger.getERROR(), this.LOG_TAG + ":makeNoise()", "Device is in a UNSUPPORTED ringer mode, can't decide what to do");
			}
		} else { // If not take into account OS sound setting, always ring at
					// highest volume and vibrate
			// Vibrate, -1 = no repeat
			v.vibrate(pattern, -1);
			// Start play message tone
			mPlayer.start();

			// Log information
			this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":makeNoise()", "Application is set to don't take into account device's sound settings. Play message tone at max volume and vibrate");
		}

		// Listen to completion, in other words when media player has finished
		// and reset media volume and media player
		mPlayer.setOnCompletionListener(new OnCompletionListener() {
			// Counter variable to count number of times played, we have already
			// played the message tone once
			int timesPlayed = 1;

			/**
			 * Listener to listen when message tone has finished playing.
			 */
			@Override
			public void onCompletion(MediaPlayer mPlayer) {
				// If message tone havn't been played enough times, else release
				// mediaplayer
				if (timesPlayed < toBePlayed) {
					// Add to counter
					timesPlayed++;
					// Seek to beginning of message tone
					mPlayer.seekTo(0);
					// Start play message tone
					mPlayer.start();
				} else {
					am.setStreamVolume(AudioManager.STREAM_MUSIC, currentMediaVolume, 0);
					mPlayer.release();
					// Log information
					logger.logCatTxt(logger.getINFO(), LOG_TAG + ":makeNoise().MediaPlayer.onCompletion()", "Media player have been released and all sound levels have been restored");
				}
			}
		});
	}

	/**
	 * Method to lookup proper message tone from media.
	 * 
	 * @param context
	 *            Context
	 * @param toneId
	 *            ToneId as Integer
	 * @return toneId Message tone as String
	 */
	public String msgToneLookup(Context context, int toneId) {
		// Resolve message tone from id
		String[] tonesArr = context.getResources().getStringArray(R.array.tones);

		// Some logging
		this.logger.logCatTxt(this.logger.getINFO(), this.LOG_TAG + ":msgToneLookup()", "Message tone has been unresolved from id");

		return tonesArr[toneId];
	}
}
