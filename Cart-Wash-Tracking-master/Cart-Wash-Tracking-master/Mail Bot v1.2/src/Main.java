/*
 * MailBot
 * Version: v1.2
 * Author: Austin Johanningsmeier
 * Developed for ZF-TRW Fowlerville
 * Created: 7/11/2018
 * Last Updated: 7/11/2018
 * 
 * List of Error Messages and meanings can be found in readme file
 */

import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.*;

public class Main {

	private static String GLOBCONNSTRING = "";
	private static String GLOBCONNUSER = "";
	private static String GLOBCONNPASS = "";
	
	public static void main(String[] args) 
	{
		String time = new SimpleDateFormat("HH:mm").format(new java.util.Date());
		Connection conn;
		int mailResult = 0;

		//Load connection data
		loadOracleConfig();
		
		//load the oracle client driver
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
		
		System.out.println("Loaded Oracle Driver");
		logger("Loaded Oracle Driver");
		
		//Connect to database
		try
		{
			conn = DriverManager.getConnection(getGlobConnStr(),getGlobConnUser(),getGlobConnPass());
			//machine_name::port/SID, user_ID, password
		}
		catch (SQLException e)
		{
			System.out.println(e.getMessage());
			System.out.println("Error Code 002 - Connection to DB failed");
			logger("Error Code 002 - Connection to DB failed");
			logger(e.getMessage());
			return;
		}
		
		System.out.println("Connected to Database");
		logger("Connected to DataBase");
		
		
		mailResult = mailIt(conn);
		//Close the connection
		try
		{
			conn.close();
		}
		catch (SQLException e)
		{
			System.out.println("Error Code 010 - Error closing DB connection");
			logger("Error Code 010 - Error closing DB connection");
			logger(e.getMessage());
		}
		//Messages for logging purposes
		if(mailResult == 0)
		{
			System.out.println("Failed to send MailBot Report");
			logger("Failed to send MailBot Report");
			return;
		}
		else if (mailResult == 1)
		{
			System.out.println(time + ": MailBot Report Successfuly Sent!!!");
			logger("MailBot Report Successfuly Sent!!!");
			return;
		}
		else if (mailResult == 2)
		{
			System.out.println("No carts are overdue, report wont be sent out");
			logger("No carts are overdue, report wont be sent out");
		}
		else
		{
			System.out.println("Error Code 009 - Bad result from mailIt()");
			logger("Error Code 009 - Bad result from mailIt()");
			return;
		}
		
	}
	
	/*
	 * mailIt(Connection)
	 * Returns 0,1, or 2 to determine if mail was sent or not
	 * Queries the database to see if any carts are overdue, if they are, those cart Id's are mailed to mail list
	 */
	public static int mailIt(Connection conn)
	{
		String date = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
		ResultSet cartQueResults = null;
		ResultSet mailQueResults = null;
		Statement mailQueCMDStmnt = null;
		Statement cartQueCMDStmnt = null;
		//Set string to be used as port
		String port = "25";
		//Set 'from' address for MailBot
		String fromAddress = "FriendlyMailBot@trw.com";
		//string to be used as host
		String host = "smtp-washington.infra.trw.com";
		//get the system properties
		Properties properties = System.getProperties();
		//Set Host Server
		properties.setProperty("mail.smtp.host", host);
		//Set host port
		properties.setProperty("mail.smtp.port", port);
		//get default session object
		Session session = Session.getDefaultInstance(properties);
		//String to hold entire message to be sent
		String messageCntnt = "CWT Mail Bot Report \n --------------------------- \n The following carts are overdue \n ";
		
		
		//SQL statement that returns all the id's of carts that are overdue for wash
		String cartQueCMD = "SELECT CART_ID from CART_WASH_DATA WHERE  DATE \'"+ date +" \' > NEXT_WASH";
		
		//SQL statement that returns all the email address on the mailing list table if they are enabled
		String mailListQueCMD = "SELECT EMAIL FROM MAIL_BOT_LIST WHERE MAIL_IT = 1";
		
		try
		{
			mailQueCMDStmnt = conn.createStatement();
		}
		catch (SQLException e)
		{
			System.out.println("Error Code 003 - Error creating mail query statement");
			logger("Error Code 003 - Error creating mail query statement");
			logger(e.getMessage());
			return 0;
		}
		
		//Link the statement to the connection
		try
		{
			cartQueCMDStmnt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_READ_ONLY);
		}
		catch (SQLException e)
		{
			System.out.println("Error Code 004 - Error Creating cart query statement");
			logger("Error Code 004 - Error Creating cart query statement");
			logger(e.getMessage());
			return 0;
		}
		
