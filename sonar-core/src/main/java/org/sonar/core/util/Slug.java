/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class Slug {
  private static final String DASH = "-";
  private static final Pattern NON_ASCII_CHARS = Pattern.compile("[^\\p{ASCII}]");
  private static final Pattern NON_WORD_CHARS = Pattern.compile("[^\\w+]");
  private static final Pattern WHITESPACES_CHARS = Pattern.compile("\\s+");
  private static final Pattern DASHES_IN_ROWS = Pattern.compile("[-]+");

  private String in;

  private Slug(String in) {
    this.in = in;
  }

  public static String slugify(String s) {
    Slug slug = new Slug(s);
    return slug
      .normalize()
      .removeNonAsciiChars()
      .dashifyNonWordChars()
      .dashifyWhitespaceChars()
      .collapseDashes()
      .removeHeadingDash()
      .removeTrailingDash()
      .toLowerCase();
  }

  private Slug normalize() {
    this.in = Normalizer.normalize(in, Normalizer.Form.NFD);
    return this;
  }

  private Slug removeNonAsciiChars() {
    this.in = removeAll(NON_ASCII_CHARS, in);
    return this;
  }

  private Slug dashifyNonWordChars() {
    this.in = dashify(NON_WORD_CHARS, in);
    return this;
  }

  private Slug dashifyWhitespaceChars() {
    this.in = dashify(WHITESPACES_CHARS, in);
    return this;
  }

  private Slug collapseDashes() {
    this.in = dashify(DASHES_IN_ROWS, in);
    return this;
  }

  private Slug removeHeadingDash() {
    if (this.in.startsWith(DASH)) {
      this.in = this.in.substring(1);
    }
    return this;
  }

  private Slug removeTrailingDash() {
    if (this.in.endsWith(DASH)) {
      this.in = this.in.substring(0, this.in.length() - 1);
    }
    return this;
  }

  private String toLowerCase() {
    return in.toLowerCase(Locale.ENGLISH);
  }

  private static String removeAll(Pattern pattern, String in) {
    return replaceAll(pattern, in, "");
  }

  private static String dashify(Pattern pattern, String in) {
    return replaceAll(pattern, in, DASH);
  }

  private static String replaceAll(Pattern pattern, String str, String replacement) {
    return pattern.matcher(str).replaceAll(replacement);
  }
}
