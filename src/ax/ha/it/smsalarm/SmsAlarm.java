/**
 * Copyright (c) 2013 Robert Nyholm. All rights reserved.
 */
package ax.ha.it.smsalarm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import ax.ha.it.smsalarm.LogHandler.LogPriorities;
import ax.ha.it.smsalarm.PreferencesHandler.DataTypes;
import ax.ha.it.smsalarm.PreferencesHandler.PrefKeys;

/**
 * Main activity to configure application. Also holds the main User Interface.
 * 
 * @author Robert Nyholm <robert.nyholm@aland.net>
 * @version 2.1.4
 * @since 0.9beta
 * 
 * @see #onCreate(Bundle)
 * @see #onPause()
 * @see #onDestroy()
 */
public class SmsAlarm extends Activity {
	/**
	 * Enumeration for different types of input dialogs.
	 * 
	 * @author Robert Nyholm <robert.nyholm@aland.net>
	 * @version 2.1
	 * @since 2.1
	 */
	private enum DialogTypes {
		PRIMARY, SECONDARY, ACKNOWLEDGE, RESCUESERVICE;
	}

	// Log tag string
	private final String LOG_TAG = this.getClass().getSimpleName();

	// Objects needed for logging, shared preferences and noise handling
	private LogHandler logger = LogHandler.getInstance();
	private static final PreferencesHandler prefHandler = PreferencesHandler.getInstance();
	private NoiseHandler noiseHandler = NoiseHandler.getInstance();

	// Object to handle database access and methods
	private DatabaseHandler db;

	// Variables of different UI elements and types
	// The EdittextObjects
	private EditText primaryListenNumberEditText;
	private EditText selectedToneEditText;
	private EditText ackNumberEditText;
	private EditText rescueServiceEditText;

	// The Button objects
	private Button editPrimaryNumberButton;
	private Button addSecondaryNumberButton;
	private Button removeSecondaryNumberButton;
	private Button editMsgToneButton;
	private Button listenMsgToneButton;
	private Button ackNumberButton;
	private Button editRescueServiceButton;

	// The CheckBox objects
	private CheckBox soundSettingCheckBox;
	private CheckBox enableAckCheckBox;
	private CheckBox playToneTwiceSettingCheckBox;
	private CheckBox enableSmsAlarmCheckBox;

	// The ImageView objects
	private ImageView divider1ImageView;
	private ImageView divider2ImageView;
	private ImageView divider3ImageView;

	// The Spinner objects
	private Spinner toneSpinner;
	private Spinner secondaryListenNumberSpinner;

	// The textView objects
	private TextView soundSettingInfoTextView;
	private TextView playToneTwiceInfoTextView;
	private TextView enableSmsAlarmInfoTextView;
	private TextView enableAckInfoTextView;

	// Strings to store different important numbers
	private String primaryListenNumber = "";
	private String acknowledgeNumber = "";

	// List of Strings containing secondaryListenNumbers
	private List<String> secondaryListenNumbers = new ArrayList<String>();
	private List<String> emptySecondaryListenNumbers = new ArrayList<String>(); // <-- A "dummy" list just containing one element, one string

	// String to store firedepartments name in
	private String rescueService = "";

	// Integer to store which tone id to be used
	private int primaryMessageToneId = 0;
	private int secondaryMessageToneId = 1;

	// Boolean variables to store whether to use OS soundsettings or not, and if
	// acknowledge is enabled
	private boolean useOsSoundSettings = false;
	private boolean useAlarmAcknowledge = false;
	private boolean playToneTwice = false;
	private boolean enableSmsAlarm = true;

	// Integer holding spinners positions
	private int toneSpinnerPos = 0;