		//executes the query to get all cart id's that are overdue for wash, populates cartQueResults with the data
		try
		{
			cartQueResults = cartQueCMDStmnt.executeQuery(cartQueCMD);
		}
		catch (SQLException e)
		{
			System.out.println(e.getMessage());
			System.out.println("Error Code 005 - Error executing query for cart data");
			logger("Error Code 005 - Error executing query for cart data");
			logger(e.getMessage());
			return 0;
		}
		
		//Execute query to get mailing list from the DB
		try
		{
			mailQueResults = mailQueCMDStmnt.executeQuery(mailListQueCMD);
		}
		catch (SQLException e)
		{
			System.out.println("Error Code 006 - Error executing mail list query");
			logger("Error Code 006 - Error executing mail list query");
			logger(e.getMessage());
			return 0;
		}
		
		//This block creates the message if there is any data retrieved from cartData, if no data is given, no email is created.
		try
		{
			if(cartQueResults.next())
			{
				cartQueResults.beforeFirst();
				//Fill the message with the cart results and add a footer
				while (!cartQueResults.isLast())
				{
					cartQueResults.next();
					messageCntnt = messageCntnt + "> " + cartQueResults.getString(1) + "\n";
					System.out.println(messageCntnt);
				}
				messageCntnt = messageCntnt + "\n\n**************************** \n This has been a message from your friendly neighborhood MailBot \n"
						+ " DO NOT REPLY TO THIS EMAIL - there is no inbox to recieve it";
				
				//Create mime message
				MimeMessage message = new MimeMessage(session);
				message.setFrom(new InternetAddress(fromAddress));
				
				//Add all names from mail query results to 'to' portion of header
				while(mailQueResults.next())
				{
					message.addRecipients(Message.RecipientType.TO,  mailQueResults.getString(1));
					System.out.println(message.getAllRecipients());
				}
				
				message.setSubject("CWT MailBot report");
				message.setText(messageCntnt);
				
				System.out.println(message.getAllRecipients());
				Transport.send(message);
				System.out.println("Message sent");
				logger("Message sent");
			}
			else if (!cartQueResults.next())
			{
				return 2;
			}
		}
		catch (SQLException e)
		{
			System.out.println(e.getMessage());
			System.out.println("Error Code 007 - Error reading result set from mail list");
			logger("Error Code 007 - Error reading result set from mail list");
			logger(e.getMessage());
			return 0;
		}
		catch (MessagingException m)
		{
			System.out.println("Error Code 008 - Error sending emails");
			System.out.println(m.getMessage());
			logger("Error Code 008 - Error sending emails");
			logger(m.getMessage());
			return 0;
		}
		return 1;
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
		String logName = "Logs\\MailBotLog-" + date + ".txt";
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
			System.out.println("Error Code 012 - Error logging application data");
			System.out.println(i.getMessage());
			logger("Error Code 012 - Error logging application data");
			logger(i.getMessage());
			return;
		}
	}
	
	/*
	 * LoadOracleConfig()
	 * This function loads the configuration file for Oracle DB connection
	 */
	public static void loadOracleConfig()
	{
		File inFile = new File("MAILBOT_Config.txt");
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
			System.out.println("Error Code 011 - Error reading data from CWT_Config.txt");
			System.out.println(e.getMessage());
			logger("Error Code 011 - Error reading data from CWT_Config.txt");
			logger(e.getMessage());
		}
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
	
}
