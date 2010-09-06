package net.sourceforge.pmd.util.filter;

import java.util.regex.Pattern;

/**
 * A filter to which uses a regular expression to match upon Strings.
 */
public class RegexStringFilter implements Filter<String> {

	protected String regex;

	protected Pattern pattern;

	public RegexStringFilter() {
	}

	public RegexStringFilter(String regex) {
		this.regex = regex;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
		this.pattern = null;
	}

	public boolean filter(String obj) {
		if (pattern == null) {
			pattern = Pattern.compile(regex);
		}
		return pattern.matcher(obj).matches();
	}

	public String toString() {
		return "matches " + regex;
	}
}
