package com.UARTLoopback;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.util.Log;
import android.graphics.Color;
import com.UARTLoopback.Globals;
import android.text.Html;
import java.math.*;

import com.UARTLoopback.R.drawable;


public class UARTLoopbackActivity extends Activity {

	/* thread to read the data */
	public handler_thread handlerThread;

	/* declare a FT311 UART interface variable */
	public FT311UARTInterface uartInterface;
	/* graphical objects */
	EditText readText;
	EditText writeText;
	Spinner baudSpinner;;
	Spinner stopSpinner;
	Spinner dataSpinner;
	Spinner paritySpinner;
	Spinner flowSpinner;

        Button writeButton, configButton, readButton , resetButton;

	/* local variables */
	byte[] writeBuffer;
	byte[] readBuffer;
	char[] readBufferToChar;
	int[] actualNumBytes;

        int readBufferSize;

	int numBytes;
	byte count;
	byte status;
	byte writeIndex = 0;
	byte readIndex = 0;
        
        boolean writing = false;
        boolean reading = false;

	int baudRate; /* baud rate */
	byte stopBit; /* 1:1stop bits, 2:2 stop bits */
	byte dataBit; /* 8:8bit, 7: 7bit */
	byte parity; /* 0: none, 1: odd, 2: even, 3: mark, 4: space */
	byte flowControl; /* 0:none, 1: flow control(CTS,RTS) */
	public Context global_context;
	public boolean bConfiged = false;
	public SharedPreferences sharePrefSettings;
	Drawable originalDrawable;
	public String act_string; 

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		sharePrefSettings = getSharedPreferences("UARTLBPref", 0);
		//cleanPreference();
		/* create editable text objects */
		readText = (EditText) findViewById(R.id.ReadValues);
                //readText.setInputType(0);
		//readText.setMovementMethod(ScrollingMovementMethod.getInstance());
		writeText = (EditText) findViewById(R.id.WriteValues);
		//writeText.setMovementMethod(ScrollingMovementMethod.getInstance());


		global_context = this;

		configButton = (Button) findViewById(R.id.configButton);
		writeButton = (Button) findViewById(R.id.WriteButton);
                readButton = (Button) findViewById( R.id.ReadButton );
                resetButton = (Button) findViewById(R.id.ResetButton);


		originalDrawable = configButton.getBackground();

                readBufferSize = 16384;

		/* allocate buffer */
		writeBuffer = new byte[64];
		readBuffer = new byte[readBufferSize];
		readBufferToChar = new char[readBufferSize]; 
		actualNumBytes = new int[1];

		/* setup the baud rate list */
		baudSpinner = (Spinner) findViewById(R.id.baudRateValue);
		ArrayAdapter<CharSequence> baudAdapter = ArrayAdapter.createFromResource(this, R.array.baud_rate,
				R.layout.my_spinner_textview);
		baudAdapter.setDropDownViewResource(R.layout.my_spinner_textview);		
		baudSpinner.setAdapter(baudAdapter);
		baudSpinner.setGravity(0x10);
		baudSpinner.setSelection(4);
		/* by default it is 9600 */
		baudRate = 9600;

		/* stop bits */
		stopSpinner = (Spinner) findViewById(R.id.stopBitValue);
		ArrayAdapter<CharSequence> stopAdapter = ArrayAdapter.createFromResource(this, R.array.stop_bits,
						R.layout.my_spinner_textview);
		stopAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
		stopSpinner.setAdapter(stopAdapter);
		stopSpinner.setGravity(0x01);
		/* default is stop bit 1 */
		stopBit = 1;

		/* daat bits */
		dataSpinner = (Spinner) findViewById(R.id.dataBitValue);
		ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(this, R.array.data_bits,
						R.layout.my_spinner_textview);
		dataAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
		dataSpinner.setAdapter(dataAdapter);
		dataSpinner.setGravity(0x11);
		dataSpinner.setSelection(1);
		/* default data bit is 8 bit */
		dataBit = 8;

		/* parity */
		paritySpinner = (Spinner) findViewById(R.id.parityValue);
		ArrayAdapter<CharSequence> parityAdapter = ArrayAdapter.createFromResource(this, R.array.parity,
						R.layout.my_spinner_textview);
		parityAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
		paritySpinner.setAdapter(parityAdapter);
		paritySpinner.setGravity(0x11);
		/* default is none */
		parity = 0;

		/* flow control */
		flowSpinner = (Spinner) findViewById(R.id.flowControlValue);
		ArrayAdapter<CharSequence> flowAdapter = ArrayAdapter.createFromResource(this, R.array.flow_control,
						R.layout.my_spinner_textview);
		flowAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
		flowSpinner.setAdapter(flowAdapter);
		flowSpinner.setGravity(0x11);
		/* default flow control is is none */
		flowControl = 0;


