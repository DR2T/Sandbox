package util;

import java.io.FileWriter;   // Import the FileWriter class
import java.io.IOException;  // Import the IOException class to handle errors

public class Logger {
	private String logFile;
	private FileWriter fileWriter;

	public Logger(String logFile) {
		this.logFile = logFile;
		this.fileWriter = null;
	}

	public String getLogFile() {
		return logFile;
	}

	public void open() {
		open(false);
	}

	public void open(boolean append) {
		try {
			if (fileWriter == null) {
				fileWriter = new FileWriter(logFile, append);
			}
			else System.out.println(logFile + " already open");
		}
		catch(IOException e) {
			System.out.println("Failed opening " + logFile);
			fileWriter = null;
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			if (fileWriter != null) {
				fileWriter.close();
				fileWriter = null;
			}
			else System.out.println(logFile + " is not open");
		}
		catch(IOException e) {
			System.out.println("Failed closing " + logFile);
			e.printStackTrace();
		}
	}

	public void clear() {
		try {
			open();
			close();
		}
		catch(Exception e) {
			System.out.println("Failed clearing " + logFile);
			e.printStackTrace();			
		}
	}

	public void writeln(String string) {
		write(string + "\n");
	}

	public void write(String string) {
		try {
			open(true);
			if (fileWriter != null) {
				fileWriter.write(string);
				close();
			}
			else System.out.println(logFile + " is not open");
		}
		catch(IOException e) {
			System.out.println("Failed writing " + string +
							   " to " + logFile);
		}
	}
}
