package util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class FileUtils {
	private static final boolean DEBUGMODE = false;

	private FileUtils() {};

	public static String getPwd() {
		return System.getProperty("user.dir");
	}

	public static void displayFileAttributes(File f, String desc) {
		try {
			if (desc == null) desc = "";
			else desc = desc.trim();
			if (desc.length() > 0) desc += " ";

			System.out.println(desc + "Name: " + f.getName());
			System.out.println(desc + "Parent: " + f.getParent());
			System.out.println(desc + "Path: " + f.getPath());
			System.out.println(desc + "Absolute Path: " + f.getAbsolutePath());
			System.out.println(desc + "Canonical Path: " + f.getCanonicalPath());
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}

	public static ArrayList<String> getContents(String fileName,
												String delimiter) {
		ArrayList<String> contents = new ArrayList<String>();

		try {
			File f = new File(fileName);
			if (f.isDirectory()) {
				if (DEBUGMODE) displayFileAttributes(f, "Directory");
				contents.add(f.getAbsolutePath() + "(DIRECTORY)");

				// If the given fileName is a directory, call getContents
				// recursively on each object in the directory. Append the
				// results of the call to dirContents.
				String[] dirNames = f.list();
				for (int i = 0; i < dirNames.length; i++) {
					ArrayList<String> dirContents =
						getContents(f.getAbsolutePath() + delimiter + dirNames[i],
									delimiter);
					for (int j = 0; j < dirContents.size(); j++)
						contents.add(dirContents.get(j));
				}
			}
			else {
				if (DEBUGMODE) displayFileAttributes(f, "File");
				contents.add(f.getAbsolutePath());
			}
		}
		catch (Exception e) {
			System.out.println(e);
		}

		if (contents.size() > 1)
			Collections.sort(contents);

		return contents;
	}

	public static ArrayList<String> findFiles(String dirName,
											  String fileName,
											  String delimiter) {
		ArrayList<String> foundFileNames = new ArrayList<String>();
		ArrayList<String> contentList =
			FileUtils.getContents(dirName, delimiter);

		for (int i = 0; i < contentList.size(); i++) {
			String curFileName = contentList.get(i);
			if (curFileName.indexOf("(DIRECTORY)") == -1 &&
					curFileName.indexOf(fileName) != -1) {
				foundFileNames.add(curFileName);
			}
		}

		return foundFileNames;
	}

	public static ArrayList<String> findDirectories(String dirName,
													String fileName,
													String delimiter) {
		ArrayList<String> foundDirNames = new ArrayList<String>();
		ArrayList<String> contentList =
			FileUtils.getContents(dirName, delimiter);

		for (int i = 0; i < contentList.size(); i++) {
			String curFileName = contentList.get(i);
			int dirFlagIndex = curFileName.indexOf("(DIRECTORY)");
			if (dirFlagIndex != -1 &&
				curFileName.indexOf(fileName) != -1) {
				foundDirNames.add(curFileName.substring(0, dirFlagIndex).trim());
			}
		}

		return foundDirNames;
	}

	public static void printFile(ArrayList<String> foundFileNames,
			boolean showPackage,
			boolean showError,
			boolean firstFileOnly) {
		if (foundFileNames.size() > 0) {
			try {
				for (int i = 0; i < foundFileNames.size(); i++) {
					String fileName = foundFileNames.get(i);
					File f = new File(fileName);
					Scanner s = new Scanner(f);
					while (s.hasNextLine()) {
						String nextLine = s.nextLine();
						if (showPackage || nextLine.indexOf("package ") != 0)
							System.out.println(nextLine);
					}
					s.close();

					if (firstFileOnly) break;
				}
			}
			catch (IOException e) {
				if (showError) e.printStackTrace();
			}
		}
		else
			System.out.println("No file(s) provided");
	}

	public static void printFile(String fileName,
								 String delimiter,
								 boolean showPackage,
								 boolean showError,
								 boolean firstFileOnly) {
		ArrayList<String> foundFileNames = findFiles(getPwd(), fileName, delimiter);
		if (foundFileNames.size() > 0)
			printFile(foundFileNames, showPackage, showError, firstFileOnly);
		else
			System.out.println("Could not locate " + fileName + " anywhere in " +
							   getPwd() + " directory tree");
	}

	public static ArrayList<String> readFiles(ArrayList<String> foundFileNames,
											  boolean skipFirstLine,
											  boolean removeBlankLines,
											  boolean showError,
											  boolean firstFileOnly) {
		ArrayList<String> fileContents = new ArrayList<String>();
		int numberOfRemovedLines = 0;

		if (foundFileNames.size() > 0) {
			try {
				for (int i = 0; i < foundFileNames.size(); i++) {
					String fileName = foundFileNames.get(i);
					File f = new File(fileName);
					Scanner s = new Scanner(f);
					while (s.hasNextLine()) {
						String nextLine = s.nextLine();
						if (skipFirstLine)
							skipFirstLine = false;
						else if (removeBlankLines && nextLine.trim().length() == 0)
							numberOfRemovedLines++;
						else
							fileContents.add(nextLine);
					}
					s.close();
					if (firstFileOnly) break;
				}
			}
			catch (IOException e) {
				if (showError) e.printStackTrace();
			}

			if (numberOfRemovedLines > 0)
				System.out.println("Number of Removed Blank Lines: " +
								   numberOfRemovedLines);
		}
		else
			System.out.println("No file(s) provided");

		return fileContents;
	}

	public static ArrayList<String> readFiles(String fileName,
											  String delimiter,
											  boolean skipFirstLine,
											  boolean removeBlankLines,
											  boolean showError,
											  boolean firstFileOnly) {
		ArrayList<String> foundFileNames = findFiles(getPwd(), fileName, delimiter);
		if (foundFileNames.size() > 0)
			return readFiles(foundFileNames, skipFirstLine, removeBlankLines,
							 showError, firstFileOnly);
		else {
			System.out.println("Could not locate " + fileName + " anywhere in " +
							   getPwd() + " directory tree");
			return new ArrayList<String>();
		}
	}
}