/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.rule;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.PropertyType;

import javax.annotation.Nullable;

/**
 * @since 4.2
 */
public final class RuleParamType {

  private static final String FIELD_SEPARATOR = "|";
  private static final String OPTION_SEPARATOR = ",";

  public static final RuleParamType STRING = new RuleParamType("STRING");
  public static final RuleParamType TEXT = new RuleParamType("TEXT");
  public static final RuleParamType BOOLEAN = new RuleParamType("BOOLEAN");
  public static final RuleParamType INTEGER = new RuleParamType("INTEGER");
  public static final RuleParamType FLOAT = new RuleParamType("FLOAT");

  private final String type;
  private final String[] options;

  // format is "type|comma-separated list of options", for example "INTEGER" or "SINGLE_SELECT_LIST|foo=one,bar,baz=two"
  private final String key;

  private RuleParamType(String type, String... options) {
    this.type = type;
    this.options = options;
    StringBuilder sb = new StringBuilder();
    sb.append(type);
    if (options.length > 0) {
      sb.append(FIELD_SEPARATOR);
      for (String option : options) {
        sb.append(StringEscapeUtils.escapeCsv(option));
        sb.append(OPTION_SEPARATOR);
      }
    }
    this.key = sb.toString();
  }

  public String type() {
    return type;
  }

  public String[] options() {
    return options;
  }

  public static RuleParamType ofValues(String... acceptedValues) {
    // reuse the same type as plugin properties in order to
    // benefit from shared helpers (validation, HTML component)
    String type = PropertyType.SINGLE_SELECT_LIST.name();
    return new RuleParamType(type, acceptedValues);
  }

  // TODO validate format
  public static RuleParamType parse(String s) {
    // deprecated formats
    if ("i".equals(s) || "i{}".equals(s)) {
      return INTEGER;
    }
    if ("s".equals(s) || "s{}".equals(s) || "r".equals(s)) {
      return STRING;
    }
    if ("b".equals(s)) {
      return BOOLEAN;
    }
    if (s.startsWith("s[")) {
      String values = StringUtils.substringBetween(s, "[", "]");
      return ofValues(StringUtils.split(values, ','));
    }

    // standard format
    String format = StringUtils.substringBefore(s, FIELD_SEPARATOR);
    String options = StringUtils.substringAfter(s, FIELD_SEPARATOR);
    if (StringUtils.isBlank(options)) {
      return new RuleParamType(format);
    }
    return new RuleParamType(format, options.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)"));
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