	/**
	 * When activity starts, this method is the entry point. The User Interface
	 * is built up and different <code>Listeners</code> are set within this
	 * method.
	 * 
	 * @param savedInstanceState
	 *            Default Bundle
	 * 
	 * @see #findViews()
	 * @see #updateSelectedToneEditText()
	 * @see #updateAcknowledgeWidgets()
	 * @see #updateWholeUI()
	 * @see #buildAndShowInputDialog(DialogTypes)
	 * @see #getSmsAlarmPrefs()
	 * @see #buildAndShowDeleteSecondaryNumberDialog()
	 * @see #buildAndShowToneDialog()
	 * @see #onPause()
	 * @see #onDestroy()
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 * @see ax.ha.it.smsalarm.LogHandler#logCatTxt(LogPriorities, String,
	 *      String) logCatTxt(LogPriorities, String, String)
	 * @see ax.ha.it.smsalarm.LogHandler#logCatTxt(LogPriorities, String,
	 *      String, Throwable) logCatTxt(LogPriorities, String, String,
	 *      Throwable)
	 * @see ax.ha.it.smsalarm.NoiseHandler#makeNoise(Context, int, boolean,
	 *      boolean) makeNoise(Context, int, boolean, boolean)
	 * @see ax.ha.it.smsalarm.PreferencesHandler#setPrefs(PrefKeys, PrefKeys,
	 *      Object, Context) setPrefs(PrefKeys, PrefKeys, Object, Context)
	 * @see ax.ha.it.smsalarm.DatabaseHandler ax.ha.it.smsalarm.DatabaseHandler
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Log in debugging and information purpose
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":onCreate()", "Creation of Sms Alarm started");

		// Get sharedPreferences
		this.getSmsAlarmPrefs();

		// FindViews
		this.findViews();

		// Initialize database handler object from context
		this.db = new DatabaseHandler(this);

		// Fill tone spinner with values
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.alarms, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// Set adapter to tone spinner
		this.toneSpinner.setAdapter(adapter);

		// Set listener to editPrimaryNumberButton
		this.editPrimaryNumberButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Logging
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().editPrimaryNumberButton.OnClickListener().onClick()", "Edit PRIMARY listen number Button pressed");
				// Build up and show input dialog of type primary number
				buildAndShowInputDialog(DialogTypes.PRIMARY);
			}
		});

		// Set listener to addSecondaryNumberButton
		this.addSecondaryNumberButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Logging
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().addSecondaryNumberButton.OnClickListener().onClick()", "Add SECONDARY listen number Button pressed");
				// Build up and show input dialog of type secondary number
				buildAndShowInputDialog(DialogTypes.SECONDARY);
			}
		});

		// Set listener to removeSecondaryNumberButton
		this.removeSecondaryNumberButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Logging
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().removeSecondaryNumberButton.OnClickListener().onClick()", "Remove SECONDARY listen number Button pressed");

				// Only show delete dialog if secondary listen numbers exists,
				// else show toast
				if (!secondaryListenNumbers.isEmpty()) {
					// Show alert dialog(prompt user for deleting number)
					buildAndShowDeleteSecondaryNumberDialog();
				} else {
					Toast.makeText(SmsAlarm.this, R.string.NO_SECONDARY_NUMBER_EXISTS, Toast.LENGTH_LONG).show();
					logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().removeSecondaryNumberButton.OnClickListener().onClick()", "Cannot build and show dialog because the list of SECONDARY listen numbers is empty so there is nothing to remove");
				}
			}
		});

		// Set listener to ackNumberButton
		this.ackNumberButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Logging
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().ackNumberButton.OnClickListener().onClick()", "Edit acknowledge number Button pressed");
				// Build up and show input dialog of type acknowledge number
				buildAndShowInputDialog(DialogTypes.ACKNOWLEDGE);
			}
		});

		// Set listener to editRescueServiceButton
		this.editRescueServiceButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Logging
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().editRescueServiceButton.OnClickListener().onClick()", "Edit rescue service Button pressed");
				// Build up and show input dialog of type primary number
				buildAndShowInputDialog(DialogTypes.RESCUESERVICE);
			}
		});

		// Set listener to editMsgToneButton
		this.editMsgToneButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Logging
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().editMsgToneButton.OnClickListener().onClick()", "Edit message tone Button pressed");
				// Build up and Show alert dialog(prompt for message tone)
				buildAndShowToneDialog();
			}
		});

		// Set listener to listenMsgToneButton
		this.listenMsgToneButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Play the correct tone and vibrate, depending on spinner value
				if (toneSpinnerPos == 0) {
					// Logging
					logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().listenMsgToneButton.OnClickListener().onClick()", "Listen message tone Button pressed. Message tone for PRIMARY alarm will be played");
					// Play message tone and vibrate
					noiseHandler.makeNoise(SmsAlarm.this, primaryMessageToneId, useOsSoundSettings, false);
				} else if (toneSpinnerPos == 1) {
					logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().listenMsgToneButton.OnClickListener().onClick()", "Listen message tone Button pressed. Message tone for SECONDARY alarm will be played");
					noiseHandler.makeNoise(SmsAlarm.this, secondaryMessageToneId, useOsSoundSettings, false);
				} else {
					// DO NOTHING EXCEPT LOG ERROR MESSAGE
					logger.logCatTxt(LogPriorities.ERROR, LOG_TAG + ":onCreate().listenMsgToneButton.OnClickListener().onClick()", "Invalid spinner position occurred. Current tone Spinner position is: \"" + Integer.toString(toneSpinnerPos) + "\"");
				}
			}
		});

		// Set listener to soundSettingCheckBox
		this.soundSettingCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// Log that CheckBox been pressed
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().soundSettingCheckBox.onCheckedChange()", "Use OS sound settings CheckBox pressed(or CheckBox initialized)");

				// Set checkbox depending on it's checked status and store
				// variable
				if (soundSettingCheckBox.isChecked()) {
					// Store value to variable
					useOsSoundSettings = true;
					// logging
					logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().soundSettingCheckBox.onCheckedChange()", "Use OS sound settings CheckBox \"Checked\"(" + useOsSoundSettings + ")");
				} else {
					useOsSoundSettings = false;
					logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().soundSettingCheckBox.onCheckedChange()", "Use OS sound settings CheckBox \"Unchecked\"(" + useOsSoundSettings + ")");
				}

				try {
					// Store value to shared preferences
					prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.USE_OS_SOUND_SETTINGS_KEY, useOsSoundSettings, SmsAlarm.this);
				} catch (IllegalArgumentException e) {
					logger.logCatTxt(LogPriorities.ERROR, LOG_TAG + ":onCreate().soundSettingCheckBox.onCheckedChange()", "An Object of unsupported instance was given as argument to PreferencesHandler.setPrefs()", e);
				}
			}
		});

		// Set listener to enableAckCheckBox
		this.enableAckCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// Log that CheckBox been pressed
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().enableAckCheckBox.onCheckedChange()", "Enable acknowledge CheckBox pressed(or CheckBox initialized)");

				// Set checkbox depending on it's checked status and store
				// variable
				if (enableAckCheckBox.isChecked()) {
					// Store value to variable
					useAlarmAcknowledge = true;
				} else {
					useAlarmAcknowledge = false;
				}

				try {
					// Store value to shared preferences
					prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.ENABLE_ACK_KEY, useAlarmAcknowledge, SmsAlarm.this);
				} catch (IllegalArgumentException e) {
					logger.logCatTxt(LogPriorities.ERROR, LOG_TAG + ":onCreate().enableAckCheckBox.onCheckedChange()", "An Object of unsupported instance was given as argument to PreferencesHandler.setPrefs()", e);
				}
				// Update UI widgets affected by enable acknowledge
				updateAcknowledgeWidgets();
			}
		});

		// Set listener to playToneTwiceSettingCheckBox
		this.playToneTwiceSettingCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// Log that CheckBox been pressed
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().playToneTwiceSettingCheckBox.onCheckedChange()", "Play tone twice CheckBox pressed(or CheckBox initialized)");

				// Set checkbox depending on it's checked status and store
				// variable
				if (playToneTwiceSettingCheckBox.isChecked()) {
					// Store value to variable
					playToneTwice = true;
					// Logging
					logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().playToneTwiceSettingCheckBox.onCheckedChange()", "Play tone twice CheckBox \"Checked\"(" + playToneTwice + ")");
				} else {
					playToneTwice = false;
					logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().playToneTwiceSettingCheckBox.onCheckedChange()", "Play tone twice CheckBox \"Unhecked\"(" + playToneTwice + ")");
				}

				try {
					// Store value to shared preferences
					prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.PLAY_TONE_TWICE_KEY, playToneTwice, SmsAlarm.this);
				} catch (IllegalArgumentException e) {
					logger.logCatTxt(LogPriorities.ERROR, LOG_TAG + ":onCreate().playToneTwiceSettingCheckBox.onCheckedChange()", "An Object of unsupported instance was given as argument to PreferencesHandler.setPrefs()", e);
				}
			}
		});

		// Set listener to enableSmsAlarmCheckBox
		this.enableSmsAlarmCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// Log that CheckBox been pressed
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().enableSmsAlarmCheckBox.onCheckedChange(", "Enable Sms Alarm CheckBox pressed(or CheckBox initialized)");

				// Set checkbox depending on it's checked status and store
				// variable
				if (enableSmsAlarmCheckBox.isChecked()) {
					// Store value to variable
					enableSmsAlarm = true;
					// Logging
					logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().enableSmsAlarmCheckBox.onCheckedChange()", "Enable SmsAlarm CheckBox \"Checked\"(" + enableSmsAlarm + ")");
				} else {
					enableSmsAlarm = false;
					logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().enableSmsAlarmCheckBox.onCheckedChange()", "Enable SmsAlarm CheckBox \"Unchecked\"(" + enableSmsAlarm + ")");
				}

				try {
					// Store value to shared preferences
					prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.ENABLE_SMS_ALARM_KEY, enableSmsAlarm, SmsAlarm.this);
				} catch (IllegalArgumentException e) {
					logger.logCatTxt(LogPriorities.ERROR, LOG_TAG + ":onCreate().enableSmsAlarmCheckBox.onCheckedChange()", "An Object of unsupported instance was given as argument to PreferencesHandler.setPrefs()", e);
				}
			}

		});

		// Set listener to tone spinner
		this.toneSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				// Store tone spinners position to class variable
				toneSpinnerPos = toneSpinner.getSelectedItemPosition();
				// Logging
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":onCreate().toneSpinner.OnItemSelectedListener().onItemSelected()", "Item in tone Spinner pressed(or Spinner initialized)");
				// Update selected tone EditText widget
				updateSelectedToneEditText();
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				// DO NOTHING!
			}
		});

		// Update all UI widgets
		this.updateWholeUI();

		// Log in debugging and information purpose
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":onCreate()", "Creation of Sms Alarm completed");
	}

	/**
	 * To handle events to trigger when activity pauses. <b><i>Not yet
	 * implemented.</i></b>
	 * 
	 * @see #onCreate(Bundle)
	 * @see #onDestroy()
	 */
	@Override
	public void onPause() {
		super.onPause();
		// DO NOTHING!
	}

	/**
	 * To handle events to trigger when activity destroys. Writes all alarms in
	 * database into a <code>.html</code> log file.
	 * 
	 * @see ax.ha.it.smsalarm.LogHandler#logAlarm(List, Context) logAlarm(List,
	 *      Context)
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 * @see ax.ha.it.smsalarm.DatabaseHandler#getAllAlarm() getAllAlarm()
	 * @see ax.ha.it.smsalarm.Alarm ax.ha.it.smsalarm.Alarm
	 * @see ax.ha.it.smsalarm.WidgetProvider#updateWidgets(Context) @see
	 *      ax.ha.it.smsalarm.WidgetProvider#updateWidgets(Context)
	 * @see #onCreate(Bundle)
	 * @see #onPause()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		// Log in debug purpose
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":onDestroy()", this.LOG_TAG + " is about to be destroyed");
		// Get all alarms from database and log them to to html file
		this.logger.logAlarm(this.db.getAllAlarm(), this);
		// Update alla widgets associated to this application
		WidgetProvider.updateWidgets(this);
	}

	/**
	 * To build up the menu, called one time only and that's the first time the
	 * menu is inflated.
	 * 
	 * @see #onOptionsItemSelected(MenuItem)
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":onCreateOptionsMenu()", "Menu created");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * Method to inflate menu with it's items.
	 * 
	 * @see #buildAndShowAboutDialog()
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 * @see ax.ha.it.smsalarm.LogHandler#logCatTxt(LogPriorities, String,
	 *      String, Throwable) logCatTxt(LogPriorities, String, String,
	 *      Throwable)
	 * @see ax.ha.it.smsalarm.PreferencesHandler#setPrefs(PrefKeys, PrefKeys,
	 *      Object, Context) setPrefs(PrefKeys, PrefKeys, Object, Context)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.ABOUT:
			// Logging
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":onOptionsItemSelected()", "Menu item 1 selected");
			// Build up and show the about dialog
			this.buildAndShowAboutDialog();
			return true;
// >>>>DEBUG CASE (DELETE OR COMMENT FOR PROD)
			// case R.id.TEST_SHOW_ACK:
			// try {
			// prefHandler.setPrefs(PrefKeys.SHARED_PREF,
			// PrefKeys.RESCUE_SERVICE_KEY, "Jomala FBK", this);
			// prefHandler.setPrefs(PrefKeys.SHARED_PREF,
			// PrefKeys.FULL_MESSAGE_KEY,
			// "02.02.2012 23:55:40 2.5 Litet larm - Automatlarm vikingline lager(1682) Länsmanshägnan 7 jomala",
			// this);
			// } catch(IllegalArgumentException e) {
			// logger.logCatTxt(LogPriorities.ERROR, this.LOG_TAG +
			// ":onOptionsItemSelected()",
			// "An Object of unsupported instance was given as argument to PreferencesHandler.setPrefs()",
			// e);
			// }
			// Intent i = new Intent(SmsAlarm.this, AcknowledgeHandler.class);
			// startActivityForResult(i, 10);
			// return true;
// <<<<DEBUG CASE (DELETE OR COMMENT FOR PROD)
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * To find UI widgets and get their reference by ID stored in class
	 * variables.
	 * 
	 * @see #onCreate(Bundle)
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 */
	private void findViews() {
		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":findViews()", "Start finding Views by their ID");

