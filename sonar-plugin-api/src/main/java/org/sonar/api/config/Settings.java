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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.DateUtils;

import java.util.*;

/**
 * Project Settings on batch side, Global Settings on server side.
 * <p/>
 * Replace the deprecated component org.apache.commons.configuration.Configuration
 *
 * @since 2.12
 */
public class Settings implements BatchComponent, ServerComponent {

  protected Map<String, String> properties = Maps.newHashMap();
  protected PropertyDefinitions definitions;

  public Settings() {
    this(new PropertyDefinitions());
  }

  public Settings(PropertyDefinitions definitions) {
    this.definitions = definitions;
  }

  public final String getDefaultValue(String key) {
    return definitions.getDefaultValue(key);
  }

  public final boolean hasKey(String key) {
    return properties.containsKey(key);
  }

  public final boolean hasDefaultValue(String key) {
    return StringUtils.isNotEmpty(getDefaultValue(key));
  }

  public final String getString(String key) {
    String value = properties.get(key);
    if (value == null) {
      value = getDefaultValue(key);
    }
    return value;
  }

  public final boolean getBoolean(String key) {
    String value = getString(key);
    return StringUtils.isNotEmpty(value) && Boolean.parseBoolean(value);
  }

  public final int getInt(String key) {
    String value = getString(key);
    if (StringUtils.isNotEmpty(value)) {
      return Integer.parseInt(value);
    }
    return 0;
  }

  public final long getLong(String key) {
    String value = getString(key);
    if (StringUtils.isNotEmpty(value)) {
      return Long.parseLong(value);
    }
    return 0L;
  }

  public final Date getDate(String key) {
    String value = getString(key);
    if (StringUtils.isNotEmpty(value)) {
      return DateUtils.parseDate(value);
    }
    return null;
  }

  public final Date getDateTime(String key) {
    String value = getString(key);
    if (StringUtils.isNotEmpty(value)) {
      return DateUtils.parseDateTime(value);
    }
    return null;
  }

  /**
   * Value is splitted by comma and trimmed.
   * <p/>
   * Examples :
   * <ul>
   * <li>"one,two,three " -> ["one", "two", "three"]</li>
   * <li>"  one, two, three " -> ["one", "two", "three"]</li>
   * <li>"one, , three" -> ["one", "", "three"]</li>
   * </ul>
   */
  public final String[] getStringArray(String key) {
    return getStringArrayBySeparator(key, ",");
  }

  /**
   * Value is splitted and trimmed.
   */
  public final String[] getStringArrayBySeparator(String key, String separator) {
    String value = getString(key);
    if (value != null) {
      String[] strings = StringUtils.splitByWholeSeparator(value, separator);
      String[] result = new String[strings.length];
      for (int index = 0; index < strings.length; index++) {
        result[index] = StringUtils.trim(strings[index]);
      }
      return result;
    }
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  public final List<String> getKeysStartingWith(String prefix) {
    List<String> result = Lists.newArrayList();
    for (String key : properties.keySet()) {
      if (StringUtils.startsWith(key, prefix)) {
        result.add(key);
      }
    }
    return result;
  }

  public final Settings appendProperty(String key, String value) {
    String newValue = properties.get(key);
    if (StringUtils.isEmpty(newValue)) {
      newValue = value;
    } else {
      newValue += "," + value;
    }
    properties.put(key, newValue);
    return this;
  }

  public final Settings setProperty(String key, String value) {
    if (!clearIfNullValue(key, value)) {
      properties.put(key, value);
    }
    return this;
  }

  public final Settings setProperty(String key, Boolean value) {
    if (!clearIfNullValue(key, value)) {
      properties.put(key, String.valueOf(value));
    }
    return this;
  }

  public final Settings setProperty(String key, Integer value) {
    if (!clearIfNullValue(key, value)) {
      properties.put(key, String.valueOf(value));
    }
    return this;
  }

  public final Settings setProperty(String key, Long value) {
    if (!clearIfNullValue(key, value)) {
      properties.put(key, String.valueOf(value));
    }
    return this;
  }

  public final Settings setProperty(String key, Double value) {
    if (!clearIfNullValue(key, value)) {
      properties.put(key, String.valueOf(value));
    }
    return this;
  }

  public final Settings setProperty(String key, Date date) {
    return setProperty(key, date, false);
  }

  public final Settings addProperties(Map<String, String> props) {
    properties.putAll(props);
    return this;
  }

  public final Settings addProperties(Properties props) {
    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      properties.put(entry.getKey().toString(), entry.getValue().toString());
    }
    return this;
  }

  public final Settings addSystemProperties() {
    return addProperties(System.getProperties());
  }

  public final Settings addEnvironmentVariables() {
    return addProperties(System.getenv());
  }

  public final Settings setProperties(Map<String, String> props) {
    properties = Maps.newHashMap(props);
    return this;
  }

  public final Settings setProperty(String key, Date date, boolean includeTime) {
    if (!clearIfNullValue(key, date)) {
      properties.put(key, includeTime ? DateUtils.formatDateTime(date) : DateUtils.formatDate(date));
    }
    return this;
  }

  public final Settings removeProperty(String key) {
    properties.remove(key);
    return this;
  }

  public final Settings clear() {
    properties.clear();
    return this;
  }

  /**
   * @return unmodifiable properties
   */
  public final Map<String, String> getProperties() {
    return Collections.unmodifiableMap(properties);
  }

  public final PropertyDefinitions getDefinitions() {
    return definitions;
  }

  private boolean clearIfNullValue(String key, Object value) {
    if (value == null) {
      properties.remove(key);
      return true;
    }
    return false;
  }

  /**
   * Create empty settings. Definition of available properties is loaded from the given annotated class.
   * This method is usually used by unit tests.
   */
  public static Settings createForComponent(Object component) {
    return new Settings(new PropertyDefinitions(component));
  }
}
