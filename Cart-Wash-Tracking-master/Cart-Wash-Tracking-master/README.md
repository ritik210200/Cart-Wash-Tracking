# Cart-Wash-Tracking
Cart cleanliness tracking system developed as internal tool for work

This is a system I developed for use during my internship over the summer of 2018, With permision, all sensitive data has been removed.

This system tracks how often carts used to move inventory are washed. 

This is a two part system to track and report. 
- CWT is run from a raspberry pi, equipt with a barcode scanner and wifi antenna. When a cart is washed, an ID barcode on the cart is
scanned. When the barcode is scanned, the ID is updated in an Oracle SQL database to reflect the last date washed. Each cart has a set
time limit in between washes. If a cart goes too long without being washed, an email is sent to the quality department, thats where
MailBot comes in.

- MailBot is a seperate app that also runs on the Raspberry pi. MailBot is scheduled by the Pi to run once every morning at 7am.
When the application runs, it checks the cart database to see if any carts are overdue for washing. If a cart is overdue, an email
is generated and sent to the quality department. Who the email is sent to is determined by a table containing email addresses.

Both applications are written using...
* Java
* Communication with an Oracle SQL Database using OJDBC 8
* (MailBot only) Emails are generated using JavaX.Mail