		// Declare and initialize variables of type EditText
		this.primaryListenNumberEditText = (EditText) findViewById(R.id.primaryNumber_et);
		this.selectedToneEditText = (EditText) findViewById(R.id.msgTone_et);
		this.ackNumberEditText = (EditText) findViewById(R.id.ackNumber_et);
		this.rescueServiceEditText = (EditText) findViewById(R.id.rescueServiceName_et);

		// Declare and initialize variables of type button
		this.editPrimaryNumberButton = (Button) findViewById(R.id.editPrimaryNumber_btn);
		this.addSecondaryNumberButton = (Button) findViewById(R.id.addSecondaryNumber_btn);
		this.removeSecondaryNumberButton = (Button) findViewById(R.id.deleteSecondaryNumber_btn);
		this.editMsgToneButton = (Button) findViewById(R.id.editMsgTone_btn);
		this.listenMsgToneButton = (Button) findViewById(R.id.listenMsgTone_btn);
		this.ackNumberButton = (Button) findViewById(R.id.editAckNumber_btn);
		this.editRescueServiceButton = (Button) findViewById(R.id.editRescueServiceName_btn);

		// Declare and initialize variables of type CheckBox
		this.soundSettingCheckBox = (CheckBox) findViewById(R.id.useSysSoundSettings_chk);
		this.enableAckCheckBox = (CheckBox) findViewById(R.id.enableAcknowledge_chk);
		this.playToneTwiceSettingCheckBox = (CheckBox) findViewById(R.id.playToneTwiceSetting_chk);
		this.enableSmsAlarmCheckBox = (CheckBox) findViewById(R.id.enableSmsAlarm_chk);

		// Declare and initialize variables of type Spinner
		this.toneSpinner = (Spinner) findViewById(R.id.toneSpinner_sp);
		this.secondaryListenNumberSpinner = (Spinner) findViewById(R.id.secondaryNumberSpinner_sp);

		// Declare and initialize variables of type TextView
		this.soundSettingInfoTextView = (TextView) findViewById(R.id.useSysSoundSettingsHint_tv);
		this.playToneTwiceInfoTextView = (TextView) findViewById(R.id.playToneTwiceSettingHint_tv);
		this.enableSmsAlarmInfoTextView = (TextView) findViewById(R.id.enableSmsAlarmHint_tv);
		this.enableAckInfoTextView = (TextView) findViewById(R.id.enableAcknowledgeHint_tv);

