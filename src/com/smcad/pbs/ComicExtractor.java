package com.smcad.pbs;
/**
 * Copyright 2012 Steven McAdams
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
