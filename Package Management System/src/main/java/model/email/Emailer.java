package model.email;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Date;
import java.util.logging.Logger;

import javax.imageio.stream.FileImageOutputStream;
import javax.mail.AuthenticationFailedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import controller.Controller;
import model.IModelToViewAdapter;
import util.FileIO;
import util.Package;
import util.Pair;
import util.Person;
import util.PropertyHandler;

/*
 * Class that handles sending notification and reminder emails to students through
 * SMTP to the Gmail mail server.
 */

//TODO Timeout
//TODO If notification sending fails, add to a list of emails to send

public class Emailer {
	
	private PropertyHandler propHandler;
	private String senderAddress ;
	private String senderPassword;
	private String senderAlias;

	private String host;
	private Session session;
	private Transport transport;
	
	//private HashMap<String,String> templates;
	
	private Logger logger;
	private IModelToViewAdapter viewAdaptor;
	
	public Emailer(IModelToViewAdapter viewAdaptor) {
		// get PropertyHandler and logger instance
		this.propHandler = PropertyHandler.getInstance();
//		System.out.println(propHandler);
		this.logger = Logger.getLogger(Emailer.class.getName());
		
		this.viewAdaptor = viewAdaptor;
		
		// Give view adaptor to the email template reader
		TemplateHandler.setViewAdaptor(viewAdaptor);
		
        this.host = "smtp.gmail.com";

	}
	
	public void start(ArrayList<Pair<Person, Package>> activeEntriesSortedByPerson) {

		propHandler.setProperty("email.email_address", "duncancollegemailroom@gmail.com");
		propHandler.setProperty("email.password", "#somosequipo2009#");
		propHandler.setProperty("email.alias", "Duncan Mail Room");

		this.senderAddress = propHandler.getProperty("email.email_address");
		this.senderPassword = propHandler.getProperty("email.password");
		this.senderAlias = propHandler.getProperty("email.alias");

//		this.senderAddress = "duncancollegemailroom@gmail.com";
//		this.senderPassword = "#somosequipo2009#";
//		this.senderAlias = "Duncan Mail Room";


//		System.out.printf("[Emailer.start()] ADDRESS: %s, PASSWORD: %s, ALIAS: %s.%n",
//				senderAddress, senderPassword, senderAlias );


		// TODO: What do you mena by loaded? Doesn't allow proper start up.
		// warn the user if the email properties were not loaded
		while(this.senderAddress == null || this.senderPassword == null || this.senderAlias == null) {
			logger.warning("Failed to load email properties.");
			viewAdaptor.displayMessage("Email information was not loaded from file.\n"
					+ "Please change email information in the next window.", 
					"Email Not Loaded");
			System.out.println("[Emailer.start()]  A FIELD IS NULL. REQUESTING EMAIL CHANGE ");
			changeEmail();
		}
		
		// attempt to connect to the mail server and alert user if it fails
		attemptConnection();
		
		if(checkReminder()) {
			sendAllReminders(activeEntriesSortedByPerson);
		}
	}
	
	/**
	 * Function changes email, password, and alias to passed values
	 * @param newAlias			New Alias for sender
	 * @param newAddress		New Email address
	 * @param newPassword		New Password to email address
	 */
	public void setEmailProperties(String newAlias, String newAddress, String newPassword) {

//		System.out.printf("[Emailer.setEmailProperties()] ADDRESS: %s , PASSWORD: %s , ALIAS: %s.", newAddress, newPassword, newAlias);
		propHandler.setProperty("email.email_address",newAddress);
		propHandler.setProperty("email.password",newPassword);
		propHandler.setProperty("email.alias",newAlias);
		
		this.senderAddress = newAddress;
		this.senderPassword = newPassword;
		this.senderAlias = newAlias;
		
		attemptConnection();
		
	}
	
	/**
	 * Attempts to connect to the Gmail server with stored credentials
	 * Sends error messages to the view if an authentication error or a 
	 * general messaging error occurs.
	 */
	private void attemptConnection() {
		boolean retry = true;
		while(retry) {
			try {
				connect();
				closeConnection();
				retry = false;
			} catch (AuthenticationFailedException e){ 
					System.err.format(e.toString());
					e.printStackTrace();

					viewAdaptor.displayMessage("Incorrect username or password.\n","Wrong username/password");

					if (!changeEmail()) {
						retry = false;
						viewAdaptor.displayMessage("Emails will not be sent until a connection is established.",
								"Notification");
					}
					
					
			} catch (MessagingException e) {
				logger.warning("Failed to connect to the mail server.");
				String[] options = {"Retry", "Cancel"};
				retry = viewAdaptor.getBooleanInput("Program failed to connect to the Gmail server.\n"
						+ "Please check your internet connection and try again.", 
						"Failed Connection",options);
				if(!retry) {
					viewAdaptor.displayMessage("Emails will not be sent until a connection is established.",
							"Notification");
				}
			}
		}
	}

