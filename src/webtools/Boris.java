package webtools;

import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import util.Logger;
import util.UrlRetriever;

public class Boris {
	private static boolean DEBUG_MODE = false;
	private static int MAX_LEVELS = 1;
	private static boolean checkExternalItems = false;
	private static Logger logger;
	private static ArrayList<String> itemsCrawled =
		new ArrayList<String>();
	private static ArrayList<String> itemsArchived =
		new ArrayList<String>();
	private static ArrayList<ArrayList<String>> itemsMissing =
		new ArrayList<ArrayList<String>>();
	private static ArrayList<ArrayList<String>> externalItemsMissing =
		new ArrayList<ArrayList<String>>();
	private static ArrayList<String> badDestinations =
		new ArrayList<String>();
	private static ArrayList<ArrayList<String>> itemsNotProcessed =
			new ArrayList<ArrayList<String>>();

	public static void main(String[] args) {
		logger = new Logger("Boris.log");
		logger.clear();

		Scanner scanner = new Scanner(System.in);
		String response = "";

		if (args.length == 0) {
			args = new String[1];
			while (response.length() == 0) {
				System.out.println("Enter the full URL to the page to copy: ");
				response = scanner.nextLine().trim();
				if (!isValidURL(response)) response = "";
			}
			args[0] = response;
		}

		System.out.println("Validate external links (Y or N, default is N)?");
		response = scanner.nextLine().trim();
		if (response.equalsIgnoreCase("Y") || response.equalsIgnoreCase("YES"))
			checkExternalItems = true;
		
		System.out.println("How many crawl levels? ");
		try {
			int levels = scanner.nextInt();
			if (levels > 0) MAX_LEVELS = levels;
			else System.out.println("Defaulting to 1 level");
		}
		catch(Exception e) {
			System.out.println("Defaulting to 1 level");
		}

		crawl(args[0], args[0], 1, checkExternalItems);
		printAndLog("");

		printAndLog("Items Archived (" + itemsArchived.size() + "): ");
		printArrayList(itemsArchived);

		printAndLog("Items Missing (" + itemsMissing.size() + "): ");
		print2DArrayList(itemsMissing);

		if (checkExternalItems) {
			printAndLog("External Items Missing (" + externalItemsMissing.size() + "): ");
			print2DArrayList(externalItemsMissing);
		}

		if (badDestinations.size() > 0) {
			printAndLog("Bad Destinations (" + badDestinations.size() + "): ");
			printArrayList(badDestinations);			
		}

		printAndLog("Items Not Processed (" + itemsNotProcessed.size() + "): ");
		print2DArrayList(itemsNotProcessed);
	}

