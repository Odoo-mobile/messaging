package com.odoo.util;

import android.text.Html;
import android.text.Spanned;

//TODO: Auto-generated Javadoc
/**
 * The Class HTMLHelper.
 */

public class HTMLHelper {
	/**
	 * Html to string.
	 * 
	 * @param html
	 *            the html
	 * @return the string
	 */
	public static String htmlToString(String html) {

		return Html.fromHtml(
				html.replaceAll("\\<.*?\\>", "").replaceAll("\n", ""))
				.toString();
	}

	/**
	 * String to html.
	 * 
	 * @param string
	 *            the string
	 * @return the spanned
	 */
	public static Spanned stringToHtml(String string) {
		return Html.fromHtml(string);
	}

}
