package util;

import java.nio.file.FileSystems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

//import com.sun.javafx.PlatformUtil; // Not available on OpenJDK

public class SystemUtils {
	private static final String OS = System.getProperty("os.name");
	private static final String VERSION = System.getProperty("os.version");
	private static final String ARCH = System.getProperty("os.arch");

	public static final boolean IS_UNIX = (OS.toLowerCase().indexOf("nix") != -1 ||
			OS.toLowerCase().indexOf("aix") != -1); // ||
	//PlatformUtil.isUnix());
	public static final boolean IS_LINUX = (OS.toLowerCase().indexOf("nux") != -1); // ||
	//PlatformUtil.isLinux());
	public static final boolean IS_SOLARIS = (OS.toLowerCase().indexOf("sunos") != -1); // ||
	//PlatformUtil.isSolaris());
	public static final boolean IS_WINDOWS = (OS.toLowerCase().indexOf("win") != -1); // ||
	//PlatformUtil.isWindows());
	public static final boolean IS_MAC = (OS.toLowerCase().indexOf("mac") != -1); // ||
	//PlatformUtil.isMac());
	//public static final boolean IS_IOS = PlatformUtil.isIOS();
	//public static final boolean IS_ANDROID = PlatformUtil.isAndroid();
	//public static final boolean IS_EMBEDDED = PlatformUtil.isEmbedded();

	private SystemUtils() {}

	public static void main(String[] args) {
		boolean verbose = true;
		System.out.println(System.getProperty("user.dir"));
		if (verbose)
			System.out.print("Operating System Info: "); getOSInfo(verbose);
			System.out.println("Operating System Family: " + getOSFamily());
			System.out.println("System Separator: " + systemSeparator());
			System.out.println("FileSystem Separator: " + fileSystemsSeparator());
			if (verbose)
				System.out.println("Environment Variables: "); getEnvironment(verbose);
	}

	public static String getOS() {
		return OS;
	}

	public static String getVersion() {
		return VERSION;
	}

	public static String getArch() {
		return ARCH;
	}

	public static String[] getOSInfo(boolean verbose) {
		String[] osInfo = {getOS(), getVersion(), getArch()};
		if (verbose) {
			System.out.print("[");
			for (int i = 0; i < osInfo.length; i++) {
				System.out.print(osInfo[i]);
				if (i < osInfo.length -1) System.out.print(",");
			}
			System.out.println("]");
		}
		return osInfo;
	}

	public static String[] getOSInfo() {
		return getOSInfo(false);
	}

	public static String getOSFamily() {
		if (IS_UNIX) return("Unix");
		else if (IS_LINUX) return("Linux");
		else if (IS_SOLARIS) return("Solaris");
		else if (IS_WINDOWS) return("Windows");
		else if (IS_MAC) return("Macintosh");
		//else if (IS_IOS) return("iOS");
		//else if (IS_ANDROID) return("Android");
		//else if (IS_EMBEDDED) return("Embedded");
		else return(OS);
	}

	public static String systemSeparator() {
		// The delimiter is usually / or \. Windows does not allow
		// space or . as the last character of a file or directory
		// name, or <>:"/\|?* ANYWHERE in the name. Unix only objects
		// to /.
		String pwd = System.getProperty("user.dir");
		if (pwd != null && pwd.trim().length() != 0) {
			pwd = pwd.trim();
			if (pwd.indexOf("/") != -1) return "/";
			else if (pwd.indexOf("\\") != -1) return "\\";
			else {
				for (int i = pwd.length() - 1; i > -1; i--) {
					char c = pwd.charAt(i);
					int index = "<>:\"/\\|?*".indexOf(c);
					if (index != -1) return String.valueOf(c);
				}
				return null;
			}
		}
		else return null;
	}

	public static String fileSystemsSeparator() {
		return FileSystems.getDefault().getSeparator();
	}

	public static String[][] getEnvironment(boolean verbose) {
		int numOfKeys = 0;
		String[][] envArray = null;
		Map<String,String> envMap = System.getenv();

		if (envMap != null && !envMap.isEmpty()) {
			// Create a case-insensitive sorted ArrayList of environment variables.
			// Use an inline Comparator to implement the case-insensitive sort.
			ArrayList<String> keyArray = new ArrayList<String>();
			keyArray.addAll(0, envMap.keySet());
			Collections.sort(keyArray,
							 new Comparator<String>() {
				public int compare(String s1, String s2) {
					return s1.toUpperCase().compareTo(s2.toUpperCase());
				}
			});

			// Create array of key-value pairs
			envArray = new String[keyArray.size()][2];
			Iterator<String> iterator = keyArray.iterator();
			while (iterator.hasNext()) {
				String keyName = iterator.next();
				String keyValue = envMap.get(keyName);
				if (verbose) System.out.println(keyName + " => " + keyValue);
				envArray[numOfKeys][0] = keyName;
				envArray[numOfKeys][1] = keyValue;
				numOfKeys++;
			}
		}

		return envArray;
	}

	public static String[][] getEnvironment() {
		return getEnvironment(false);
	}
}