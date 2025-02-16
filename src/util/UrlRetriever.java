package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.text.DateFormat;

import java.util.Calendar;
import java.util.Date;

public class UrlRetriever {
	// Instance Variables
	private final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:135.0) Gecko/20100101 Firefox/135.0";
	private final String DEFAULT_FILENAME = "index.html";
	private final String DELIMITER = SystemUtils.fileSystemsSeparator();
	private String urlAddress;
	URL url;
	private String urlHost;
	private InputStream is;
	private Path path;
	private String message;

	// Constructor

	public UrlRetriever(String urlAddress) {
		this.urlAddress = urlAddress;
		url = null;
		urlHost = null;
		is = null;
		path = null;
		message = null;
	}

	// Setters and Getters

	public void setUrlAddress(String urlAddress) {
		reset();
		this.urlAddress = urlAddress;
	}

	public String getUrlAddress() {
		return urlAddress;
	}

	public URL getUrl() {
		return url;
	}

	public String getUrlHost() {
		return urlHost;
	}

	public InputStream getInputStream() {
		return is;
	}

	public Path getPath() {
		return path;
	}

	public String getMessage() {
		return message;
	}

	// Class Methods

	public void reset() {
		reset(true);
	}
	public void reset(boolean clearMessage) {
		url = null;
		urlHost = null;
		try {
			is.close();
		}
		catch(Exception e) {}
		finally {
			is = null;
		}
		path = null;
		if (clearMessage)
			message = null;
	}

	private void addMessage(String message) {
		addMessage(message, true);
	}

	private void addMessage(String message, boolean prefixWithNewLine) {
		if (this.message == null)
			this.message =  message;
		else if (prefixWithNewLine)
			this.message += "\n" + message;
		else
			this.message += message;
	}

	public boolean getUrlStream() {
		if (is != null) {
			System.out.println("URL stream already obtained");
			addMessage("URL stream already obtained");
			return true;
		}
		else if (path != null) {
			System.out.println("Url stream already saved");
			addMessage("Url stream already saved");
			return true;
		}
		else if (urlAddress == null || !isValidURL(urlAddress.trim())) {
			System.out.println("Resetting UrlRetriever");
			addMessage("Resetting UrlRetriever");
			reset();
			return false;
		}

		try {
			urlAddress = urlAddress.trim();
			url  = new URL(urlAddress);

			System.out.println("Opening connection to " + urlAddress + "...");
			addMessage("Opening connection to " + urlAddress + "...");
			URLConnection urlC = url.openConnection();
			urlHost = url.getHost();

			// Set the URLConnection user agent
			urlC.setRequestProperty("User-Agent", USER_AGENT);
			//urlC.setRequestProperty("Accept", "*/*");
			//urlC.setRequestProperty("Accept-Language", "en-US");
			//urlC.setRequestProperty("Connection", "close");

			// Create an InputStream to the URL with timeout
			urlC.setConnectTimeout(30000);
			urlC.setReadTimeout(50000);
			is = url.openStream();
			System.out.println("Obtained URL stream to " + urlAddress);
			addMessage("Obtained URL stream to " + urlAddress);

			// Print info about resource
			Calendar calendar = Calendar.getInstance();
			System.out.print(calendar.getTime() + ": Retrieving (type: " +
							 urlC.getContentType());
			addMessage(calendar.getTime() + ": Retrieving (type: " +
					   urlC.getContentType());
			Date date = new Date(urlC.getLastModified());
			DateFormat dateFormat =
					DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
			String modDate = dateFormat.format(date);
			System.out.println(", modified on: " + modDate + ")...");
			addMessage(", modified on: " + modDate + ")...", false);
			System.out.flush();
		}
		catch(ConnectException e) {
			System.out.println("Failed connecting to URL stream before timeout");
			System.out.println("Resetting UrlRetriever");
			addMessage("Failed connecting to URL stream before timeout");
			addMessage("Resetting UrlRetriever");
			reset(false);
			System.err.println(e.toString());
			addMessage(e.toString());
			return false;
		}
		catch(IOException e) {
			System.out.println("Failed reading URL stream");
			System.out.println("Resetting UrlRetriever");
			addMessage("Failed reading URL stream");
			addMessage("Resetting UrlRetriever");
			reset(false);
			System.err.println(e.toString());
			addMessage(e.toString());
			return false;
		}

		// If URL stream is obtained, url, urlHost and is will have values
		return true;
	}

	private String getFlatFilename() {
		try {
			// Split the URL into its components and concatenate
			String filename = urlHost.replace('.', '_');
			String[] urlComponents = url.getFile().split("/");
			String lastComponent = null;
			for (String urlComponent : urlComponents) {
				if (urlComponent != null && urlComponent.trim().length() > 0) {
					filename += "-" + urlComponent;
					lastComponent = urlComponent;
				}
			}

			// If the last component does not include a period/dot, use
			// DEFAULT_FILENAME as the stream's filename
			if (!(lastComponent != null && lastComponent.indexOf(".") != -1))
				filename += "-" + DEFAULT_FILENAME;	

			return filename;
		}
		catch(Exception e) {
			System.out.println("Failed to determine flat filename");
			addMessage("Failed to determine flat filename");
			System.err.println(e.toString());
			addMessage(e.toString());
			return null;
		}
	}

	private String getHierarchicalFilename() {
		try {
			// Split the URL into its components and concatenate
			String filename = urlHost.replace('.', '_');
			String[] urlComponents = url.getFile().split("/");
			String lastComponent = null;
			for (String urlComponent : urlComponents) {
				if (urlComponent != null && urlComponent.trim().length() > 0) {
					filename += DELIMITER + urlComponent;
					lastComponent = urlComponent;
				}
			}

			// If the last component does not include a period/dot, use
			// DEFAULT_FILENAME as the stream's filename
			if (!(lastComponent != null && lastComponent.indexOf(".") != -1))
				filename += DELIMITER + DEFAULT_FILENAME;	

			// Prepend the current working directory
			filename = FileUtils.getPwd() + DELIMITER + filename;

			return filename;
		}
		catch(Exception e) {
			System.out.println("Failed to determine hierarchical filename");
			addMessage("Failed to determine hierarchical filename");
			System.err.println(e.toString());
			addMessage(e.toString());
			return null;
		}
	}

	public boolean isHierarchicalFilename(String filename) {
		if (filename != null && filename.indexOf(DELIMITER) != -1)
			return true;
		else return false;
	}

	public String getFilenamePath(String filename) {
		if (isHierarchicalFilename(filename))
			return filename.substring(0, filename.lastIndexOf(DELIMITER) + 1);
		else return null;
	}

	private boolean createFilenamePath(String filepath) {
		if (filepath != null) {
			File directory = new File(filepath);
			if (!directory.exists()) {
				// Use mkdir() if only make the lowest directory. Use
				// mkdirs() to make the entire directory path, including
				// any missing parent directories.
				System.out.println("Creating path " + filepath + "...");
				addMessage("Creating path " + filepath + "...");
				try {
					directory.mkdirs();
					return true;
				}
				catch(Exception e) {
					System.out.println("Failed creating path " + filepath);
					addMessage("Failed creating path " + filepath);
					return false;					
				}
			}
			else {
				System.out.println(filepath + " already exists");
				addMessage(filepath + " already exists");
				return true;
			}
		}
		else {
			System.out.println("Path must be provided");
			addMessage("Path must be provided");
			return false;
		}
	}

	public boolean saveToFile() {
		return saveToFile(null, false);
	}

	public boolean saveToFile(boolean hierarchical) {
		return saveToFile(null, hierarchical);
	}

	public boolean saveToFile(String filename) {
		return saveToFile(filename, false);
	}

	private boolean saveToFile(String filename, boolean hierarchical) {
		if (is == null) {
			System.out.println("No URL stream available");
			addMessage("No URL stream available");
			return false;
		}

		try {
			// Construct filename from the URL if it is null or empty
			if (filename == null || filename.trim().length() == 0) {
				if (hierarchical) {
					System.out.println("Hierarchical Filename: " +
									   getHierarchicalFilename());
					addMessage("Hierarchical Filename: " +
							   getHierarchicalFilename());
					filename = getHierarchicalFilename();
				}
				else {
					System.out.println("Flat Filename: " + getFlatFilename());
					addMessage("Flat Filename: " + getFlatFilename());
					filename = getFlatFilename();
				}
			}

			if (filename == null)
				throw new FileNotFoundException("Could not construct filename");
			System.out.println("Saving URL stream to " + filename + "...");
			addMessage("Saving URL stream to " + filename + "...");

			// If the filename is hierarchical (includes a directory path), the
			// specified directories must exist before creating the local file
			if (isHierarchicalFilename(filename)) {
				String filepath = getFilenamePath(filename);
				System.out.println("Required Path: " + filepath);
				addMessage("Required Path: " + filepath);
				if (!createFilenamePath(filepath))
					throw new FileNotFoundException();
			}

			// Open the local file
			File file = new File(filename);
			path = file.toPath();

			// Copy the contents of the InputStream into the local file,
			// replacing it if it already exists
			long count = Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);

			// Close the InputStream
			is.close();

			// Clear url and is since they can no longer be used
			url = null;
			is = null;

			System.out.println(count + " byte(s) copied to " + filename);
			addMessage(count + " byte(s) copied to " + filename);
		}
		catch(FileNotFoundException e) {
			System.out.println("Failed to save URL stream");
			System.out.println("Resetting UrlRetriever");
			addMessage("Failed to save URL stream");
			addMessage("Resetting UrlRetriever");
			reset(false);
			System.err.println(e.toString());
			addMessage(e.toString());
			return false;
		}
		catch(IOException e) {
			System.out.println("Failed to save URL stream");
			System.out.println("Resetting UrlRetriever");
			addMessage("Failed to save URL stream");
			addMessage("Resetting UrlRetriever");
			reset(false);
			System.err.println(e.toString());
			addMessage(e.toString());
			return false;
		}

		// If local file is written, path will have a value
		return true;
	}

	public boolean archive() {
		if (!getUrlStream() || !saveToFile()) return false;
		else return true;
	}

	public boolean archive(boolean hierarchical) {
		if (!getUrlStream() || !saveToFile(hierarchical)) return false;
		else return true;
	}

	public boolean archive(String filename) {
		if (!getUrlStream() || !saveToFile(filename)) return false;
		else return true;
	}

	private boolean isValidURL(String urlString) {
		try {
			new URL(urlString);
		}
		catch(MalformedURLException e) {
			System.out.println(urlString + " is not a valid URL");
			addMessage(urlString + " is not a valid URL");
			System.err.println(e.toString());
			addMessage(e.toString());
			return false;
		}

		return true;
	}
}
