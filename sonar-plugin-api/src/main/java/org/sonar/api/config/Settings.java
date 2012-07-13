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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.DateUtils;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Project Settings on batch side, Global Settings on server side. This component does not access to database, so
 * property changed via setter methods are not persisted.
 * <p/>
 * <p>
 * This component replaces the deprecated org.apache.commons.configuration.Configuration
 * </p>
 *
 * @since 2.12
 */
public class Settings implements BatchComponent, ServerComponent {

  protected final Map<String, String> properties;
  protected final PropertyDefinitions definitions;
  private final Encryption encryption;

  public Settings() {
    this(new PropertyDefinitions());
  }

  public Settings(PropertyDefinitions definitions) {
    this.properties = Maps.newHashMap();
    this.definitions = definitions;
    this.encryption = new Encryption(this);
  }

  /**
   * Clone settings. Actions are not propagated to cloned settings.
   *
   * @since 3.1
   */
  public Settings(Settings other) {
    this.properties = Maps.newHashMap(other.properties);
    this.definitions = other.definitions;
    this.encryption = other.encryption;
  }

  public final Encryption getEncryption() {
    return encryption;
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
    } else if (encryption.isEncrypted(value)) {
      try {
        value = encryption.decrypt(value);
      } catch (Exception e) {
        throw new IllegalStateException("Fail to decrypt the property " + key + ". Please check your secret key.", e);
      }
    }
    return value;
  }

  /**
   * Does not decrypt value.
   */
  protected String getClearString(String key) {
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

  /**
   * @return the value as int. If the property does not exist and has no default value, then 0 is returned.
   */
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
   * Value is split by comma and trimmed.
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
   * Value is split by carriage returns.
   *
   * @return non-null array of lines. The line termination characters are excluded.
   * @since 3.2
   */
  public final String[] getStringLines(String key) {
    String value = getString(key);
    if (Strings.isNullOrEmpty(value)) {
      return ArrayUtils.EMPTY_STRING_ARRAY;
    }
    return value.split("\r?\n|\r", -1);
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
      newValue = StringUtils.trim(value);
    } else {
      newValue += "," + StringUtils.trim(value);
    }
    return setProperty(key, newValue);
  }

  public final Settings setProperty(String key, @Nullable String value) {
    if (value == null) {
      properties.remove(key);
      doOnRemoveProperty(key);
    } else {
      properties.put(key, StringUtils.trim(value));
      doOnSetProperty(key, value);
    }
    return this;
  }

  public final Settings setProperty(String key, @Nullable Boolean value) {
    return setProperty(key, String.valueOf(value));
  }

  public final Settings setProperty(String key, @Nullable Integer value) {
    return setProperty(key, String.valueOf(value));
  }

  public final Settings setProperty(String key, @Nullable Long value) {
    return setProperty(key, String.valueOf(value));
  }

  public final Settings setProperty(String key, @Nullable Double value) {
    return setProperty(key, String.valueOf(value));
  }

  public final Settings setProperty(String key, @Nullable Date date) {
    return setProperty(key, date, false);
  }

  public final Settings addProperties(Map<String, String> props) {
    for (Map.Entry<String, String> entry : props.entrySet()) {
      setProperty(entry.getKey(), entry.getValue());
    }
    return this;
  }

  public final Settings addProperties(Properties props) {
    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      setProperty(entry.getKey().toString(), entry.getValue().toString());
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
    clear();
    return addProperties(props);
  }

  public final Settings setProperty(String key, @Nullable Date date, boolean includeTime) {
    return setProperty(key, includeTime ? DateUtils.formatDateTime(date) : DateUtils.formatDate(date));
  }

  public final Settings removeProperty(String key) {
    return setProperty(key, (String) null);
  }

  public final Settings clear() {
    properties.clear();
    doOnClearProperties();
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

  /**
   * Create empty settings. Definition of available properties is loaded from the given annotated class.
   * This method is usually used by unit tests.
   */
  public static Settings createForComponent(Object component) {
    return new Settings(new PropertyDefinitions(component));
  }

  protected void doOnSetProperty(String key, @Nullable String value) {
  }

  protected void doOnRemoveProperty(String key) {
  }

  protected void doOnClearProperties() {
  }
}
