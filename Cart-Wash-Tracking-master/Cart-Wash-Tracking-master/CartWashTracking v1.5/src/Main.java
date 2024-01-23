/*
 * CartWashTracking (CWT)
 * Version: v1.5
 * Author: Austin Johanningsmeier
 * Developed for ZF-TRW Fowlerville
 * Created: 7/2/2018
 * Last Updated: 7/11/2018
 * 
 * List of Error Messages and meanings can be found in readme file
 * 
 * This software is for the tracking of cart washing schedules inside the plant.
 * 
 * TODO:
 * > Implement GPIO once LEDs are in place
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class Main 
{
	final static GpioController gpio = GpioFactory.getInstance();
	public static String VERSION = "1.5";
	public static String GLOBCONNSTRING ="";
	private static String GLOBCONNUSER = "";
	private static String GLOBCONNPASS = "";
	
	public static void main(String [] args) throws Exception 
	{
		final GpioPinDigitalOutput pin11 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "DBConnected",PinState.HIGH);
		final GpioPinDigitalOutput pin13 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "DBError",PinState.HIGH);
		
		Connection conn;
		String dataEntered = "";
		
		//Initial printout on program start
		System.out.println("NOW RUNNING CWT v" + VERSION);
		logger("NOW RUNNING CWT v" + VERSION);
		
		//Load oracle Db config
		loadOracleConfig();
		
		//Load the oracle driver
		try
		{
			Class.forName ("oracle.jdbc.OracleDriver");
		}
		catch (ClassNotFoundException e)
		{
			System.out.println("Error Code 001 - Could not find Oracle DB driver");
			logger("Error Code 001 - Could not find Oracle DB driver");
			logger(e.getMessage());
			return;
		}
		
		System.out.println("Oracle DB driver found");
		logger("Oracle DB driver found");
		
		//Try to connect to the database
		try
		{
		conn = DriverManager.getConnection(getGlobConnStr(),getGlobConnUser(), getGlobConnPass());
		//machine_name::port/SID, user_ID, password
		}
		catch (SQLException e)
		{
			System.out.println("Error Code 002 - Connection to DB failed");
			logger("Error Code 002 - Connection to DB failed");
			logger(e.getMessage());
			//light up error pin for 5sec
			pin13.pulse(5000,true);
			return;
		}
		
		//light up good connection pin for 5 secs
		pin11.pulse(5000, true);
		System.out.println("Connected to DB");
		logger("Connected to DB");
		
		//Create Scanner object to get input from
		Scanner handScan = new Scanner(System.in);
		
		while (dataEntered != "Q")
		{
			dataEntered = handScan.nextLine();
			sendData(dataEntered, conn);
		}
		
		//Close the connection to the database
		try
		{
			conn.close();
		}
		catch (SQLException e)
		{
			System.out.println("Error Code 005 - Failed to close Database connection");
			logger("Error Code 005 - Failed to close Database connection");
			logger(e.getMessage());
		}
		
		//close Scanner
		handScan.close();
		return;
	}
	
	/*
	 * sendData(String,Connection)
	 * Updates the cart ID it is given in the DB with the current wash date
	 */
	public static void sendData(String data, Connection conn)
	{
		final GpioPinDigitalOutput pin15 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "DataGood",PinState.HIGH);
		final GpioPinDigitalOutput pin16 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "DataError",PinState.HIGH);
		
		//get the duration value of the cart, returns if duration wasn't retrieved
		int duration = getDuration(data,conn);
		if(duration == 0)
		{
			return;
		}
		
		//Get the current date
		String date = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
		//Creates a date format to use on the modified date
		SimpleDateFormat dateForm = new SimpleDateFormat ("yyyy-MM-dd");
		String nextWashSTR ="";
		Date nextWash;
		
		//Tries to parse the current date so it can be modified
		try
		{
			nextWash = dateForm.parse(date);
		}
		catch(Exception e)
		{
			System.out.println("Error Code 008 - Error Parsing Date Format");
			logger("Error Code 008 - Error Parsing Date Format");
			logger(e.getMessage());
			return;
		}
		//Creates a new calendar instance
		Calendar cal = Calendar.getInstance();
		//add the duration to the current date to get NEXT_WASH
		cal.add(Calendar.DAY_OF_MONTH, duration);
		nextWash = cal.getTime();
		//Format the nextWash variable so Oracle will accept it
		nextWashSTR = dateForm.format(nextWash);
		
		//SQL statement to update the Cart in the DB with the current date and the next wash date
		String sendString = "UPDATE CART_WASH_DATA SET LAST_WASHED = DATE \'" + date + "\', NEXT_WASH = DATE \'"+ nextWashSTR + "\' WHERE CART_ID = \'"+ data + "\'";
		
		Statement sendCMDStmnt = null;
		
		//Links the statement object to the connection object
		try
		{
			sendCMDStmnt = conn.createStatement();
		}
		catch (SQLException e)
		{
			System.out.println("Error Code 004 - Error creating statement to update cart info");
			logger("Error Code 004 - Error creating statement to update cart info");
			logger(e.getMessage());
			return;
		}
		
		//Executes the command to update the database
		try
		{
			sendCMDStmnt.executeUpdate(sendString);
		}
		catch (SQLException e)
		{
			//light up error pin for 5secs
			pin16.pulse(5000,true);
			System.out.println("Error Code 003 - Error sending data");
			System.out.println(e.getMessage());
			logger("Error Code 003 - Error sending data");
			logger(e.getMessage());
			return;
		}
		//light up good pin for 5secs
		pin15.pulse(5000, true);
		System.out.println("Cart Scanned Successfully!");
		logger("Cart Scanned Successfully!");
		return;
	}

	/*
	 * getDuration(String, Connection)
	 * This function returns an integer that is the DURATION field in the DB, of whatever cart ID is given
	 */
	public static int getDuration(String cartId, Connection conn)
	{
		//SQL statement to get the Duration value of the cart
		String getDurQue = "SELECT DURATION FROM CART_WASH_DATA WHERE CART_ID = \'"+ cartId + "\'";
		Statement getDurStatement = null;
		
		int duration = 0;
		ResultSet resSet = null;
		
		//try to create the statement
		try
		{
			getDurStatement = conn.createStatement();
		}
		catch(SQLException e)
		{
			System.out.println("Error Code 006 - Error creating getDuration statement");
			logger("Error Code 006 - Error creating getDuration statement");
			logger(e.getMessage());
			return 0;
		}
		
		//executes the statement, sets the data to the result set, pulls the duration out of the set.
		try
		{
			getDurStatement.execute(getDurQue);
			resSet = getDurStatement.getResultSet();
			resSet.next();
			duration = resSet.getInt(1);
		}
		catch(SQLException e)
		{
			System.out.println(e.getMessage());
			System.out.println("Error Code 007 - Error Executing getDuration Statement");
			logger("Error Code 007 - Error Executing getDuration Statement");
			logger(e.getMessage());
			return 0;
		}
		System.out.println("Duration = " + duration);
		logger("Duration = " + duration);
		return duration;
	}
	
	/*
	 * logger(String)
	 * writes the log file for a run
	 */
	public static void logger(String toLog)
	{
		//Set current date and time to strings
		String time = new SimpleDateFormat("HH:mm MM/dd/yyyy").format(new java.util.Date());
		String date = new SimpleDateFormat("MM_dd_yyyy").format(new java.util.Date());
		//sets log path/name and what data to log
		String logName = "Logs\\CWTLog-" + date + ".txt";
		String logString = System.lineSeparator() + time + " " + toLog;
		
		try
		{
			File f = new File (logName);
			//enters if the log file already exists
			if (f.exists())
			{
				BufferedWriter writer = new BufferedWriter(new FileWriter(logName, true));
				writer.append(logString);
				writer.close();
			}
			//enters if the log file does not exist
			else
			{
				BufferedWriter writer = new BufferedWriter(new FileWriter(logName));
				writer.write(logString);
				writer.close();
			}
		}
		catch(Exception i)
		{
			System.out.println("Error Code 009 - Error logging application data");
			System.out.println(i.getMessage());
			return;
		}
	}
	
	/*
	 * LoadOracleConfig()
	 * This function loads the configuration file for Oracle DB connection
	 */
	public static void loadOracleConfig()
	{
		final GpioPinDigitalOutput pin18 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "CONFIGGOOD",PinState.HIGH);
		final GpioPinDigitalOutput pin22 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "CONFIGBAD",PinState.HIGH);
		File inFile = new File("CWT_Config.txt");
		String inText ="";
		try
		{
			Scanner	scan = new Scanner(inFile);
			
			//loops until the end of the file is reached
			while(scan.hasNextLine())
			{
				inText = scan.nextLine();
				//enters conditional if the first char is not #
				if(inText.substring(0, 0) != "#")
				{
					//enters if its the line to set the conn string
					if(inText.contains("CONNECTION STRING"))
					{
						int startInd = inText.indexOf("\"") + 1;
						int endInd = inText.indexOf("\"", startInd);
						setGlobConnStr(inText.substring(startInd,endInd));
					}
					//enters if its the line to set the username
					else if(inText.contains("DataBase User"))
					{
						int startInd = inText.indexOf("\"") + 1;
						int endInd = inText.indexOf("\"", startInd);
						setGlobConnUser(inText.substring(startInd,endInd));
					}
					//enters if its the line to set the password
					else if(inText.contains("DataBase Pass"))
					{
						int startInd = inText.indexOf("\"") + 1;
						int endInd = inText.indexOf("\"", startInd);
						setGlobConnPass(inText.substring(startInd,endInd));
					}
				}
			}
			//closes the scanner
			scan.close();
		}
		catch(Exception e)
		{
			pin22.high();
			System.out.println("Error Code 010 - Error reading data from CWT_Config.txt");
			System.out.println(e.getMessage());
			logger("Error Code 010 - Error reading data from CWT_Config.txt");
			logger(e.getMessage());
		}
		pin18.high();
		return;
	}
	
	/*
	 * setGlobConnStr(String)
	 * Used to set the global connection string to the oracle DB
	 */
	public static void setGlobConnStr(String connString)
	{
		GLOBCONNSTRING = connString;
	}
	
	/*
	 * getGlobConnStr()
	 * Used to retrieve the global connection string for the oracle DB
	 */
	public static String getGlobConnStr()
	{
		return GLOBCONNSTRING;
	}
	
	/*
	 * setGlobConnStr(String)
	 * Used to set the global user string to the oracle DB
	 */
	public static void setGlobConnUser(String user)
	{
		GLOBCONNUSER = user;
	}
	
	/*
	 * getGlobConnStr()
	 * Used to retrieve the global user string for the oracle DB
	 */
	public static String getGlobConnUser()
	{
		return GLOBCONNUSER;
	}
	
	/*
	 * setGlobConnPass(String)
	 * sets the global password for accessing the oracle DB
	 */
	public static void setGlobConnPass(String pass)
	{
		GLOBCONNPASS = pass;
	}
	
	/*
	 * getGlobConnPass()
	 * returns the global password for accessing the oracle DB
	 */
	public static String getGlobConnPass()
	{
		return GLOBCONNPASS;
	}
	
	//This block of code will be used to implement LED status lights, using GPIO output
	public static void Gpio()
	{
		//create GPIO controller
		//final GpioController gpio = GpioFactory.getInstance();
		
		//Provision pin #1 as output and turn it on
		//final GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "MyLED",PinState.HIGH);
		
		//Set shutdown state for the pin
		//pin.setShutdownOptions(true, PinState.LOW);
		
		//turn off pin
		//pin.low();
		
		//Turn on pin
		//pin.high();
		
		//toggle state of the pin
		//pin.toggle();
		
		//turn pin on for one second then off
		//pin.pulse(5000, true);
		
		//shutdown all gpio activity
		//gpio.shutdown();
	}
}
