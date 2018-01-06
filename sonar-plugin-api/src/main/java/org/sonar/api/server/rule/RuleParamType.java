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
package org.sonar.api.server.rule;

import java.util.List;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.PropertyType;

import static java.util.Arrays.asList;


/**
 * @since 4.2
 */
public final class RuleParamType {

  private static final String OPTION_SEPARATOR = ",";

  public static final RuleParamType STRING = new RuleParamType("STRING");
  public static final RuleParamType TEXT = new RuleParamType("TEXT");
  public static final RuleParamType BOOLEAN = new RuleParamType("BOOLEAN");
  public static final RuleParamType INTEGER = new RuleParamType("INTEGER");
  public static final RuleParamType FLOAT = new RuleParamType("FLOAT");

  private static final String CSV_SPLIT_REGEX = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
  private static final String VALUES_PARAM = "values";
  private static final String MULTIPLE_PARAM = "multiple";
  private static final String PARAMETER_SEPARATOR = "=";

  private final String type;
  private final List<String> values;
  private final boolean multiple;

  // format is "type|comma-separated list of options", for example "INTEGER" or "SINGLE_SELECT_LIST|foo=one,bar,baz=two"
  private final String key;

  private RuleParamType(String type, String... options) {
    this(type, false, options);
  }

  private RuleParamType(String type, boolean multiple, String... values) {
    this.type = type;
    this.values = asList(values);
    StringBuilder sb = new StringBuilder();
    sb.append(type);
    if (multiple) {
      sb.append(OPTION_SEPARATOR);
      sb.append(MULTIPLE_PARAM + PARAMETER_SEPARATOR);
      sb.append(Boolean.toString(multiple));
    }
    if (values.length > 0) {
      sb.append(OPTION_SEPARATOR);
      sb.append(VALUES_PARAM + PARAMETER_SEPARATOR);
      sb.append(StringEscapeUtils.escapeCsv(valuesToCsv(values)));
    }
    this.key = sb.toString();
    this.multiple = multiple;
  }

  private static String valuesToCsv(String... values) {
    StringBuilder sb = new StringBuilder();
    for (String value : values) {
      sb.append(StringEscapeUtils.escapeCsv(value));
      sb.append(OPTION_SEPARATOR);
    }
    return sb.toString();
  }

  public String type() {
    return type;
  }

  public List<String> values() {
    return values;
  }

  public boolean multiple() {
    return multiple;
  }

  public static RuleParamType singleListOfValues(String... acceptedValues) {
    // reuse the same type as plugin properties in order to
    // benefit from shared helpers (validation, HTML component)
    String type = PropertyType.SINGLE_SELECT_LIST.name();
    return new RuleParamType(type, acceptedValues);
  }

  public static RuleParamType multipleListOfValues(String... acceptedValues) {
    // reuse the same type as plugin properties in order to
    // benefit from shared helpers (validation, HTML component)
    String type = PropertyType.SINGLE_SELECT_LIST.name();
    return new RuleParamType(type, true, acceptedValues);
  }

  // TODO validate format
  public static RuleParamType parse(String s) {
    // deprecated formats
    if ("i".equals(s) || "i{}".equals(s)) {
      return INTEGER;
    }
    if ("s".equals(s) || "s{}".equals(s) || "r".equals(s) || "REGULAR_EXPRESSION".equals(s)) {
      return STRING;
    }
    if ("b".equals(s)) {
      return BOOLEAN;
    }
    if (s.startsWith("s[")) {
      String values = StringUtils.substringBetween(s, "[", "]");
      return multipleListOfValues(StringUtils.split(values, ','));
    }

    // standard format
    String format = StringUtils.substringBefore(s, OPTION_SEPARATOR);
    String values = null;
    boolean multiple = false;
    String[] options = s.split(CSV_SPLIT_REGEX);
    for (String option : options) {
      String opt = StringEscapeUtils.unescapeCsv(option);
      if (opt.startsWith(VALUES_PARAM + PARAMETER_SEPARATOR)) {
        values = StringEscapeUtils.unescapeCsv(StringUtils.substringAfter(opt, VALUES_PARAM + PARAMETER_SEPARATOR));
      } else if (opt.startsWith(MULTIPLE_PARAM + PARAMETER_SEPARATOR)) {
        multiple = Boolean.parseBoolean(StringUtils.substringAfter(opt, MULTIPLE_PARAM + PARAMETER_SEPARATOR));
      }
    }
    if (values == null || StringUtils.isBlank(values)) {
      return new RuleParamType(format);
    }
    return new RuleParamType(format, multiple, values.split(CSV_SPLIT_REGEX));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RuleParamType that = (RuleParamType) o;
    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return key;
  }
}