		/* set the adapter listeners for baud */
		baudSpinner.setOnItemSelectedListener(new MyOnBaudSelectedListener());
		/* set the adapter listeners for stop bits */
		stopSpinner.setOnItemSelectedListener(new MyOnStopSelectedListener());
		/* set the adapter listeners for data bits */
		dataSpinner.setOnItemSelectedListener(new MyOnDataSelectedListener());
		/* set the adapter listeners for parity */
		paritySpinner.setOnItemSelectedListener(new MyOnParitySelectedListener());
		/* set the adapter listeners for flow control */
		flowSpinner.setOnItemSelectedListener(new MyOnFlowSelectedListener());		
		
		act_string = getIntent().getAction();
		if( -1 != act_string.indexOf("android.intent.action.MAIN")){
			restorePreference();
		}			
		else if( -1 != act_string.indexOf("android.hardware.usb.action.USB_ACCESSORY_ATTACHED")){
			cleanPreference();
		}		
		
		configButton.setOnClickListener(new View.OnClickListener() {

			// @Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// configButton.setBackgroundResource(drawable.start);
				
				if( bConfiged == false ){
					bConfiged = true;
					uartInterface.SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
					savePreference();
				}

				if(true == bConfiged){
					configButton.setBackgroundColor(0xff888888); // color GRAY:0xff888888
					configButton.setText("Configured");
				}
			}

		});


                resetButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                            public void onClick(View v) {
                            bConfiged = false;
                            // configButton.setBackground(originalDrawable);
                            configButton.setBackgroundResource(R.drawable.button_pattern);
                            uartInterface.SetConfig(9600, (byte)8, (byte)1, (byte)0, (byte)0);
                            savePreference();
                            // uartInterface = new FT311UARTInterface(this, sharePrefSettings);
                        }
                });



		/* handle write click */
		writeButton.setOnClickListener(new View.OnClickListener() {

			// @Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// writeButton.setBackgroundResource(drawable.start);
                                if( writing )  { 
                                    writeButton.setBackgroundResource( R.drawable.button_pattern);
                                    writing = false;
                                    writeButton.setText( R.string.write_test );
                                    Log.d( com.UARTLoopback.Globals.LOGSTR ,"Trying to stop write test" );
                                    // This method needs to be rewritten to actually
                                    // timeout and end the runnable
                                    uartInterface.EndWriteTest();
                                    writeText.setText( "Bytes Written: " + uartInterface.getWriteTestNumBytes() );
                                } else {
                                        writing = true;
                                        // First change the color of the Button
                                        writeButton.setBackgroundResource( R.drawable.button_pattern_running);
                                        writeButton.setText( R.string.running_test );
                                        writeText.setText("");
                                        status = uartInterface.WriteTest(10);
                                }
                        }

                    });

                readButton.setOnClickListener( new View.OnClickListener() { 
                            @Override
                            public void onClick( View v ) { 
                                if( reading ) {
                                    readButton.setBackgroundResource( R.drawable.button_pattern);
                                    reading = false;
                                    readButton.setText( R.string.read_test );
                                    // handlerThread.update_display = false;
                                    handlerThread.disableThread();
                                    int tmpbyte_count = handlerThread.byte_count;
                                    handlerThread.byte_count = 0;
                                    String color = ( handlerThread.getFailureCount() > 0 ? "RED" : "WHITE" );
                                    readText.setText( Html.fromHtml("Bytes Read: " + tmpbyte_count + 
                                                                    " Failures: <font COLOR=\"" + color + "\"><b>" + 
                                                                    handlerThread.getFailureCount() + "</b></font>" ) );
                                    handlerThread.setFailureCount(0);

                                } else {
                                    reading = true;
                                    readButton.setBackgroundResource( R.drawable.button_pattern_running);
                                    readButton.setText( R.string.running_test );
                                    readText.setText("");
                                    // handlerThread.update_display = true;
                                    handlerThread.enableThread();
                                }
                            }
                    });

		uartInterface = new FT311UARTInterface(this, sharePrefSettings);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		handlerThread = new handler_thread(handler, 256 );
                handlerThread.setReadBufferSize( readBufferSize ) ;
		handlerThread.start();

	}
	
	protected void cleanPreference(){
		SharedPreferences.Editor editor = sharePrefSettings.edit();
		editor.remove("configed");
		editor.remove("baudRate");
		editor.remove("stopBit");
		editor.remove("dataBit");
		editor.remove("parity");
		editor.remove("flowControl");
		editor.commit();
	}

	protected void savePreference() {
		if(true == bConfiged){
			sharePrefSettings.edit().putString("configed", "TRUE").commit();
			sharePrefSettings.edit().putInt("baudRate", baudRate).commit();
			sharePrefSettings.edit().putInt("stopBit", stopBit).commit();
			sharePrefSettings.edit().putInt("dataBit", dataBit).commit();
			sharePrefSettings.edit().putInt("parity", parity).commit();			
			sharePrefSettings.edit().putInt("flowControl", flowControl).commit();			
		}
		else{
			sharePrefSettings.edit().putString("configed", "FALSE").commit();
		}
	}
	
	protected void restorePreference() {
		String key_name = sharePrefSettings.getString("configed", "");
		if(true == key_name.contains("TRUE")){
			bConfiged = true;
		}
		else{
			bConfiged = false;
                }
		
		baudRate = sharePrefSettings.getInt("baudRate", 9600);
		stopBit = (byte)sharePrefSettings.getInt("stopBit", 1);
		dataBit = (byte)sharePrefSettings.getInt("dataBit", 8);
		parity = (byte)sharePrefSettings.getInt("parity", 0);
		flowControl = (byte)sharePrefSettings.getInt("flowControl", 0);

		if(true == bConfiged){			
			configButton.setText("Configured");
			configButton.setBackgroundColor(0xff888888); // color GRAY:0xff888888
			switch(baudRate)
			{
			case 300:baudSpinner.setSelection(0);break;
			case 600:baudSpinner.setSelection(1);break;
			case 1200:baudSpinner.setSelection(2);break;
			case 4800:baudSpinner.setSelection(3);break;
			case 9600:baudSpinner.setSelection(4);break;
			case 19200:baudSpinner.setSelection(5);break;
			case 38400:baudSpinner.setSelection(6);break;
			case 57600:baudSpinner.setSelection(7);break;
			case 115200:baudSpinner.setSelection(8);break;
			case 230400:baudSpinner.setSelection(9);break;
			case 460800:baudSpinner.setSelection(10);break;
			case 921600:baudSpinner.setSelection(11);break;
			default:baudSpinner.setSelection(4);break;
			}
			
			switch(stopBit)
			{
			case 1:stopSpinner.setSelection(0);break;
			case 2:stopSpinner.setSelection(1);break;
			default:stopSpinner.setSelection(0);break;
			}

			switch(dataBit)
			{
			case 7:dataSpinner.setSelection(0);break;
			case 8:dataSpinner.setSelection(1);break;
			default:dataSpinner.setSelection(1);break;
			}

			switch(parity)
			{
			case 0:paritySpinner.setSelection(0);break;
			case 1:paritySpinner.setSelection(1);break;
			case 2:paritySpinner.setSelection(2);break;
			case 3:paritySpinner.setSelection(3);break;
			case 4:paritySpinner.setSelection(4);break;
			default:paritySpinner.setSelection(0);break;
			}
			
			switch(flowControl)
			{
			case 0:flowSpinner.setSelection(0);break;
			case 1:flowSpinner.setSelection(1);break;
			default:flowSpinner.setSelection(0);break;
			}
		}
		else{
			baudSpinner.setSelection(4);
			stopSpinner.setSelection(0);
			dataSpinner.setSelection(1);
			paritySpinner.setSelection(0);
			flowSpinner.setSelection(0);
			configButton.setBackgroundDrawable(originalDrawable);			
		}
	}
	
	
	public class MyOnBaudSelectedListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {

			baudRate = Integer.parseInt(parent.getItemAtPosition(pos).toString());
		}

		public void onNothingSelected(AdapterView<?> parent) { // Do nothing. }}
		}
	}

	public class MyOnStopSelectedListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {

			stopBit = (byte) Integer.parseInt(parent.getItemAtPosition(pos).toString());
		}

		public void onNothingSelected(AdapterView<?> parent) { // Do nothing. }}
		}
	}

	public class MyOnDataSelectedListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {

			dataBit = (byte) Integer.parseInt(parent.getItemAtPosition(pos).toString());
		}

		public void onNothingSelected(AdapterView<?> parent) { // Do nothing. }}
		}
	}

	public class MyOnParitySelectedListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {

			String parityString = new String(parent.getItemAtPosition(pos).toString());
			if (parityString.compareTo("None") == 0) {
				parity = 0;
			}

			if (parityString.compareTo("Odd") == 0) {
				parity = 1;
			}

			if (parityString.compareTo("Even") == 0) {
				parity = 2;
			}

			if (parityString.compareTo("Mark") == 0) {
				parity = 3;
			}

			if (parityString.compareTo("Space") == 0) {
				parity = 4;
			}
		}

		public void onNothingSelected(AdapterView<?> parent) { // Do nothing. }}
		}
	}

	public class MyOnFlowSelectedListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {

			String flowString = new String(parent.getItemAtPosition(pos).toString());
			if (flowString.compareTo("None") == 0) {
				flowControl = 0;
			}

			if (flowString.compareTo("CTS/RTS") == 0) {
				flowControl = 1;
			}
		}

		public void onNothingSelected(AdapterView<?> parent) { // Do nothing. }}
		}
	}

	//@Override
	public void onHomePressed() {
		onBackPressed();
	}	

	public void onBackPressed() {
	    super.onBackPressed();
	}	
	
	@Override
	protected void onResume() {
		// Ideally should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onResume();		
		if( 2 == uartInterface.ResumeAccessory() )
		{
			cleanPreference();
			restorePreference();
		}
	}

	@Override
	protected void onPause() {
		// Ideally should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onPause();
	}

	@Override
	protected void onStop() {
		// Ideally should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		uartInterface.DestroyAccessory(bConfiged);
		super.onDestroy();
	}

        public class TmpHandler extends Handler {
                public void errorMessage( String errmsg ) {
                }
        }


	final TmpHandler handler = new TmpHandler() {
                int reset_display_count = 0;
                int failure_count = 0;
                int prev_value = -1;

                    
                public int getFailureCount() {
                    return failure_count;
                }

                public void setFailureCount(int val ) { 
                        failure_count = val;
                }

		@Override
		public void handleMessage(Message msg) {
                        int cur_value;
                        Bundle bundle = msg.getData();

                        if( "error".equals( bundle.getString("MessageType") ) ) {
                            String errmsg = bundle.getString("Message");
                            Log.d( com.UARTLoopback.Globals.LOGSTR ,"Detected error message" + errmsg );

                            readText.setText("");
                            readText.append( Html.fromHtml("<font COLOR=\"RED\"><b>" + errmsg + "</b></font>" ));
                            reset_display_count = 0;
                        } else {
                            for(int i=0; i<actualNumBytes[0]; i++)
                                {
                                    readBufferToChar[i] = (char)readBuffer[i];
                                }
                            
                            readText.append(String.copyValueOf(readBufferToChar, 0, actualNumBytes[0]));
                            reset_display_count += actualNumBytes[0];
                            if( reset_display_count > 4096 ) {
                                Log.d( com.UARTLoopback.Globals.LOGSTR ,"Resetting readText");
                                readText.setText("");
                                reset_display_count = 0;
                            }
                        }
		}
	};

	/* usb input data handler */
	private class handler_thread extends Thread {
		public TmpHandler mHandler;
                boolean update_display = false;
                int byte_count = 0;
                int failure_count = 0;
                int reset_display_count = 0;
                public int readBufferSize = 4096;
                int prev_value = -1;
                int reset_value = 256;

		/* constructor */
		handler_thread(TmpHandler h, int rvalue ) {
			mHandler = h;
		}
                public void setReadBufferSize(int size) {
                    readBufferSize = size;
                }
                public int getReadBufferSize() { 
                    return readBufferSize;
                }

                public void enableThread() {
                    update_display = true;
                }
                public void disableThread() { 
                    update_display = false;
                }
                public int getFailureCount() {
                    return failure_count;
                }
                public void setFailureCount(int val) {
                    failure_count = val;
                }

		public void run() {
			Message msg;

			while (true) {
				
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
				}

                                if( update_display ) { 
                                    status = uartInterface.ReadData(readBufferSize, readBuffer,actualNumBytes);

                                    Log.d( com.UARTLoopback.Globals.LOGSTR ,"actualNumBytes:"+actualNumBytes[0]);

                                     int cur_value;
                    
                                     for(int i=0; i<actualNumBytes[0]; i++)
                                     {
                                         cur_value = ((int)readBuffer[i] & 0xFF );
                                         if( prev_value == -1 ) { // no prev value
                                             prev_value = ((int)readBuffer[i] & 0xFF );
                                         } else {
                                             if( ((prev_value + 1) % reset_value) != cur_value ) {
                                                 String errmsg = "Prev: " + prev_value + " Expected: '" +  ((prev_value + 1) % reset_value) + "' Got: '" + cur_value + "'";
                                                 Log.e( com.UARTLoopback.Globals.LOGSTR, errmsg );
                                                 // mHandler.errorMessage( errmsg );
                                                 msg = mHandler.obtainMessage();
                                                 Bundle bundle = new Bundle();
                                                 bundle.putString("MessageType", "error" );
                                                 bundle.putString("Message", errmsg );
                                                 msg.setData( bundle );
                                                 mHandler.sendMessage( msg );
                                                 failure_count ++;
                                             }
                                             prev_value = cur_value;
                                         }
                                     }
                                        
                                    if (status == 0x00 && actualNumBytes[0] > 0) {
                                        byte_count += actualNumBytes[0];

                                        msg = mHandler.obtainMessage();
                                            
                                        // 1. first cut, try to reset the buffer occasionally
                                        if( reset_display_count > 4096 || reset_display_count == 0 ) { 
                                            mHandler.sendMessage(msg);
                                        }
                                        reset_display_count += actualNumBytes[0] % 4096;

                                    }
                                }
			}
		}
	}
}
