/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

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
