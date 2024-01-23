/*
 * MailBot
 * Version: v1.2
 * Author: Austin Johanningsmeier
 * Developed for ZF-TRW Fowlerville
 * Created: 7/11/2018
 * Last Updated: 7/11/2018
 * 
 */
 
 **Scan Station Raspberry Pi must have Java packages installed in same folder as MailBot program**
 >javax.mail
 >ojdbc8 (or higher)
 
  ** Error Messages **
 ------------------------------------------------------------------------------
 -001 "Could not find Oracle DB driver"
  >There was an error loading the oracle database thin client driver
  
 -002 "Connection to DB failed"
  >There was an error connecting to the database during the initial connection
  
 -003 "Error creating mail query statement"
  >An error occurred while creating mail list query statement
  
 -004 "Error Creating cart query statement"
  >An error occurred while creating cart data query statement
  
 -005 "Error executing query for cart data"
  >An error occurred while executing the SQL query to get the cart data
  
 -006 "Error executing mail list query"
  >An error occurred while executing the SQL query to get the cart data
  
 -007 "Error reading result set from mail list"
  >An error occurred while cart data result set was read, or while mail list result set was read
  
 -008 "Error Code 008 - Error sending e-mails"
  >An error occurred during the transmission of the report e-mails.
  
 -009 "Bad result from mailIt()"
  >The result from mailIt() was not a 0 or 1
  
 -010 "Error closing DB connection"
  >An error occurred while attempting to close connection to the database

 -011 "Error reading data from CWT_Config.txt"
   >An error occurred while CWT was trying to read the oracle connection data

 -012 "Error logging application data"
  >An error occurred when CWT tried to log data to the log file
  
  ** Change Log **
  ----------------------------------------------------------------------------------
  v1.1
   > Implemented Logging to a file

  v1.2
   >Implemented a config file for the user to change connection info
    -user can enter data in the MAILBOT_Config.txt file
    -User can only change what is in quotes