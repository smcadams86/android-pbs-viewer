package com.smcad.pbs;

import java.io.IOException;
import java.text.SimpleDateFormat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ComicExtractor {
	
	static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
	static final String USER_AGENT = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6";
	
	public static String getComicImageUrlFromPageScrape(String urlToScrape) throws IOException {
		System.out.println("urlToScrape = " + urlToScrape);
		String imgSrc = null;
		Document doc = null;
		try {
			doc = Jsoup.connect(urlToScrape).userAgent(USER_AGENT).get();
			Element image_element = doc.select("p.feature_item a[href] img").first();
			imgSrc = image_element.attr("src");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return imgSrc;
	}

}