	/**
	 * Function that sends all reminder emails
	 * @param allEntriesSortedByPerson	All active entries - MUST be sorted by person
	 * @return							Success of sending all reminders
	 */
	public boolean sendAllReminders(ArrayList<Pair<Person,Package>> allEntriesSortedByPerson) {
		
		//collect ArrayList of pairs of person,ArrayList<Package>
		ArrayList<Pair<Person,ArrayList<Package>>> remindList = collectPairs(allEntriesSortedByPerson);		
		try {
			connect();
			// iterate through ArrayList, sending emails if the person has packages
			for (Pair<Person,ArrayList<Package>> ppPair : remindList) {
				sendPackageReminder(ppPair.first,ppPair.second);
			}
			closeConnection();
			
			logger.info("Successfully sent reminder emails.");
			
			// add property with the current time as the last sent date
			propHandler.setProperty("email.last_reminder", Long.valueOf(new Date().getTime()).toString());
			return true;
		} catch(NoSuchProviderException e) {
			logger.warning(e.getMessage());
			return false;
		} catch(MessagingException e) {
			logger.warning(e.getMessage());
			return false;
		} catch (UnsupportedEncodingException e) {
			logger.severe(e.getMessage());
			return false;
		}
	}

	/*
	 * Sends a notification email to the recipient informing them that they 
	 * have a new package 
	 */
	public boolean sendPackageNotification(Person recipient, Package pkg) {
	
		// Find variable values
		Map<String,String> variables = new HashMap<String,String>();
		variables.put("COMMENT", pkg.getComment());
		variables.put("PKGTIME", pkg.getCheckInDate().toString());
		variables.put("PKGID",   String.valueOf(pkg.getPackageID()));  
		variables.put("FNAME",   recipient.getFirstName());  
		variables.put("LNAME",   recipient.getLastName());  
		variables.put("NETID",   recipient.getPersonID());  
		variables.put("NUMPKGS", "--");
		//variables.put("ALIAS",   "");  

		// Load email templates from template file
		Map<String,String> templates = TemplateHandler.getResolvedTemplates(variables);

		String body = templates.get("NOTIFICATION-BODY");
		String subject = templates.get("NOTIFICATION-SUBJECT");

		//TODO: Add some kind of mechanic to send packages in a staggered fashion. Not here
		// 1) Add person to a list Maybe a list that sends packa
		// send a package notification
		try {
			connect();
			sendEmail(recipient.getEmailAddress(), recipient.getFullName(), subject, body);
			closeConnection();
		} catch (UnsupportedEncodingException e) {
			logger.severe("UnsupportedEncodingException for Person (ID: " + recipient.getPersonID() +
					") and Package (ID: " + pkg.getPackageID() + ")");
			return false;
		} catch (MessagingException e) {
			logger.warning(e.getMessage());
			return false;
		}
		return true;
	}
	
	public String getSenderAddress() {
		return senderAddress;
	}

	public String getSenderAlias() {
		return senderAlias;
	}
	
	// connect to the mail server
	private void connect() 
			throws NoSuchProviderException, MessagingException {
		
		Properties props = System.getProperties();

		// TODO: Exception coming from here. Apparently password and sender address are done incorrectly
//		System.out.println("[Emailer.connect()] BEFORE transport.connect()");
//		System.out.println("[Emailer.connect()] HOST: " + host);
//		System.out.println("[Emailer.connect()] SENDER_ADDRESS: " + senderAddress);
//		System.out.println("[Emailer.connect()] SENDER_PASSWORD:" + senderPassword);

        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.user", senderAddress);
        props.put("mail.smtp.password", senderPassword);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        session = Session.getDefaultInstance(props);
		transport = session.getTransport("smtp");
		transport.connect(host, "duncancollegemailroom@gmail.com", "#somosequipo2009#");

		// THIS DOESN'T PRINT: System.out.println("[Emailer.connect()]    AFTER transport.connect() ");


	}
	
	// send an email through the mail server
	private void sendEmail(String recipientEmail, String recipientAlias, String subject,
			String body) throws UnsupportedEncodingException, MessagingException {
		
        MimeMessage message = new MimeMessage(session);
        
        message.addHeader("Content-Type", "text/html; charset=utf-8");
        
        message.setFrom(new InternetAddress(senderAddress, senderAlias));
        message.addRecipient(Message.RecipientType.TO, 
        		new InternetAddress(recipientEmail, recipientAlias));
        
        message.setSubject(subject);
        //message.setText(body);
        
        message.setContent(body, "text/html");
        
        message.saveChanges();
        transport.sendMessage(message, message.getAllRecipients());
	}