	private static void crawl(String urlAddress, String parentAddress,
							  int level, boolean checkExternalItems) {
		if (urlAddress == null || urlAddress.trim().length() == 0) {
			printAndLog("No URL provided for crawling");
			return;
		}

		if (itemsCrawled.contains(trimProtocol(urlAddress))) {
			printAndLog(urlAddress + " already processed");
			return;
		}
		else itemsCrawled.add(trimProtocol(urlAddress));

		// The UrlRetriever has three archive methods:
		// 1) archive(): Archives file using constructed filename into
		//    the working directory
		// 2) archive(true): Archives file (preserving original
		//    filename where possible) into subdirectory tree in the
		//    working directory that reflects the archived site
		// 3) archive("filename"): Archives file using the provided
		//    filename into the working directory
		//
		// Test URLs:
		// https://people.ict.usc.edu/~traum/
		// https://people.ict.usc.edu/~traum/Talks/ict-dm-tutorial2.pdf
		// http://www.ling.gu.se/ (MISSING)
		UrlRetriever ur = new UrlRetriever(urlAddress);
		if (ur.archive(true)) {
			logger.writeln(indent(ur.getMessage(), 2));
			itemsArchived.add(trimProtocol(urlAddress));
			printAndLog(urlAddress + " archived successfully");
		}

		if (ur.getPath() != null) {
			try {
				Document doc = Jsoup.parse(ur.getPath());
				doc.setBaseUri(getBaseUri(ur.getUrlAddress()));
				printAndLog("Base URI: " + doc.baseUri());

				// Extract the base URI path
				String baseUriPath = doc.baseUri();
				int uriIndex = baseUriPath.lastIndexOf("/") + 1;
				String lastSegment = baseUriPath.substring(uriIndex);
				if (lastSegment.indexOf(".") != -1)
					baseUriPath = baseUriPath.substring(0, uriIndex);
				printAndLog("Base URI Path: " + baseUriPath);

				ArrayList<String> itemsToCrawl = new ArrayList<String>();
				ArrayList<String> externalItems = new ArrayList<String>();
				Elements links = doc.select("a[href]");
				Elements media = doc.select("[src]");
				Elements imports = doc.select("link[href]");

				print("\nMedia: (%d)", media.size());
				for (Element src : media) {
					// Determine if the media needs to be crawled
					String actualSrc = src.attr("src").trim();
					String absSrc = src.attr("abs:src").trim();
					if (actualSrc.indexOf("http") == -1) {
						printAndLog("   " + actualSrc + 
									" is a relative address that should be crawled as " +
									absSrc);
						itemsToCrawl.add(absSrc);
					}
					else if (actualSrc.indexOf(baseUriPath) == 0 ||
							 actualSrc.indexOf(trimProtocol(baseUriPath)) != -1) {
						printAndLog("   " + actualSrc + 
								   	" is an absolute address that should be crawled");
						itemsToCrawl.add(actualSrc);
					}
					else if (actualSrc.indexOf("http") == 0) {
						if (checkExternalItems) {
							printAndLog("   " + actualSrc +
										" is an absolute address that should be verified");
							externalItems.add(actualSrc);
						}
					}
					else {
						printAndLog("   " + actualSrc + " will not be processed");
						ArrayList<String> item = new ArrayList<String>();
						item.add(urlAddress);
						item.add(actualSrc);
						itemsNotProcessed.add(item);
					}

					if (DEBUG_MODE)
						if (src.nameIs("img"))
							print(" * %s: <%s> %sx%s (%s)",
								  src.tagName(), src.attr("abs:src"), src.attr("width"),
								  src.attr("height"), trim(src.attr("alt"), 20));
						else
							print(" * %s: <%s>", src.tagName(), src.attr("abs:src"));
				}

				print("\nImports: (%d)", imports.size());
				for (Element link : imports) {
					// Determine if the link needs to be crawled
					String actualHref = link.attr("href").trim();
					String absHref = link.attr("abs:href").trim();
					if (actualHref.indexOf("http") == -1) {
						printAndLog("   " + actualHref + 
									" is a relative address that should be crawled as " +
									absHref);
						itemsToCrawl.add(absHref);
					}
					else if (actualHref.indexOf(baseUriPath) == 0 ||
							 actualHref.indexOf(trimProtocol(baseUriPath)) != -1) {
						printAndLog("   " + actualHref + 
								   	" is an absolute address that should be crawled");
						itemsToCrawl.add(actualHref);
					}
					else if (actualHref.indexOf("http") == 0) {
						if (checkExternalItems) {
							printAndLog("   " + actualHref +
										" is an absolute address that should be verified");
							externalItems.add(actualHref);
						}
					}
					else {
						printAndLog("   " + actualHref + " will not be processed");
						ArrayList<String> item = new ArrayList<String>();
						item.add(urlAddress);
						item.add(actualHref);
						itemsNotProcessed.add(item);
					}
					if (DEBUG_MODE)
						print(" * %s <%s> (%s)", link.tagName(), link.attr("abs:href"),
							  link.attr("rel"));
				}

				print("\nLinks: (%d)", links.size());
				for (Element link : links) {
					// Determine if the link needs to be crawled
					String actualHref = link.attr("href").trim();
					String absHref = link.attr("abs:href").trim();
					if (actualHref.indexOf("mailto:") == 0 ||
						actualHref.indexOf("ftp:") == 0 ||
						actualHref.indexOf("file:") == 0) {
						printAndLog("   " + actualHref + " cannot be crawled");
					}
					else if (actualHref.indexOf("http") == -1) {
						printAndLog("   " + actualHref + 
									" is a relative address that should be crawled as " +
									absHref);
						itemsToCrawl.add(absHref);
					}
					else if (actualHref.indexOf(baseUriPath) == 0 ||
							 actualHref.indexOf(trimProtocol(baseUriPath)) != -1) {
						printAndLog("   " + actualHref + 
								   	" is an absolute address that should be crawled");
						itemsToCrawl.add(actualHref);
					}
					else if (actualHref.indexOf("http") == 0) {
						if (checkExternalItems) {
							printAndLog("   " + actualHref +
										" is an absolute address that should be verified");
							externalItems.add(actualHref);
						}
					}
					else {
						printAndLog("   " + actualHref + " will not be processed");
						ArrayList<String> item = new ArrayList<String>();
						item.add(urlAddress);
						item.add(actualHref);
						itemsNotProcessed.add(item);
					}

					if (DEBUG_MODE)
						print(" * a: <%s>  (%s)", absHref, trim(link.text(), 35));
				}

				// Check the external items for existence
				for (String externalItem : externalItems) {
					if (itemsCrawled.contains(trimProtocol(externalItem))) {
						printAndLog(externalItem + " already processed");
					}
					else if (badDestinations.contains(extractDomain(externalItem))) {
						printAndLog(externalItem + " is on the destination blacklist");
						ArrayList<String> item = new ArrayList<String>();
						item.add(urlAddress);
						item.add(externalItem);
						externalItemsMissing.add(item);
					}
					else {
						itemsCrawled.add(trimProtocol(externalItem));
						UrlRetriever ur2 = new UrlRetriever(externalItem);
						if (ur2.getUrlStream()) {
							logger.writeln(indent(ur2.getMessage(), 2));
							printAndLog("   " + externalItem + " verified");
						}
						else {
							logger.writeln(indent(ur2.getMessage(), 2));
							printAndLog("   " + externalItem + " could not be verified");
							ArrayList<String> item = new ArrayList<String>();
							item.add(urlAddress);
							item.add(externalItem);
							externalItemsMissing.add(item);

							// Extract the domain if java.net.ConnectException
							// encountered and do not try connecting with that
							// domain again
							if (ur2.getMessage().indexOf("java.net.ConnectException") != -1 ||
								ur2.getMessage().indexOf("java.net.UnknownHostException") != -1 ||
								ur2.getMessage().indexOf("java.net.SocketException") != -1) {
								String badDomain = extractDomain(externalItem);
								printAndLog("Adding " + badDomain + " to destination blacklist");
								badDestinations.add(badDomain);
							}
						}
						ur2.reset();
					}
				}
				externalItems.clear();

				if (itemsToCrawl.size() > 0 && level + 1 > MAX_LEVELS) {
					printAndLog("Maximum number of levels exceeded");
				}
				else {
					if (itemsToCrawl.size() > 0) {
						printAndLog("Items To Crawl: ");
						printArrayList(itemsToCrawl);
					}
					else
						printAndLog("No links to follow in " + urlAddress);

					for (String itemToCrawl : itemsToCrawl) {
						crawl(itemToCrawl, urlAddress, level + 1, checkExternalItems);
					}
					itemsToCrawl.clear();
				}
			}
			catch(IOException e) {
				printAndLog(e.toString());
			}
		}
		else {
			logger.writeln(indent(ur.getMessage(), 2));
			printAndLog("Unable to archive " + urlAddress);
			ArrayList<String> item = new ArrayList<String>();
			item.add(parentAddress);
			item.add(urlAddress);
			itemsMissing.add(item);
		}		
	}

