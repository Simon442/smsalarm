/**
 * Copyright (c) 2013 Robert Nyholm. All rights reserved.
 */
package ax.ha.it.smsalarm;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import ax.ha.it.smsalarm.activity.Acknowledge;
import ax.ha.it.smsalarm.handler.LogHandler;
import ax.ha.it.smsalarm.handler.PreferencesHandler;
import ax.ha.it.smsalarm.handler.LogHandler.LogPriorities;
import ax.ha.it.smsalarm.handler.PreferencesHandler.DataTypes;
import ax.ha.it.smsalarm.handler.PreferencesHandler.PrefKeys;

/**
 * Helper class to build up and show notifications, also creates pending <code>intents</code> for
 * the <code>notification</code>. Contains some deprecated functionality, this is to support
 * <code>Android SDK</code> versions below 11.
 * 
 * @author Robert Nyholm <robert.nyholm@aland.net>
 * @version 2.2.1
 * @since 1.2.1-SE
 */
public class AcknowledgeNotificationHelper extends IntentService {

	// Log tag string
	private final String LOG_TAG = getClass().getSimpleName();

	// Objects needed for logging and shared preferences handling
	private final LogHandler logger = LogHandler.getInstance();
	private final PreferencesHandler prefHandler = PreferencesHandler.getInstance();

	// Variables for notifications text and icon
	private String tickerText = "";
	private String contentTitle = "";
	private String contentText = "";
	private int icon = 0;

	/**
	 * Mandatory constructor calling it's <code>super class</code>.
	 * 
	 * @see #onHandleIntent(Intent)
	 * @see ax.ha.it.smsalarm.handler.LogHandler#logCat(LogPriorities, String, String) logCat(LogPriorities,
	 *      String, String)
	 */
	public AcknowledgeNotificationHelper() {
		// Note: MUST call the super() constructor with an (arbitrary) string
		super("AcknowledgeNotificationHelper");

		// Log message for debugging/information purpose
		logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":AcknowledgeNotificationHelper()", "NotificationHelper constructor called");
	}

	/**
	 * Overridden method to handle <code>intent</code>, build up and show <code>notification</code>.
	 * Contains some deprecated functionality just to support <code>Android SDK</code> versions
	 * below 11.
	 * 
	 * @param i
	 *            Intent for notification
	 * 
	 * @see #AcknowledgeNotificationHelper()
	 * @see ax.ha.it.smsalarm.handler.LogHandler#logCat(LogPriorities, String, String) logCat(LogPriorities,
	 *      String, String)
	 * @see ax.ha.it.smsalarm.handler.LogHandler#logCatTxt(LogPriorities, String, String, Throwable)
	 *      logCatTxt(LogPriorities, String, String, Throwable)
	 * @see ax.ha.it.smsalarm.handler.PreferencesHandler#getPrefs(PrefKeys, PrefKeys, DataTypes, Context)
	 *      getPrefs(PrefKeys, PrefKeys, DataTypes, Context)
	 * 
	 * @deprecated
	 */
	@SuppressLint("DefaultLocale")
	@Deprecated
	@Override
	protected void onHandleIntent(Intent i) {
		// Log information
		logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onHandleIntent()", "Start retrieving shared preferences needed by class AcknowledgeNotificationHelper");

		// To store message and rescue service in
		String message = "";
		String rescueService = "";

		try {
			// Get some values from the sharedprefs
			message = (String) prefHandler.getPrefs(PrefKeys.SHARED_PREF, PrefKeys.MESSAGE_KEY, DataTypes.STRING, this);
			rescueService = (String) prefHandler.getPrefs(PrefKeys.SHARED_PREF, PrefKeys.RESCUE_SERVICE_KEY, DataTypes.STRING, this);
		} catch (IllegalArgumentException e) {
			logger.logCatTxt(LogPriorities.ERROR, LOG_TAG + ":onHandleIntent()", "An unsupported datatype was given as argument to PreferencesHandler.getPrefs()", e);
		}

		logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onHandleIntent()", "Shared preferences retrieved");

		// Set intent to AcknowledgeHandler
		Intent notificationIntent = new Intent(this, Acknowledge.class);
		logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onHandleIntent()", "Intent has been set");

		// Setup a notification, directly from android development site
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// To get a unique refresh id for the intents
		long REFRESH_ID = System.currentTimeMillis();
		long when = System.currentTimeMillis();

		// Set proper texts and icon to notification
		setNotificationTexts(android.R.drawable.ic_delete, getString(R.string.PRIMARY_ALARM), rescueService.toUpperCase(), getString(R.string.PRIMARY_ALARM), message);

		// Log
		logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onHandleIntent()", "Notification has been set for a PRIMARY alarm with acknowledgement");

		// Create notification
		Notification notification = new Notification(icon, tickerText, when);

		// Get application context
		Context context = getApplicationContext();

		// Setup message and pendingintent
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

		// This flag auto cancels the notification when clicked and indicating that devices LED
		// should light up
		notification.flags = Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL;
		// Configurate LED
		notification.ledARGB = 0xFFff0000; // Red
		notification.ledOnMS = 100; // On time
		notification.ledOffMS = 100; // Off time

		logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onHandleIntent()", "Notification and it's intent has been configured and are ready to be shown");

		// Show the notification
		mNotificationManager.notify((int) REFRESH_ID, notification);
	}

	/**
	 * To set texts and icon for a notification. The ticker text is built up dynamically depending
	 * on argument <code>rescueService</code>
	 * 
	 * @param icon
	 *            Icon as integer value, use <code>android.R.drawable.*</code>
	 * @param tickerText
	 *            Notifications ticker text
	 * @param rescueService
	 *            String that may or may not contain rescue service's name
	 * @param contentTitle
	 *            Notification contents title
	 * @param contentText
	 *            Notifications content
	 */
	private void setNotificationTexts(int icon, String tickerText, String rescueService, String contentTitle, String contentText) {
		// Log
		logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":setNotificationTexts()", "Setting texts and icon to notification");
		// Set icon for notification
		this.icon = icon;
		// Set ticker text, with rescue service name if it exists
		if (!"".equals(rescueService)) {
			this.tickerText = rescueService + " " + tickerText;
		} else {
			this.tickerText = tickerText;
		}
		// Set content title
		this.contentTitle = contentTitle;
		// Set message to notification
		this.contentText = contentText;
	}
}