	// close the connection to the mail server
	private void closeConnection() throws MessagingException {
		transport.close();
	}
	
	/**
	 * Check if a reminder email should be sent. Reminder email will be sent if
	 * 		1) The last reminder was not sent within the same day
	 * 		2) The time is at least 07:00
	 * 		3) The day is a weekday
	 * @return					True if a reminder email should be sent
	 */
	private boolean checkReminder() {
		
		// Initialize calendar
		Calendar now = new GregorianCalendar();
		
		// Check if the hour is past 7 and the day is not a weekend
		if (now.get(Calendar.HOUR_OF_DAY) >= 7 && 
				now.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY &&
				now.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
			
			if (propHandler.getProperty("email.last_reminder") == null && now.get(Calendar.HOUR_OF_DAY) >= 7) {
				return true;
			}
			
			// Initialize last reminder calendar
			Calendar lastReminder = new GregorianCalendar();
			lastReminder.setTimeInMillis(Long.valueOf(propHandler.getProperty("email.last_reminder")));

			if (now.get(Calendar.DAY_OF_YEAR) != lastReminder.get(Calendar.DAY_OF_YEAR)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Sends a reminder email to the recipient reminding them of each package that
	 * is returned by recipient.getPackageList()
	 */
	private void sendPackageReminder(Person recipient, ArrayList<Package> packages) 
			throws UnsupportedEncodingException, MessagingException {
		
		// Find variable values
		Map<String,String> variables = new HashMap<String,String>();
		variables.put("COMMENT", "");
		variables.put("PKGTIME", "");
		variables.put("PKGID",   "");  
		variables.put("FNAME",   recipient.getFirstName());  
		variables.put("LNAME",   recipient.getLastName());  
		variables.put("NETID",   recipient.getPersonID());  
		variables.put("NUMPKGS", String.valueOf(packages.size()));

		// Load email templates from template file
		Map<String,String> templates = TemplateHandler.getResolvedTemplates(variables);

		String body = templates.get("REMINDER-BODY");
		String subject = templates.get("REMINDER-SUBJECT");
		
//		for (int i=0; i<packages.size(); i++) {
//			Package pkg = packages.get(i);
//			body += "Package " + (i+1) + " (ID: " + pkg.getPackageID() + ")" + ":\n";
//			body += "\tChecked in on " + pkg.getCheckInDate().toString() + "\n";
//			if(!pkg.getComment().isEmpty()) {
//				body += "\tComment: " + pkg.getComment() + "\n";
//			}
//			body += "\n";
//		}
//		body += "Jones Mail Room";
		sendEmail(recipient.getEmailAddress(), recipient.getFullName(), subject, body);
	}
	
	/**
	 * Function requests the view to get user input for new email information
	 * @return								True if the user input email information
	 */
	private boolean changeEmail() {
//		System.out.println("[Emailer.changeEmail()] Attempted to change email");
		String[] newEmail = viewAdaptor.changeEmail(senderAddress,senderPassword,senderAlias);
		if(newEmail != null) {
			setEmailProperties(newEmail[0],newEmail[1],newEmail[2]);
			return true;
		}
		return false;
	}
	
	/**
	 * Function that collects all of the pairs for the send all reminders function
	 * @param allEntriesSortedByPerson		DB entries sorted by person
	 * @return								ArrayList of pairs of person and owned packages
	 */
	private ArrayList<Pair<Person,ArrayList<Package>>> collectPairs(
			ArrayList<Pair<Person,Package>> allEntriesSortedByPerson) {
		
		// create a container for the result
		ArrayList<Pair<Person,ArrayList<Package>>> result = new ArrayList<Pair<Person,ArrayList<Package>>>();

		
		// initialize holders for the last person and a package list
		Person lastPerson = null;
		ArrayList<Package> pkgList = new ArrayList<Package>();
		for(Pair<Person,Package> entry : allEntriesSortedByPerson) {
			if (entry.first != lastPerson) {
				// if the person is new, add old person entry with packages
				if (lastPerson != null) {
					result.add(new Pair<Person,ArrayList<Package>>(lastPerson,pkgList));
				}
				
				// set the last person and give them a new package list
				lastPerson = entry.first;			// set the lastPerson
				pkgList = new ArrayList<Package>(); // new reference for new person
			}
			
			pkgList.add(entry.second);
		}
		
		// add the last person
		if (lastPerson != null && pkgList.size() > 0) {
			result.add(new Pair<Person,ArrayList<Package>>(lastPerson,pkgList));
		}
		
		return result;
	}
	
// Questionable comment section
	public static void main(String[] args) {
//		testSend();
	}


}
