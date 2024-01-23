 * CartWashTracking (CWT)
 * Version: v1.5
 * Author: Austin Johanningsmeier
 * Developed for ZF-TRW Fowlerville
 * Created: 7/2/2018
 * 
 * This software is for the tracking of cart washing schedules inside the plant.
 ------------------------------------------------------------------------------
 
 **Scan Station Raspberry Pi must have Java packages installed in same folder as CWT program**
 >ojdbc8 (or higher)
 
 **Oracle DB tables must conform to these rules
  > "CART_WASH_DATA"
    -"CART_ID"
    -"LAST_WASHED"
    -"DURATION"
    -"NECT_WASH"
  > "CART_WASH_RECORD"
    -"TIMESTAMP" Type: TIMESTAMP
    -"CART_ID"

 ** LED Pinout **
-------------------------------------------------------------------------------
 pin 11 (GPIO 0) - Database connected good
 pin 13 (GPIO 2) - Failed to connect to Database
 pin 15 (GPIO 3) - Data sent Susccess
 pin 16 (GPIO 4) - Data send failed
 pin 18 (GPIO 5) - Config load good
 pin 22 (GPIO 6) - Config load bad
 
 
 ** Error Messages **
 ------------------------------------------------------------------------------
 -001 "Could not find Oracle DB driver"
  >There was an error loading the oracle database thin client driver
  
 -002 "Connection to DB failed"
  >There was an error connecting to the database during the initial connection
  
 -003 "Error sending data"
  >an error occurred while executing SQL command to update data of last cart scanned in the database
  
 -004 "Error creating statement to update cart info"
  >an error occurred while creating the statement to update cart info in the database
  
 -005 "Failed to close Database connection"
  >An error occurred while closing connection to the database, connection may already be closed
  
 -006 "Error Creating cart query statement"
  >An error occurred while creating the statement to query the database for cart info
  
 -007 "Error Executing getDuration Statement"
  >An error occurred while creating the statement to get the DURATION value from the DB
  
 -008 "Error Parsing Date Format"
  >An error occurred during a format change of the current date value

 -009 "Error logging application data"
  >An error occurred when CWT tried to log data to the log file

 -010 "Error reading data from CWT_Config.txt"
  >An error occurred while CWT was trying to read the oracle connection data
  
   **ChangeLog**
   -------------------------------------------------------------------------------
   v1.1
   >Software can now update an id - issue with date formatting
   >Can now update multiple id's in a row - logic issue
   ***
   v1.2
   >added NEXT_WASH column to CART_WASH_TRACKING - needed for date comparisons
    -Added supplementing code to set column Value
   >Cleaned up code a bit - Visual
   ***
   v1.3
   >Implemented Logging
    -Log files will be generated while running
    -new log file every separate day ran
   v1.4
   >Implemented a config file for the user to change connection info
    -user can enter data in the CWT_Config.txt file
    -User can only change what is in quotes
   v1.5
   >Implemented the GPIO pins to light LEDs
    -6 pins setup to control 6 LEDS
    -3 red 3 green for good and bad results