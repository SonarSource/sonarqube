/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.api.config;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.utils.DateUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @since 3.3
 */
public final class PropertySetValue {
  private final Map<String, String> keyValues;

  private PropertySetValue(Map<String, String> keyValues) {
    this.keyValues = ImmutableMap.copyOf(keyValues);
  }

  public static PropertySetValue create(Map<String, String> keyValues) {
    return new PropertySetValue(keyValues);
  }

  /**
   * @return the field as String. If the field does not exist, then an empty string is returned.
   */
  public String getString(String fieldName) {
    String value = keyValues.get(fieldName);
    return (value == null) ? "" : value;
  }

  /**
   * @return the field as int. If the field does not exist, then <code>0</code> is returned.
   */
  public int getInt(String fieldName) {
    String value = keyValues.get(fieldName);
    return (value == null) ? 0 : Integer.parseInt(value);
  }

  /**
   * @return the field as boolean. If the field does not exist, then <code>false</code> is returned.
   */
  public boolean getBoolean(String fieldName) {
    String value = keyValues.get(fieldName);
    return (value == null) ? false : Boolean.parseBoolean(value);
  }

  /**
   * @return the field as float. If the field does not exist, then <code>0.0</code> is returned.
   */
  public float getFloat(String fieldName) {
    String value = keyValues.get(fieldName);
    return (value == null) ? 0f : Float.parseFloat(value);
  }

  /**
   * @return the field as long. If the field does not exist, then <code>0L</code> is returned.
   */
  public long getLong(String fieldName) {
    String value = keyValues.get(fieldName);
    return (value == null) ? 0L : Long.parseLong(value);
  }

  /**
   * @return the field as Date. If the field does not exist, then <code>null</code> is returned.
   */
  public Date getDate(String fieldName) {
    String value = keyValues.get(fieldName);
    return (value == null) ? null : DateUtils.parseDate(value);
  }

  /**
   * @return the field as Date with time. If the field does not exist, then <code>null</code> is returned.
   */
  public Date getDateTime(String fieldName) {
    String value = keyValues.get(fieldName);
    return (value == null) ? null : DateUtils.parseDateTime(value);
  }

  /**
   * @return the field as an array of String. If the field does not exist, then an empty array is returned.
   */
  public String[] getStringArray(String fieldName) {
    String value = keyValues.get(fieldName);
    if (value == null) {
      return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    List<String> values = Lists.newArrayList();
    for (String v : Splitter.on(",").trimResults().split(value)) {
      values.add(v.replace("%2C", ","));
    }
    return values.toArray(new String[values.size()]);
  }
}