		// If Android API level is greater than 16 we need to adjust some margins
		if (Build.VERSION.SDK_INT > 16) {
			// We need to get some Android resources in order to calculate proper pixel dimensions from dp
			Resources resources = getResources();
			// Calculate pixel dimensions for the different margins
			int pixelsLeft = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, resources.getDisplayMetrics()); // 32dp calculated to pixels
			int pixelsRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, resources.getDisplayMetrics()); // 5dp calculated to pixels
			int pixelsTop = 0;
			// If the locale on device is german(de) set pixelstop to -6dp else -9dp
			if ("de".equals(Locale.getDefault().getLanguage())) {
				// Logging
				this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":findViews()", "The device has german(de) locale, set different margin-top on information TextViews for the checkboxes than other locales");
				pixelsTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -6, resources.getDisplayMetrics()); // -6dp calculated to pixels
			} else {
				pixelsTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -9, resources.getDisplayMetrics()); // -9dp calculated to pixels
			}

			// Set layout parameters for the sound settings info textview
			RelativeLayout.LayoutParams paramsSoundSettingInfoTextView = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT); // Wrap content, both on height and width
			paramsSoundSettingInfoTextView.setMargins(pixelsLeft, pixelsTop, pixelsRight, 0); // Margins left, top, right, bottom
			paramsSoundSettingInfoTextView.addRule(RelativeLayout.BELOW, this.soundSettingCheckBox.getId()); // Add rule, below UI widget
			paramsSoundSettingInfoTextView.addRule(RelativeLayout.ALIGN_LEFT, this.soundSettingCheckBox.getId()); // Add rule, align left of UI widget

			// Set layout parameters for the play tone twice textview
			RelativeLayout.LayoutParams paramsPlayToneTwiceInfoTextView = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			paramsPlayToneTwiceInfoTextView.setMargins(pixelsLeft, pixelsTop, pixelsRight, 0);
			paramsPlayToneTwiceInfoTextView.addRule(RelativeLayout.BELOW, this.playToneTwiceSettingCheckBox.getId());
			paramsPlayToneTwiceInfoTextView.addRule(RelativeLayout.ALIGN_LEFT, this.playToneTwiceSettingCheckBox.getId());

			// Set layout parameters for the enable ack info textview
			RelativeLayout.LayoutParams paramsEnableAckInfoTextView = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			paramsEnableAckInfoTextView.setMargins(pixelsLeft, pixelsTop, pixelsRight, 0);
			paramsEnableAckInfoTextView.addRule(RelativeLayout.BELOW, this.enableAckCheckBox.getId());
			paramsEnableAckInfoTextView.addRule(RelativeLayout.ALIGN_LEFT, this.enableAckCheckBox.getId());

			// Set layout parameters for the enable sms alarm info textview
			RelativeLayout.LayoutParams paramsEnableSmsAlarmInfoTextView = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			paramsEnableSmsAlarmInfoTextView.setMargins(pixelsLeft, pixelsTop, pixelsRight, 0);
			paramsEnableSmsAlarmInfoTextView.addRule(RelativeLayout.BELOW, this.enableSmsAlarmCheckBox.getId());
			paramsEnableSmsAlarmInfoTextView.addRule(RelativeLayout.ALIGN_LEFT, this.enableSmsAlarmCheckBox.getId());

			// Apply the previously configured layout parameters to the correct
			// textviews
			this.soundSettingInfoTextView.setLayoutParams(paramsSoundSettingInfoTextView);
			this.playToneTwiceInfoTextView.setLayoutParams(paramsPlayToneTwiceInfoTextView);
			this.enableAckInfoTextView.setLayoutParams(paramsEnableAckInfoTextView);
			this.enableSmsAlarmInfoTextView.setLayoutParams(paramsEnableSmsAlarmInfoTextView);

			// Logging
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":findViews()", "API level > 16, edit margins on information TextViews for the checkboxes");
		} else { // The device has API level < 17, we just need to check if the locale is german
			// If the locale on device is german(de) we need to adjust the margin top for the information textviews for the chockboxes to -6dp
			if ("de".equals(Locale.getDefault().getLanguage())) {
				// We need to get some Android resources in order to calculate proper pixel dimensions from dp
				Resources resources = getResources();
				
				// Calculate pixel dimensions for the different margins
				int pixelsLeft = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 38, resources.getDisplayMetrics()); // 38dp calculated to pixels
				int pixelsRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, resources.getDisplayMetrics()); // 5dp calculated to pixels
				int pixelsTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -6, resources.getDisplayMetrics()); // -6dp calculated to pixels
				
				// Set layout parameters for the sound settings info textview
				RelativeLayout.LayoutParams paramsSoundSettingInfoTextView = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT); // Wrap content, both on height and width
				paramsSoundSettingInfoTextView.setMargins(pixelsLeft, pixelsTop, pixelsRight, 0); // Margins left, top, right, bottom
				paramsSoundSettingInfoTextView.addRule(RelativeLayout.BELOW, this.soundSettingCheckBox.getId()); // Add rule, below UI widget
				paramsSoundSettingInfoTextView.addRule(RelativeLayout.ALIGN_LEFT, this.soundSettingCheckBox.getId()); // Add rule, align left of UI widget

				// Set layout parameters for the play tone twice textview
				RelativeLayout.LayoutParams paramsPlayToneTwiceInfoTextView = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				paramsPlayToneTwiceInfoTextView.setMargins(pixelsLeft, pixelsTop, pixelsRight, 0);
				paramsPlayToneTwiceInfoTextView.addRule(RelativeLayout.BELOW, this.playToneTwiceSettingCheckBox.getId());
				paramsPlayToneTwiceInfoTextView.addRule(RelativeLayout.ALIGN_LEFT, this.playToneTwiceSettingCheckBox.getId());

				// Set layout parameters for the enable ack info textview
				RelativeLayout.LayoutParams paramsEnableAckInfoTextView = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				paramsEnableAckInfoTextView.setMargins(pixelsLeft, pixelsTop, pixelsRight, 0);
				paramsEnableAckInfoTextView.addRule(RelativeLayout.BELOW, this.enableAckCheckBox.getId());
				paramsEnableAckInfoTextView.addRule(RelativeLayout.ALIGN_LEFT, this.enableAckCheckBox.getId());

				// Set layout parameters for the enable sms alarm info textview
				RelativeLayout.LayoutParams paramsEnableSmsAlarmInfoTextView = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				paramsEnableSmsAlarmInfoTextView.setMargins(pixelsLeft, pixelsTop, pixelsRight, 0);
				paramsEnableSmsAlarmInfoTextView.addRule(RelativeLayout.BELOW, this.enableSmsAlarmCheckBox.getId());
				paramsEnableSmsAlarmInfoTextView.addRule(RelativeLayout.ALIGN_LEFT, this.enableSmsAlarmCheckBox.getId());

				// Apply the previously configured layout parameters to the correct
				// textviews
				this.soundSettingInfoTextView.setLayoutParams(paramsSoundSettingInfoTextView);
				this.playToneTwiceInfoTextView.setLayoutParams(paramsPlayToneTwiceInfoTextView);
				this.enableAckInfoTextView.setLayoutParams(paramsEnableAckInfoTextView);
				this.enableSmsAlarmInfoTextView.setLayoutParams(paramsEnableSmsAlarmInfoTextView);

				// Logging
				this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":findViews()", "API level < 17 but the device has german(de) locale, set different margin-top on information TextViews for the checkboxes to fit the language");	
			} 		
		}

		// Declare and initialize variables of type ImageView
		this.divider1ImageView = (ImageView) findViewById(R.id.mainDivider1_iv);
		this.divider2ImageView = (ImageView) findViewById(R.id.mainDivider2_iv);
		this.divider3ImageView = (ImageView) findViewById(R.id.mainDivider3_iv);

		// If Android API level less then 11 set bright gradient else set dark gradient
		if (Build.VERSION.SDK_INT < 11) {
			this.divider1ImageView.setImageResource(R.drawable.gradient_divider_10_and_down);
			this.divider2ImageView.setImageResource(R.drawable.gradient_divider_10_and_down);
			this.divider3ImageView.setImageResource(R.drawable.gradient_divider_10_and_down);
			// Logging
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":findViews()", "API level < 11, set bright gradients");
		} else {
			this.divider1ImageView.setImageResource(R.drawable.gradient_divider_11_and_up);
			this.divider2ImageView.setImageResource(R.drawable.gradient_divider_11_and_up);
			this.divider3ImageView.setImageResource(R.drawable.gradient_divider_11_and_up);
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":findViews()", "API level > 10, set dark gradients");
		}

		// Set some attributes to the smsPrimaryNumberEditText
		this.primaryListenNumberEditText.setEnabled(false);
		this.primaryListenNumberEditText.setClickable(false);
		this.primaryListenNumberEditText.setFocusable(false);
		this.primaryListenNumberEditText.setBackgroundColor(Color.WHITE);
		this.primaryListenNumberEditText.setTextColor(Color.BLACK);

		// Set some attributes to the ackNumberEditText
		this.ackNumberEditText.setEnabled(false);
		this.ackNumberEditText.setClickable(false);
		this.ackNumberEditText.setFocusable(false);
		this.ackNumberEditText.setBackgroundColor(Color.WHITE);

		// Set some attributes to the fireDepartmentEditText
		this.rescueServiceEditText.setEnabled(false);
		this.rescueServiceEditText.setClickable(false);
		this.rescueServiceEditText.setFocusable(false);
		this.rescueServiceEditText.setBackgroundColor(Color.WHITE);
		this.rescueServiceEditText.setTextColor(Color.BLACK);

		// Set some attributes to the selectedToneEditText
		this.selectedToneEditText.setEnabled(false);
		this.selectedToneEditText.setClickable(false);
		this.selectedToneEditText.setFocusable(false);
		this.selectedToneEditText.setBackgroundColor(Color.WHITE);
		this.selectedToneEditText.setTextColor(Color.BLACK);

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":findViews()", "All Views found");
	}

	/**
	 * To set all <code>Shared Preferences</code> used by class
	 * <code>SmsAlarm</code>.
	 * 
	 * @see #getSmsAlarmPrefs()
	 * @see ax.ha.it.smsalarm.PreferencesHandler#setPrefs(PrefKeys, PrefKeys,
	 *      Object, Context) setPrefs(PrefKeys, PrefKeys, Object, Context)
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 * @see ax.ha.it.smsalarm.LogHandler#logCatTxt(LogPriorities, String,
	 *      String, Throwable) logCatTxt(LogPriorities, String, String,
	 *      Throwable)
	 */
	private void setSmsAlarmPrefs() {
		// Some logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":setSmsAlarmPrefs()", "Start setting shared preferences used by class SmsAlarm");

		try {
			// Set preferences used by class Sms Alarm
			prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.PRIMARY_LISTEN_NUMBER_KEY, this.primaryListenNumber, this);
			prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.SECONDARY_LISTEN_NUMBERS_KEY, this.secondaryListenNumbers, this);
			prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.PRIMARY_MESSAGE_TONE_KEY, this.primaryMessageToneId, this);
			prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.SECONDARY_MESSAGE_TONE_KEY, this.secondaryMessageToneId, this);
			prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.USE_OS_SOUND_SETTINGS_KEY, this.useOsSoundSettings, this);
			prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.ENABLE_ACK_KEY, this.useAlarmAcknowledge, this);
			prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.ACK_NUMBER_KEY, this.acknowledgeNumber, this);
			prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.PLAY_TONE_TWICE_KEY, this.playToneTwice, this);
			prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.ENABLE_SMS_ALARM_KEY, this.enableSmsAlarm, this);
			prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.RESCUE_SERVICE_KEY, this.rescueService, this);
		} catch (IllegalArgumentException e) {
			this.logger.logCatTxt(LogPriorities.ERROR, this.LOG_TAG + ":setSmsAlarmPrefs()", "An Object of unsupported instance was given as argument to PreferencesHandler.setPrefs()", e);
		}
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":setSmsAlarmPrefs()", "Shared preferences set");
	}

	/**
	 * To get <code>Shared Preferences</code> used by class
	 * <code>SmsAlarm</code>.
	 * 
	 * @see #setSmsAlarmPrefs()
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 * @see ax.ha.it.smsalarm.LogHandler#logCatTxt(LogPriorities, String,
	 *      String, Throwable) logCatTxt(LogPriorities, String, String,
	 *      Throwable)
	 * @see ax.ha.it.smsalarm.PreferencesHandler#getPrefs(PrefKeys, PrefKeys,
	 *      DataTypes, Context) getPrefs(PrefKeys, PrefKeys, DataTypes, Context)
	 * @see ax.ha.it.smsalarm.PreferencesHandler#getPrefs(PrefKeys, PrefKeys,
	 *      DataTypes, Context, Object) getPrefs(PrefKeys, PrefKeys, DataTypes,
	 *      Context, Object)
	 */
	@SuppressWarnings("unchecked")
	private void getSmsAlarmPrefs() {
		// Some logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":getSmsAlarmPrefs()", "Start retrieving shared preferences needed by class SmsAlarm");

		try {
			// Get shared preferences needed by class Sms Alarm
			this.primaryListenNumber = (String) prefHandler.getPrefs(PrefKeys.SHARED_PREF, PrefKeys.PRIMARY_LISTEN_NUMBER_KEY, DataTypes.STRING, this);
			this.secondaryListenNumbers = (List<String>) prefHandler.getPrefs(PrefKeys.SHARED_PREF, PrefKeys.SECONDARY_LISTEN_NUMBERS_KEY, DataTypes.LIST, this);
			this.primaryMessageToneId = (Integer) prefHandler.getPrefs(PrefKeys.SHARED_PREF, PrefKeys.PRIMARY_MESSAGE_TONE_KEY, DataTypes.INTEGER, this);
			this.secondaryMessageToneId = (Integer) prefHandler.getPrefs(PrefKeys.SHARED_PREF, PrefKeys.SECONDARY_MESSAGE_TONE_KEY, DataTypes.INTEGER, this, 1);
			this.useOsSoundSettings = (Boolean) prefHandler.getPrefs(PrefKeys.SHARED_PREF, PrefKeys.USE_OS_SOUND_SETTINGS_KEY, DataTypes.BOOLEAN, this);
			this.useAlarmAcknowledge = (Boolean) prefHandler.getPrefs(PrefKeys.SHARED_PREF, PrefKeys.ENABLE_ACK_KEY, DataTypes.BOOLEAN, this);
			this.acknowledgeNumber = (String) prefHandler.getPrefs(PrefKeys.SHARED_PREF, PrefKeys.ACK_NUMBER_KEY, DataTypes.STRING, this);
			this.playToneTwice = (Boolean) prefHandler.getPrefs(PrefKeys.SHARED_PREF, PrefKeys.PLAY_TONE_TWICE_KEY, DataTypes.BOOLEAN, this);
			this.enableSmsAlarm = (Boolean) prefHandler.getPrefs(PrefKeys.SHARED_PREF, PrefKeys.ENABLE_SMS_ALARM_KEY, DataTypes.BOOLEAN, this, true);
			this.rescueService = (String) prefHandler.getPrefs(PrefKeys.SHARED_PREF, PrefKeys.RESCUE_SERVICE_KEY, DataTypes.STRING, this);
		} catch (IllegalArgumentException e) {
			logger.logCatTxt(LogPriorities.ERROR, this.LOG_TAG + ":getSmsAlarmPrefs()", "An unsupported datatype was given as argument to PreferencesHandler.getPrefs()", e);
		}
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":getSmsAlarmPrefs()", "Shared preferences retrieved");
	}

	/**
	 * To build up a dialog prompting user if it's okay to delete the selected
	 * secondary listen number.
	 * 
	 * @see #buildAndShowAboutDialog()
	 * @see #buildAndShowInputDialog(DialogTypes)
	 * @see #buildAndShowToneDialog()
	 * @see #updateSecondaryListenNumberSpinner()
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 * @see ax.ha.it.smsalarm.LogHandler#logCatTxt(LogPriorities, String,
	 *      String, Throwable) logCatTxt(LogPriorities, String, String,
	 *      Throwable)
	 * @see ax.ha.it.smsalarm.PreferencesHandler#setPrefs(PrefKeys, PrefKeys,
	 *      Object, Context) setPrefs(PrefKeys, PrefKeys, Object, Context)
	 */
	private void buildAndShowDeleteSecondaryNumberDialog() {
		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":buildAndShowDeleteSecondaryNumberDialog()", "Start building delete SECONDARY number dialog");

		// Store secondaryListenNumberSpinner position
		final int position = secondaryListenNumberSpinner.getSelectedItemPosition();

		// String to store complete prompt message in
		String promptMessage = getString(R.string.DELETE_SECONDARY_NUMBER_PROMPT_MESSAGE) + " " + secondaryListenNumbers.get(position) + "?";

		// Build up the alert dialog
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);

		// Set some attributes, title and message containing actual number
		dialog.setIcon(android.R.drawable.ic_dialog_alert);
		dialog.setTitle(R.string.DELETE_SECONDARY_NUMBER_PROMPT_TITLE);
		dialog.setMessage(promptMessage);

		// Set dialog to non cancelable
		dialog.setCancelable(false);

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":buildAndShowDeleteSecondaryNumberDialog()", "Dialog attributes set");

		// Set a positive button and listen on it
		dialog.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				// Log information
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowDeleteSecondaryNumberDialog().PosButton.OnClickListener().onClick()", "Positive Button pressed");
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowDeleteSecondaryNumberDialog().PosButton.OnClickListener().onClick()", "SECONDARY listen number: \"" + secondaryListenNumbers.get(position) + "\" is about to be removed from list of SECONDARY listen numbers");
				// Delete number from list
				secondaryListenNumbers.remove(position);
				try {
					// Store to shared preferences
					prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.SECONDARY_LISTEN_NUMBERS_KEY, secondaryListenNumbers, SmsAlarm.this);
				} catch (IllegalArgumentException e) {
					logger.logCatTxt(LogPriorities.ERROR, LOG_TAG + ":buildAndShowDeleteSecondaryNumberDialog().PosButton.OnClickListener().onClick()", "An Object of unsupported instance was given as argument to PreferencesHandler.setPrefs()", e);
				}
				// Update affected UI widgets
				updateSecondaryListenNumberSpinner();
			}
		});

		// Set a neutral button, due to documentation it has same functionality
		// as "back" button
		dialog.setNeutralButton(R.string.NO, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				// DO NOTHING, except logging
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowDeleteSecondaryNumberDialog().NeutralButton.OnClickListener().onClick()", "Neutral Button pressed in dialog, nothing done");
			}
		});

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowDeleteSecondaryNumberDialog()", "Showing dialog");

		// Show it
		dialog.show();
	}

	/**
	 * Universal method to build of four different types of input dialogs. The
	 * supported types are: <b><i>PRIMARY</b></i>, <b><i>SECONDARY</b></i>,
	 * <b><i>ACKNOWLEDGE</b></i> and <b><i>RESCUESERVICE</b></i>. If a dialog
	 * type are given as parameter thats not supported a dummy dialog will be
	 * built and shown.
	 * 
	 * @param type
	 *            Type of dialog to build up and show
	 * 
	 * @see #buildAndShowAboutDialog()
	 * @see #buildAndShowToneDialog()
	 * @see #buildAndShowDeleteSecondaryNumberDialog()
	 * @see #updatePrimaryListenNumberEditText()
	 * @see #updateSecondaryListenNumberSpinner()
	 * @see #updateAcknowledgeNumberEditText()
	 * @see #updateRescueServiceEditText()
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 * @see ax.ha.it.smsalarm.LogHandler#logCatTxt(LogPriorities, String,
	 *      String) logCatTxt(LogPriorities, String, String)
	 * @see ax.ha.it.smsalarm.LogHandler#logCatTxt(LogPriorities, String,
	 *      String, Throwable) logCatTxt(LogPriorities, String, String,
	 *      Throwable)
	 * @see ax.ha.it.smsalarm.PreferencesHandler#setPrefs(PrefKeys, PrefKeys,
	 *      Object, Context) setPrefs(PrefKeys, PrefKeys, Object, Context)
	 */
	private void buildAndShowInputDialog(final DialogTypes type) {
		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":buildAndShowInputDialog()", "Start building dialog");

		// Build up the alert dialog
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);

		// Set some attributes
		dialog.setIcon(android.R.drawable.ic_dialog_info);

		// Set an EditText view to get user input
		final EditText input = new EditText(this);

		/*
		 * Switch through the different dialog types and set correct strings and
		 * edittext to the dialog. If dialog type is non supported a default
		 * dialog DUMMY is built up.
		 */
		switch (type) {
		case PRIMARY:
			// Set title
			dialog.setTitle(R.string.NUMBER_PROMPT_TITLE);
			// Set message
			dialog.setMessage(R.string.PRIMARY_NUMBER_PROMPT_MESSAGE);
			// Set hint to edittext
			input.setHint(R.string.NUMBER_PROMPT_HINT);
			// Set Input type to edittext
			input.setInputType(InputType.TYPE_CLASS_NUMBER);
			// Set dialog to non cancelable
			dialog.setCancelable(false);
			// Bind dialog to input
			dialog.setView(input);
			// Logging
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":buildAndShowInputDialog()", "Dialog attributes is set for dialog type PRIMARY");
			break;
		case SECONDARY:
			dialog.setTitle(R.string.NUMBER_PROMPT_TITLE);
			dialog.setMessage(R.string.SECONDARY_NUMBER_PROMPT_MESSAGE);
			input.setHint(R.string.NUMBER_PROMPT_HINT);
			input.setInputType(InputType.TYPE_CLASS_NUMBER);
			dialog.setCancelable(false);
			dialog.setView(input);
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":buildAndShowInputDialog()", "Dialog attributes is set for dialog type SECONDARY");
			break;
		case ACKNOWLEDGE:
			dialog.setTitle(R.string.NUMBER_PROMPT_TITLE);
			dialog.setMessage(R.string.ACK_NUMBER_PROMPT_MESSAGE);
			input.setHint(R.string.NUMBER_PROMPT_HINT);
			input.setInputType(InputType.TYPE_CLASS_NUMBER);
			dialog.setCancelable(false);
			dialog.setView(input);
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":buildAndShowInputDialog()", "Dialog attributes is set for dialog type ACKNOWLEDGE");
			break;
		case RESCUESERVICE:
			dialog.setTitle(R.string.RESCUE_SERVICE_PROMPT_TITLE);
			dialog.setMessage(R.string.RESCUE_SERVICE_PROMPT_MESSAGE);
			input.setHint(R.string.RESCUE_SERVICE_NAME_HINT);
			input.setInputType(InputType.TYPE_CLASS_TEXT);
			dialog.setCancelable(false);
			dialog.setView(input);
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":buildAndShowInputDialog()", "Dialog attributes is set for dialog type RESCUESERVICE");
			break;
		default: // <--Unsupported dialog type. Displaying a dummy dialog!
			dialog.setTitle("Congratulations!");
			dialog.setMessage("Somehow you got this dialog to show up! I bet a monkey must have been messing around with the code;-)");
			dialog.setCancelable(false);
			this.logger.logCatTxt(LogPriorities.ERROR, this.LOG_TAG + ":buildAndShowInputDialog()", "A UNSUPPORTED dialog type has been given as parameter, a DUMMY dialog will be built and shown");
		}

		// Set a positive button and listen on it
		dialog.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				// Log information
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowInputDialog().PositiveButton.OnClickListener().onClick()", "Positive Button pressed");

				// Boolean indicating if there are duplicates of primary and secondary alarm numbers
				boolean duplicatedNumbers = false;

				/*
				 * Switch through the different dialog types and set proper
				 * input handling to each of them. If dialog type is non
				 * supported no input is taken.
				 */
				switch (type) {
				case PRIMARY:
					// If list is not empty there are numbers to equalize with each other, else just store the input
					if (!secondaryListenNumbers.isEmpty()) {
						// Iterate through all strings in the list
						for (int i = 0; i < secondaryListenNumbers.size(); i++) {
							// If a string in the list is equal with the input then it's a duplicated
							if (secondaryListenNumbers.get(i).equals(input.getText().toString()) && !input.getText().toString().equals("")) {
								duplicatedNumbers = true;
							}
						}

						// Store input if no duplication of numbers exists, else prompt user for number again
						if (!duplicatedNumbers) {
							// Store input to class variable
							primaryListenNumber = input.getText().toString();
							try {
								// Store to shared preferences
								prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.PRIMARY_LISTEN_NUMBER_KEY, primaryListenNumber, SmsAlarm.this);
							} catch (IllegalArgumentException e) {
								logger.logCatTxt(LogPriorities.ERROR, LOG_TAG + ":buildAndShowInputDialog().PositiveButton.OnClickListener().onClick()", "An Object of unsupported instance was given as argument to PreferencesHandler.setPrefs()", e);
							}
							// Update affected UI widgets
							updatePrimaryListenNumberEditText();
							// Log
							logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowInputDialog().PositiveButton.OnClickListener().onClick()", "New PRIMARY phone number has been stored from user input. New PRIMARY phone number is: \"" + primaryListenNumber + "\"");
						} else {
							Toast.makeText(SmsAlarm.this, R.string.DUPLICATED_NUMBERS, Toast.LENGTH_LONG).show();
							logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowInputDialog().PositiveButton.OnClickListener().onClick()", "Given PRIMARY phone number(" + input.getText().toString() + ") exists in the list of SECONDARY phone numbers and therefore cannot be stored. Showing dialog of type PRIMARY again");
							buildAndShowInputDialog(type);
						}
					} else {
						primaryListenNumber = input.getText().toString();
						try {
							prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.PRIMARY_LISTEN_NUMBER_KEY, primaryListenNumber, SmsAlarm.this);
						} catch (IllegalArgumentException e) {
							logger.logCatTxt(LogPriorities.ERROR, LOG_TAG + ":buildAndShowInputDialog().PositiveButton.OnClickListener().onClick()", "An Object of unsupported instance was given as argument to PreferencesHandler.setPrefs()", e);
						}
						updatePrimaryListenNumberEditText();
						logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowInputDialog().PositiveButton.OnClickListener().onClick()", "New PRIMARY phone number has been stored from user input. New PRIMARY phone number is: \"" + primaryListenNumber + "\"");
					}
					break;
				case SECONDARY:
					// If input isn't equal with the primaryListenNumber and input isn't empty
					if (!primaryListenNumber.equals(input.getText().toString()) && !input.getText().toString().equals("")) {
						// Iterate through all strings in the list to check if number already exists in list
						for (int i = 0; i < secondaryListenNumbers.size(); i++) {
							// If a string in the list is equal with the input then it's a duplicated
							if (secondaryListenNumbers.get(i).equals(input.getText().toString()) && !input.getText().toString().equals("")) {
								duplicatedNumbers = true;
							}
						}

						// Store input if duplicated numbers is false
						if (!duplicatedNumbers) {
							// Add given input to list
							secondaryListenNumbers.add(input.getText().toString());
							try {
								// Store to shared preferences
								prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.SECONDARY_LISTEN_NUMBERS_KEY, secondaryListenNumbers, SmsAlarm.this);
							} catch (IllegalArgumentException e) {
								logger.logCatTxt(LogPriorities.ERROR, LOG_TAG + ":buildAndShowInputDialog().PositiveButton.OnClickListener().onClick()", "An Object of unsupported instance was given as argument to PreferencesHandler.setPrefs()", e);
							}
							// Update affected UI widgets
							updateSecondaryListenNumberSpinner();
							// Log
							logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowInputDialog().PositiveButton.OnClickListener().onClick()", "New SECONDARY phone number has been stored from user input to the list of SECONDARY phone numbers . New SECONDARY phone number is: \"" + input.getText().toString() + "\"");
						} else {
							Toast.makeText(SmsAlarm.this, R.string.NUMBER_ALREADY_IN_LIST, Toast.LENGTH_LONG).show();
							logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowInputDialog().PositiveButton.OnClickListener().onClick()", "Given SECONDARY phone number(" + input.getText().toString() + ") already exists in the list of SECONDARY phone numbers and therefore cannot be stored. Showing dialog of type SECONDARY again");
							buildAndShowInputDialog(type);
						}
					} else {
						// Empty secondary number was given
						if (input.getText().toString().equals("")) {
							Toast.makeText(SmsAlarm.this, R.string.EMPTY_SECONDARY_NUMBER, Toast.LENGTH_LONG).show();
							logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowInputDialog().PositiveButton.OnClickListener().onClick()", "Given SECONDARY phone number is empty and therefore cannot be stored. Showing dialog of type SECONDARY again");
						} else { // Given secondary number is the same as primary number
							Toast.makeText(SmsAlarm.this, R.string.DUPLICATED_NUMBERS, Toast.LENGTH_LONG).show();
							logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowInputDialog().PositiveButton.OnClickListener().onClick()", "Given SECONDARY phone number(" + input.getText().toString() + ") is the same as the PRIMARY phone number and therefore cannot be stored. Showing dialog of type SECONDARY again");
						}
						buildAndShowInputDialog(type);
					}
					break;
				case ACKNOWLEDGE:
					// Store input to class variable
					acknowledgeNumber = input.getText().toString();
					try {
						// Store to shared preferences
						prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.ACK_NUMBER_KEY, acknowledgeNumber, SmsAlarm.this);
					} catch (IllegalArgumentException e) {
						logger.logCatTxt(LogPriorities.ERROR, LOG_TAG + ":buildAndShowInputDialog().PositiveButton.OnClickListener().onClick()", "An Object of unsupported instance was given as argument to PreferencesHandler.setPrefs()", e);
					}
					// update affected UI widgets
					updateAcknowledgeNumberEditText();
					// Log
					logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowInputDialog().PositiveButton.OnClickListener().onClick()", "New ACKNOWLEDGE phone number has been stored from user input . New ACKNOWLEDGE phone number is: \"" + acknowledgeNumber + "\"");
					break;
				case RESCUESERVICE:
					// Store input to class variable
					rescueService = input.getText().toString();
					try {
						// Store to shared preferences
						prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.RESCUE_SERVICE_KEY, rescueService, SmsAlarm.this);
					} catch (IllegalArgumentException e) {
						logger.logCatTxt(LogPriorities.ERROR, LOG_TAG + ":buildAndShowInputDialog().PositiveButton.OnClickListener().onClick()", "An Object of unsupported instance was given as argument to PreferencesHandler.setPrefs()", e);
					}
					// Update affected UI widgets
					updateRescueServiceEditText();
					// Log
					logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowInputDialog().PositiveButton.OnClickListener().onClick()", "New RESCUESERVICE name has been stored from user input . New RESCUESERVICE name is: \"" + rescueService + "\"");
					break;
				default: // <--Unsupported dialog type
					logger.logCatTxt(LogPriorities.ERROR, LOG_TAG + ":buildAndShowInputDialog().PositiveButton.OnClickListener().onClick()", "Nothing is stored beacause given dialog type is UNSUPPORTED, given dialog is of type number: \"" + type.name() + "\"");
				}
			}
		});

		// Only set neutral button if dialog type is supported
		if (type.ordinal() >= DialogTypes.PRIMARY.ordinal() && type.ordinal() <= DialogTypes.RESCUESERVICE.ordinal()) {
			// Set a neutral button, due to documentation it has same functionality as "back" button
			dialog.setNeutralButton(R.string.CANCEL, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					// DO NOTHING, except logging
					logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowInputDialog().NeutralButton.OnClickListener().onClick()", "Neutral Button pressed in dialog, nothing done");
				}
			});
		}

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowInputDialog()", "Showing dialog");

		// Show it
		dialog.show();
	}

	/**
	 * To build up and show a dialog with a list populated with message tones.
	 * User chooses applications message tones from that list.
	 * 
	 * @see #buildAndShowInputDialog(DialogTypes)
	 * @see #buildAndShowAboutDialog()
	 * @see #updateSelectedToneEditText()
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 * @see ax.ha.it.smsalarm.LogHandler#logCatTxt(LogPriorities, String,
	 *      String) logCatTxt(LogPriorities, String, String)
	 * @see ax.ha.it.smsalarm.LogHandler#logCatTxt(LogPriorities, String,
	 *      String, Throwable) logCatTxt(LogPriorities, String, String,
	 *      Throwable)
	 * @see ax.ha.it.smsalarm.PreferencesHandler#setPrefs(PrefKeys, PrefKeys,
	 *      Object, Context) setPrefs(PrefKeys, PrefKeys, Object, Context)
	 */
	private void buildAndShowToneDialog() {
		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":buildAndShowToneDialog()", "Start building tone dialog");

		// Build up the alert dialog
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);

		// Set attributes
		dialog.setIcon(android.R.drawable.ic_dialog_info);
		dialog.setTitle(R.string.TONE_PROMPT_TITLE);
		dialog.setCancelable(false);

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":buildAndShowToneDialog()", "Dialog attributes set");

		// Set items to list view from resource array tones
		dialog.setItems(R.array.tones, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int listPosition) {
				// Log information
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowToneDialog().Item.OnClickListener().onClick()", "Item in message tones list pressed");

				// Store position(toneId) in correct variable, depending on spinner value
				if (toneSpinnerPos == 0) { // <--PRIMARY MESSAGE TONE
					// Store primary message tone id from position of list
					primaryMessageToneId = listPosition;
					// Log information
					logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowToneDialog().Item.OnClickListener().onClick()", "New PRIMARY message tone selected. Tone: \"" + noiseHandler.msgToneLookup(SmsAlarm.this, primaryMessageToneId) + "\", id: \"" + primaryMessageToneId + "\" and tone Spinner position: \"" + Integer.toString(toneSpinnerPos) + "\"");
					try {
						// Store primary message tone id to preferences to preferences
						prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.PRIMARY_MESSAGE_TONE_KEY, primaryMessageToneId, SmsAlarm.this);
					} catch (IllegalArgumentException e) {
						logger.logCatTxt(LogPriorities.ERROR, LOG_TAG + ":buildAndShowToneDialog().Item.OnClickListener().onClick()", "An Object of unsupported instance was given as argument to PreferencesHandler.setPrefs()", e);
					}
					// Update selected tone EditText
					updateSelectedToneEditText();
				} else if (toneSpinnerPos == 1) { // <--SECONDARY MESSAGE TONE
					secondaryMessageToneId = listPosition;
					logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowToneDialog().Item.OnClickListener().onClick()", "New SECONDARY message tone selected. Tone: \"" + noiseHandler.msgToneLookup(SmsAlarm.this, secondaryMessageToneId) + "\", id: \"" + secondaryMessageToneId + "\" and tone Spinner position: \"" + Integer.toString(toneSpinnerPos) + "\"");
					try {
						prefHandler.setPrefs(PrefKeys.SHARED_PREF, PrefKeys.SECONDARY_MESSAGE_TONE_KEY, secondaryMessageToneId, SmsAlarm.this);
					} catch (IllegalArgumentException e) {
						logger.logCatTxt(LogPriorities.ERROR, LOG_TAG + ":buildAndShowToneDialog().Item.OnClickListener().onClick()", "An Object of unsupported instance was given as argument to PreferencesHandler.setPrefs()", e);
					}
					updateSelectedToneEditText();
				} else { // <--UNSUPPORTED SPINNER POSITION
					// DO NOTHING EXCEPT LOG ERROR MESSAGE
					logger.logCatTxt(LogPriorities.ERROR, LOG_TAG + ":buildAndShowToneDialog().Item.OnClickListener().onClick()", "Invalid spinner position occurred. Current tone Spinner position is: \"" + Integer.toString(toneSpinnerPos) + "\"");
				}
			}
		});

		// Set a neutral button and listener
		dialog.setNeutralButton(R.string.CANCEL, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				// DO NOTHING, except logging
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowToneDialog().NeutralButton.OnClickListener().onClick()", "Neutral Button pressed in dialog, nothing done");
			}
		});

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":buildAndShowToneDialog()", "Showing dialog");

		// Show dialog
		dialog.show();
	}

	/**
	 * To build up and show an about dialog.
	 * 
	 * @see #buildAndShowDeleteSecondaryNumberDialog()
	 * @see #buildAndShowInputDialog(DialogTypes)
	 * @see #buildAndShowToneDialog()
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 */
	private void buildAndShowAboutDialog() {
		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":buildAndShowAboutDialog()", "Start building about dialog");

		// Build up the alert dialog
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);

		LayoutInflater factory = LayoutInflater.from(this);

		final View view = factory.inflate(R.layout.about, null);

		// Get TextViews from its view
		TextView buildTextView = (TextView) view.findViewById(R.id.aboutBuild_tv);
		TextView versionTextView = (TextView) view.findViewById(R.id.aboutVersion_tv);
		
		// Set correct text, build and version number, to the TextViews
		buildTextView.setText(String.format(getString(R.string.ABOUT_BUILD), getString(R.string.APP_BUILD)));
		versionTextView.setText(String.format(getString(R.string.ABOUT_VERSION), getString(R.string.APP_VERSION)));
		
		// Set correct icon depending on api level
		if (Build.VERSION.SDK_INT < 11) {
			dialog.setIcon(R.drawable.ic_launcher_trans_10_and_down);
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":buildAndShowAboutDialog()", "API level < 11, set icon adapted to black background color");
		} else {
			dialog.setIcon(R.drawable.ic_launcher_trans);
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":buildAndShowAboutDialog()", "API level > 10, set icon adapted to white background color");
		}
		
		// Set rest of the attributes
		dialog.setTitle(R.string.ABOUT);
		dialog.setView(view);
		dialog.setCancelable(false);

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":buildAndShowAboutDialog()", "Dialog attributes set");

		// Set a neutral button
		dialog.setNeutralButton(R.string.OK, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				// DO NOTHING, except logging
				logger.logCat(LogPriorities.DEBUG, LOG_TAG + ":buildAndShowAboutDialog().NeutralButton.OnClickListener().onClick()", "Neutral Button pressed in dialog, nothing done");
			}
		});

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":buildAndShowAboutDialog()", "Showing dialog");

		// Show dialog
		dialog.show();
	}

	/**
	 * To update Sms Alarms whole User Interface.
	 * 
	 * @see #updatePrimaryListenNumberEditText()
	 * @see #updateSecondaryListenNumberSpinner()
	 * @see #updateAcknowledgeNumberEditText()
	 * @see #updateRescueServiceEditText()
	 * @see #updateSelectedToneEditText()
	 * @see #updateUseOsSoundSettingsCheckbox()
	 * @see #updatePlayToneTwiceCheckBox()
	 * @see #updateEnableSmsAlarmCheckBox()
	 * @see #updateAcknowledgeWidgets()
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 */
	private void updateWholeUI() {
		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateWholeUI", "Whole user interface is about to be updated");

		// Update primary listen number EditText
		this.updatePrimaryListenNumberEditText();

		// Update secondary listen numbers Spinner
		this.updateSecondaryListenNumberSpinner();

		// Update acknowledge number EditText
		this.updateAcknowledgeNumberEditText();

		// Update rescue service EditText
		this.updateRescueServiceEditText();

		// Update selected EditText widget
		this.updateSelectedToneEditText();

		// Update use OS sound settings CheckBox widget
		this.updateUseOsSoundSettingsCheckbox();

		// Update play tone twice CheckBox widget
		this.updatePlayToneTwiceCheckBox();

		// Update enable Sms Alarm CheckBox widget
		this.updateEnableSmsAlarmCheckBox();

		// Update widgets in relation to alarm acknowledgment
		this.updateAcknowledgeWidgets();

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateWholeUI", "User interface updated");
	}

	/**
	 * To update primary listen number <code>EditText</code> widget.
	 * 
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 */
	private void updatePrimaryListenNumberEditText() {
		// Update primary listen number EditText with value
		this.primaryListenNumberEditText.setText(this.primaryListenNumber);

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updatePrimaryListenNumberEditText()", "PRIMARY listen number EditText set to: " + this.primaryListenNumber);
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updatePrimaryListenNumberEditText()", "PRIMARY listen number EditText updated");
	}

	/**
	 * To update secondary listen numbers <code>Spinner</code> with correct
	 * values.
	 * 
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 */
	private void updateSecondaryListenNumberSpinner() {
		// Check if there are secondary listen numbers and build up a proper spinner according to that information
		if (!this.secondaryListenNumbers.isEmpty()) {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, this.secondaryListenNumbers);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			this.secondaryListenNumberSpinner.setAdapter(adapter);
			// Logging
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateSecondaryListenNumberSpinner()", "Populate SECONDARY listen number spinner with values: " + this.secondaryListenNumbers);
		} else {
			// Only add item to list if it's empty
			if (this.emptySecondaryListenNumbers.isEmpty()) {
				this.emptySecondaryListenNumbers.add(getString(R.string.ENTER_PHONE_NUMBER_HINT));
			}
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, this.emptySecondaryListenNumbers);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			this.secondaryListenNumberSpinner.setAdapter(adapter);
			// Logging
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateSecondaryListenNumberSpinner()", "List with SECONDARY listen numbers is empty, populating spinner with an empty list");
		}

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateSecondaryListenNumberSpinner()", "SECONDARY listen numbers Spinner updated");
	}

	/**
	 * To update acknowledge number <code>EditText</code> widget.
	 * 
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 */
	private void updateAcknowledgeNumberEditText() {
		// Update acknowledge number EditText with value
		this.ackNumberEditText.setText(this.acknowledgeNumber);

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateAcknowledgeNumberEditText()", "Acknowledge number EditText set to: " + this.acknowledgeNumber);
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateAcknowledgeNumberEditText()", "Acknowledge number EditText updated");
	}

	/**
	 * To update selected tone <code>EditText</code> widget with value of
	 * <code>toneSpinner</code> position.
	 * 
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 * @see ax.ha.it.smsalarm.LogHandler#logCatTxt(LogPriorities, String,
	 *      String) logCatTxt(LogPriorities, String, String)
	 */
	private void updateSelectedToneEditText() {
		// Log tone spinner position
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateSelectedToneEditText()", "Tone Spinner position is: " + Integer.toString(this.toneSpinnerPos));

		// Set message tone to the selectedToneEditText, depending on which value spinner has. Also log this event
		if (this.toneSpinnerPos == 0) {
			this.selectedToneEditText.setText(this.noiseHandler.msgToneLookup(this, this.primaryMessageToneId));
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateSelectedToneEditText()", "Selected tone EditText updated");
		} else if (this.toneSpinnerPos == 1) {
			this.selectedToneEditText.setText(this.noiseHandler.msgToneLookup(this, this.secondaryMessageToneId));
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateSelectedToneEditText()", "Selected tone EditText updated");
		} else {
			// DO NOTHING EXCEPT LOG ERROR MESSAGE
			this.logger.logCatTxt(LogPriorities.ERROR, this.LOG_TAG + ":updateSelectedToneEditText()", "Invalid spinner position occurred. Current tone Spinner position is: \"" + Integer.toString(this.toneSpinnerPos) + "\"");
		}
	}

	/**
	 * To update rescue service <code>EditText</code> widget.
	 * 
	 * @see ax.ha.it.smsalarm#LogHandler.logCatTxt(int, String , String)
	 */
	private void updateRescueServiceEditText() {
		// Update rescue service EditText
		this.rescueServiceEditText.setText(this.rescueService);

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateRescueServiceEditText()", "Rescue service EditText set to: " + this.rescueService);
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateRescueServiceEditText()", "Rescue service EditText updated");
	}

	/**
	 * To update use OS sound settings <code>CheckBox</code> widget.
	 * 
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 */
	private void updateUseOsSoundSettingsCheckbox() {
		// Update use OS sound settings CheckBox
		if (this.useOsSoundSettings) {
			this.soundSettingCheckBox.setChecked(true);
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateUseOsSoundSettingsCheckbox()", "Use OS sound settings CheckBox \"Checked\"(" + this.useOsSoundSettings + ")");
		} else {
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateUseOsSoundSettingsCheckbox()", "Use OS sound settings CheckBox \"Unchecked\"(" + this.useOsSoundSettings + ")");
		}

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateUseOsSoundSettingsCheckbox()", "Use OS sound settings CheckBox updated");
	}

	/**
	 * To update play tone twice <code>CheckBox</code> widget.
	 * 
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 */
	private void updatePlayToneTwiceCheckBox() {
		// Update play tone twice CheckBox
		if (this.playToneTwice) {
			this.playToneTwiceSettingCheckBox.setChecked(true);
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updatePlayToneTwiceCheckBox()", "Play tone twice CheckBox \"Checked\"(" + this.playToneTwice + ")");
		} else {
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updatePlayToneTwiceCheckBox()", "Play tone twice CheckBox \"Unchecked\"(" + this.playToneTwice + ")");
		}

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updatePlayToneTwiceCheckBox()", "Play tone twice CheckBox updated");
	}

	/**
	 * To update enable Sms Alarm <code>CheckBox</code> widget.
	 * 
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 */
	private void updateEnableSmsAlarmCheckBox() {
		// Update enable Sms Alarm CheckBox(default checked=true)
		if (!this.enableSmsAlarm) {
			this.enableSmsAlarmCheckBox.setChecked(false);
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateEnableSmsAlarmCheckBox()", "Enable SmsAlarm CheckBox \"Unchecked\"(" + this.enableSmsAlarm + ")");
		} else {
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateEnableSmsAlarmCheckBox()", "Enable SmsAlarm CheckBox \"Checked\"(" + this.enableSmsAlarm + ")");
		}

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateEnableSmsAlarmCheckBox()", "Enable SmsAlarm CheckBox updated");
	}

	/**
	 * To update widgets with relations to alarm acknowledgement. These are
	 * widgets of type <code>CheckBox</code>, <code>Button</code> and
	 * <code>EditText</code>, they are enableAckCheckBox, ackNumberButton and
	 * ackNumberEditText.
	 * 
	 * @see ax.ha.it.smsalarm.LogHandler#logCat(LogPriorities, String, String)
	 *      logCat(LogPriorities, String, String)
	 */
	private void updateAcknowledgeWidgets() {
		/*
		 * Set checkbox for the enableAckCheckBox to true or false, also set
		 * some attributes to the ackNumberButton and the ackNumberField
		 */
		if (this.useAlarmAcknowledge) {
			this.enableAckCheckBox.setChecked(true);
			this.ackNumberButton.setEnabled(true);
			this.ackNumberEditText.setTextColor(Color.BLACK);
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateAcknowledgeWidgets()", "Enable acknowledge CheckBox \"Checked\"(" + this.useAlarmAcknowledge + "), acknowledge number Button is \"Enabled\" and acknowledge number EditText is \"Enabled\"");
		} else {
			this.ackNumberButton.setEnabled(false);
			this.ackNumberEditText.setTextColor(Color.GRAY);
			this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateAcknowledgeWidgets()", "Enable acknowledge CheckBox \"Unchecked\"(" + this.useAlarmAcknowledge + "), acknowledge number Button is \"Disabled\" and acknowledge number EditText is \"Disabled\"");
		}

		// Logging
		this.logger.logCat(LogPriorities.DEBUG, this.LOG_TAG + ":updateAcknowledgeWidgets()", "Acknowledge alarm UI widgets updated");
	}
}