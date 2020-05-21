package main.java.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * PropertyHandler class for the project management utility.
 * This class contains a properties object that holds settings 
 * for different functions. NOTE THAT ALL VALUES ARE STORED IN
 * PLAINTEXT - PASSWORDS ARE NOT SECURE.
 * @author Navin
 *
 */

//TODO Add security
public class PropertyHandler {
	
	private static PropertyHandler instance = null;
	
	private Properties props;
	private String propsFilePath;
	private Logger logger =  Logger.getLogger(PropertyHandler.class.getName());
	
	private PropertyHandler() {
		// prevents multiple instantiation of singleton class
	}
	
	public void init(String programDirectory) {
		// setup file directories
		String propsDirName = programDirectory + '\\' + "props";
		this.propsFilePath = propsDirName + '\\' + "config.properties";
		this.props = new Properties();
		
		File propsDir = new File(propsDirName);
		File propsFile = new File(propsFilePath);
		try {
			if (propsFile.exists()) {
				// if the file exists, load it.
				FileInputStream inProps = new FileInputStream(propsFile);
				props.load(inProps);
				inProps.close();
			} else if(!propsDir.exists()){

				logger.info("Creating new properties directory");
				System.out.println("Creating new properties directory");
				propsDir.mkdirs();
			}
		} catch (IOException e) {
			// Add to logger
			logger.info("Failed to read properties file");
			System.out.println("Failed to read properties file: " + propsFilePath);
		} finally {
			// Add programDirectory passed to settings function
			props.setProperty("program_directory", programDirectory);
		}

		
	}
	
	public static synchronized PropertyHandler getInstance() {
		if (instance == null) {
			instance = new PropertyHandler();
		}
		return instance;
	}
	
	public String getProperty(String key) {
		return props.getProperty(key);
	}
	
	public String getProperty(String key, String defaultValue) {
		return props.getProperty(key,defaultValue);
	}
	
	public void setProperty(String key, String value) {
//		System.out.println("[PropertyHandler.setProperty()]     PROPERTY BEING SET: (Key)" + key+ "Value: " + value);
		props.setProperty(key, value);
		
		try {
			FileOutputStream outProps = new FileOutputStream(propsFilePath);
			props.store(outProps,"Properties for the Package Management System");
			outProps.close();
		} catch (FileNotFoundException e) {
			System.out.println("Could not find file: " + propsFilePath);
		} catch (IOException e) {
			System.out.println("Failed to write properties file: " + propsFilePath);
		}
		
	}

	/**
	 * Display all properties in a clean format
	 */
	public void printAllProperties() {
		System.out.println("In printAllProps");
		props.forEach((a,b) -> {
			System.out.printf(" Key: %s, Property: %s. %n", a , b);
		});
	}
	
/*
	public static void main(String[] args) {
		PropertyHandler ph = PropertyHandler.getInstance();
		ph.init("testFiles");
		ph.setProperty("test_first_name", "navin");
		ph.setProperty("test_last_name", "pathak");
		System.out.println(ph.getProperty("test_first_name"));
		System.out.println(ph.getProperty("test_last_name"));
		System.out.println(ph.getProperty("test_netID"));
		System.out.println(ph.getProperty("test_netID","np8"));
	}
*/
	
//	/*
//	 * Global
//	 */
//	public static String rootDir;
//	
//	/*
//	 * Printer
//	 */
//	public static String printServiceName;
//	
//	
//	/*
//	 * Emailer()
//	 */
//	// reminder emails
//	public static String sendReminderEmails;
//	
//	public static String password;
//	public static String adminEmailAddress;
//	public static String mailerName;
//	public static String replyToAddress;
//	public static String replyToName;
	
}