	private static boolean isValidURL(String urlString) {
		try {
			new URL(urlString);
		}
		catch(MalformedURLException e) {
			printAndLog(urlString + " is not a valid URL");
			return false;
		}

		return true;
	}

	private static String trimProtocol(String urlString) {
		if (urlString != null && urlString.indexOf("://") != -1) {
			return urlString.substring(urlString.indexOf("://") + 3);
		}
		else return urlString;
	}

	private static String extractDomain(String urlString) {
		if (urlString == null || urlString.trim().length() == 0)
			return urlString;

		urlString = trimProtocol(urlString);
		int endIndex = urlString.indexOf("/");
		if (endIndex == -1) return urlString;
		else return urlString.substring(0, endIndex);
	}

	private static String getBaseUri(String urlAddress) {
		if (urlAddress == null || urlAddress.trim().length() == 0)
			return urlAddress;

		// BUG: If the URL address ends with a / or if the URL address
		// contains an actual document name (e.g. filename with an extension),
		// it can be used as is. Otherwise, append a / to the URL address.
		// If this is not done, JSoup fails to determine the correct absolute
		// reference links of any relative reference links in the Document.
		String lastSegment = urlAddress
							 .substring(urlAddress.lastIndexOf("/") + 1);
		if (lastSegment.length() == 0 || lastSegment.indexOf(".") != -1)
			return urlAddress;
		else return urlAddress + "/";
	}

	private static String indent(String msg, int indentations) {
		if (msg == null || msg.trim().length() == 0)
			return msg;

		String indentationString = "\n";
		for (int i = 0; i < indentations; i++)
			indentationString += "\t";

		msg = msg.replace("\n", indentationString);
		while (msg.charAt(msg.length() - 1) == '\t')
			msg = msg.substring(0, msg.length() - 1);

		return msg;
	}

	private static void printAndLog(String msg) {
		System.out.println(msg);
		logger.writeln(msg);
	}

	private static void print(String msg, Object... args) {
		printAndLog(String.format(msg, args));
	}

	private static String trim(String s, int width) {
		if (s != null && s.length() > width)
			return s.substring(0, width-1) + ".";
		else
			return s;
	}

	public static void printArrayList(ArrayList<String> arrayList) {
		for (String string : arrayList)
			printAndLog(string);
		printAndLog("");
	}

	public static void print2DArrayList(ArrayList<ArrayList<String>> arrayList) {
		for (ArrayList<String> stringList : arrayList) {
			String string = "";
			for (int i = 0; i < stringList.size(); i++) {
				if (string.length() > 0) string += " => ";
				string += stringList.get(i);
			}
			printAndLog(string);
		}
		printAndLog("");
	}
}
