/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.charts;

import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.sonar.api.utils.SonarException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The class to hold parameters to configure a chart
 * @since 1.10
 * @deprecated in 4.5.1, replaced by Javascript charts
 */
@Deprecated
public class ChartParameters {
  private static final String[] EMPTY = new String[0];

  public static final String PARAM_WIDTH = "w";
  public static final String PARAM_BACKGROUND_COLOR = "bgc";
  public static final String PARAM_HEIGHT = "h";
  public static final int MAX_WIDTH = 900;
  public static final String PARAM_LOCALE = "locale";

  public static final int MAX_HEIGHT = 900;
  public static final int DEFAULT_WIDTH = 200;

  public static final int DEFAULT_HEIGHT = 200;


  private final Map<String, String> params;

  /**
   * Creates a ChartParameter based on a list of parameters
   * @param params the list of parameters
   */
  public ChartParameters(Map<String, String> params) {
    this.params = params;
  }

  /**
   * Creates a Chartparameter based on a query string with a format key1=value1&key2=value2...
   *
   * @param  queryString string
   */
  public ChartParameters(String queryString) {
    this.params = new HashMap<String, String>();
    String[] groups = StringUtils.split(queryString, "&");
    for (String group : groups) {
      String[] keyval = StringUtils.split(group, "=");
      params.put(keyval[0], keyval[1]);
    }
  }

  /**
   * Shortcut to getValue with no decoding and no default value
   * @param key the param ket
   * @return the value of the param
   */
  public String getValue(String key) {
    return getValue(key, "", false);
  }

  /**
   * Returns the [decoded or not] value of a param from its key or the default value
   * if id does not exist
   *
   * @param key the param ket
   * @param defaultValue the default value if not exist
   * @param decode whther the value should be decoded
   * @return the value of the param
   */

  public String getValue(String key, String defaultValue, boolean decode) {
    String val = params.get(key);
    if (decode) {
      val = decode(val);
    }
    if (val == null) {
      val = defaultValue;
    }
    return val;
  }

  /**
   * Returns an array of a param values, given its key and the values delimiter
   *
   * @param key the param key
   * @param delimiter the values delimiter
   * @return the list of vaalues
   */
  public String[] getValues(String key, String delimiter) {
    String value = params.get(key);
    if (value != null) {
      return StringUtils.split(value, delimiter);
    }
    return EMPTY;
  }

  /**
   * Returns an array of a param values, given its key and the values delimiter
   * Values can be decoded or not
   *
   * @param key the param key
   * @param delimiter the values delimiter
   * @param decode whether to decode values
   * @return the list of vaalues
   */
  public String[] getValues(String key, String delimiter, boolean decode) {
    String value = params.get(key);
    if (value != null) {
      if (decode) {
        value = decode(value);
      }
      return new StrTokenizer(value, delimiter).setIgnoreEmptyTokens(false).getTokenArray();
    }
    return EMPTY;
  }

  /**
   * Get the chart width
   *
   * @return width
   */
  public int getWidth() {
    int width = Integer.parseInt(getValue(PARAM_WIDTH, "" + DEFAULT_WIDTH, false));
    return Math.min(width, MAX_WIDTH);
  }

  /**
   * Get the chart height
   *
   * @return height
   */
  public int getHeight() {
    int height = Integer.parseInt(getValue(PARAM_HEIGHT, "" + DEFAULT_HEIGHT, false));
    return Math.min(height, MAX_HEIGHT);
  }

  /**
   * Get the Locale
   *
   * @return Locale
   */
  public Locale getLocale() {
    String locale = getValue(PARAM_LOCALE);
    if (StringUtils.isNotBlank(locale)) {
      return new Locale(locale);
    }
    return Locale.ENGLISH;
  }

  private String decode(String val) {
    if (val != null) {
      try {
        val = URLDecoder.decode(val, CharEncoding.UTF_8);
      } catch (UnsupportedEncodingException e) {
        throw new SonarException("Decoding chart parameter : " + val, e);
      }
    }
    return val;
  }
}
