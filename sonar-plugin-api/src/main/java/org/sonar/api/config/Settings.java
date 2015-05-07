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
package org.sonar.api.config;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.api.utils.DateUtils;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Project settings on batch side, or global settings on server side. This component does not access to database, so
 * property changed via setter methods are not persisted.
 * <p/>
 * <p>
 * For testing, you can create a new empty {@link Settings} component using {@link #Settings()} and then
 * populate it using all variant of {@code setProperty}. <br/>
 * If you want to test with default values of your properties taken into account there are two ways dependening on how you declare your properties.
 * <ul>
 * <li>If you are using annotations like:
 * <pre>
 * <code>{@literal @}Properties({
 *   {@literal @}Property(
 *     key = "sonar.myProp",
 *     defaultValue = "A default value",
 *     name = "My property"),
 * })
 * public class MyPlugin extends SonarPlugin {
 * </code>
 * </pre>
 * then you can use:
 * <pre>
 * <code>new Settings(new PropertyDefinitions(MyPlugin.class))
 * </code>
 * </pre>
 * </li>
 * <li>If you are using the {@link PropertyDefinition#builder(String)} way like:
 * <pre>
 * <code>
 * public class MyPlugin extends SonarPlugin {
 *     public List getExtensions() {
 *       return Arrays.asList(
 *         PropertyDefinition.builder("sonar.myProp").name("My property").defaultValue("A default value").build()
 *       );
 *     }
 *   }
 * </code>
 * </pre>
 * then you can use:
 * <pre>
 * <code>new Settings(new PropertyDefinitions(new MyPlugin().getExtensions()))
 * </code>
 * </pre>
 * </li>
 * </p>
 * @since 2.12
 */
@BatchSide
@ServerSide
public class Settings {

  protected Map<String, String> properties;
  protected PropertyDefinitions definitions;
  private Encryption encryption;

  public Settings() {
    this(new PropertyDefinitions());
  }

  public Settings(PropertyDefinitions definitions) {
    this.properties = Maps.newHashMap();
    this.definitions = definitions;
    this.encryption = new Encryption(null);
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

  public Encryption getEncryption() {
    return encryption;
  }

  public String getDefaultValue(String key) {
    return definitions.getDefaultValue(key);
  }

  public boolean hasKey(String key) {
    return properties.containsKey(key);
  }

  public boolean hasDefaultValue(String key) {
    return StringUtils.isNotEmpty(getDefaultValue(key));
  }

  public String getString(String key) {
    String value = getClearString(key);
    if (value != null && encryption.isEncrypted(value)) {
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
    doOnGetProperties(key);
    String validKey = definitions.validKey(key);
    String value = properties.get(validKey);
    if (value == null) {
      value = getDefaultValue(validKey);
    }
    return value;
  }

  public boolean getBoolean(String key) {
    String value = getString(key);
    return StringUtils.isNotEmpty(value) && Boolean.parseBoolean(value);
  }

  /**
   * @return the value as int. If the property does not exist and has no default value, then 0 is returned.
   */
  public int getInt(String key) {
    String value = getString(key);
    if (StringUtils.isNotEmpty(value)) {
      return Integer.parseInt(value);
    }
    return 0;
  }

  public long getLong(String key) {
    String value = getString(key);
    if (StringUtils.isNotEmpty(value)) {
      return Long.parseLong(value);
    }
    return 0L;
  }

  public Date getDate(String key) {
    String value = getString(key);
    if (StringUtils.isNotEmpty(value)) {
      return DateUtils.parseDate(value);
    }
    return null;
  }

  public Date getDateTime(String key) {
    String value = getString(key);
    if (StringUtils.isNotEmpty(value)) {
      return DateUtils.parseDateTime(value);
    }
    return null;
  }

  public Float getFloat(String key) {
    String value = getString(key);
    if (StringUtils.isNotEmpty(value)) {
      try {
        return Float.valueOf(value);
      } catch (NumberFormatException e) {
        throw new IllegalStateException(String.format("The property '%s' is not a float value", key));
      }
    }
    return null;
  }

  public Double getDouble(String key) {
    String value = getString(key);
    if (StringUtils.isNotEmpty(value)) {
      try {
        return Double.valueOf(value);
      } catch (NumberFormatException e) {
        throw new IllegalStateException(String.format("The property '%s' is not a double value", key));
      }
    }
    return null;
  }

  /**
   * Value is split by comma and trimmed. Never returns null.
   * <p/>
   * Examples :
   * <ul>
   * <li>"one,two,three " -> ["one", "two", "three"]</li>
   * <li>"  one, two, three " -> ["one", "two", "three"]</li>
   * <li>"one, , three" -> ["one", "", "three"]</li>
   * </ul>
   */
  public String[] getStringArray(String key) {
    PropertyDefinition property = getDefinitions().get(key);
    if ((null != property) && (property.multiValues())) {
      String value = getString(key);
      if (value == null) {
        return ArrayUtils.EMPTY_STRING_ARRAY;
      }

      List<String> values = new ArrayList<>();
      for (String v : Splitter.on(",").trimResults().split(value)) {
        values.add(v.replace("%2C", ","));
      }
      return values.toArray(new String[values.size()]);
    }

    return getStringArrayBySeparator(key, ",");
  }

  /**
   * Value is split by carriage returns.
   *
   * @return non-null array of lines. The line termination characters are excluded.
   * @since 3.2
   */
  public String[] getStringLines(String key) {
    String value = getString(key);
    if (Strings.isNullOrEmpty(value)) {
      return ArrayUtils.EMPTY_STRING_ARRAY;
    }
    return value.split("\r?\n|\r", -1);
  }

  /**
   * Value is splitted and trimmed.
   */
  public String[] getStringArrayBySeparator(String key, String separator) {
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

  public List<String> getKeysStartingWith(String prefix) {
    List<String> result = new ArrayList<>();
    for (String key : properties.keySet()) {
      if (StringUtils.startsWith(key, prefix)) {
        result.add(key);
      }
    }
    return result;
  }

  public Settings appendProperty(String key, String value) {
    String newValue = properties.get(definitions.validKey(key));
    if (StringUtils.isEmpty(newValue)) {
      newValue = StringUtils.trim(value);
    } else {
      newValue += "," + StringUtils.trim(value);
    }
    return setProperty(key, newValue);
  }

  public Settings setProperty(String key, @Nullable String[] values) {
    PropertyDefinition property = getDefinitions().get(key);
    if ((null == property) || (!property.multiValues())) {
      throw new IllegalStateException("Fail to set multiple values on a single value property " + key);
    }

    String text = null;
    if (values != null) {
      List<String> escaped = new ArrayList<>();
      for (String value : values) {
        if (null != value) {
          escaped.add(value.replace(",", "%2C"));
        } else {
          escaped.add("");
        }
      }

      String escapedValue = Joiner.on(',').join(escaped);
      text = StringUtils.trim(escapedValue);
    }
    return setProperty(key, text);
  }

  public Settings setProperty(String key, @Nullable String value) {
    String validKey = definitions.validKey(key);
    if (value == null) {
      properties.remove(validKey);
      doOnRemoveProperty(validKey);
    } else {
      properties.put(validKey, StringUtils.trim(value));
      doOnSetProperty(validKey, value);
    }
    return this;
  }

  public Settings setProperty(String key, @Nullable Boolean value) {
    return setProperty(key, value == null ? null : String.valueOf(value));
  }

  public Settings setProperty(String key, @Nullable Integer value) {
    return setProperty(key, value == null ? null : String.valueOf(value));
  }

  public Settings setProperty(String key, @Nullable Long value) {
    return setProperty(key, value == null ? null : String.valueOf(value));
  }

  public Settings setProperty(String key, @Nullable Double value) {
    return setProperty(key, value == null ? null : String.valueOf(value));
  }

  public Settings setProperty(String key, @Nullable Float value) {
    return setProperty(key, value == null ? null : String.valueOf(value));
  }

  public Settings setProperty(String key, @Nullable Date date) {
    return setProperty(key, date, false);
  }

  public Settings addProperties(Map<String, String> props) {
    for (Map.Entry<String, String> entry : props.entrySet()) {
      setProperty(entry.getKey(), entry.getValue());
    }
    return this;
  }

  public Settings addProperties(Properties props) {
    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      setProperty(entry.getKey().toString(), entry.getValue().toString());
    }
    return this;
  }

  /**
   * @deprecated since 4.4 For embedding purpose all properties should be provided by the bootstrapper
   */
  @Deprecated
  public Settings addSystemProperties() {
    return addProperties(System.getProperties());
  }

  /**
   * @deprecated since 4.4 For embedding purpose all properties should be provided by the bootstrapper
   */
  @Deprecated
  public Settings addEnvironmentVariables() {
    return addProperties(System.getenv());
  }

  public Settings setProperties(Map<String, String> props) {
    clear();
    return addProperties(props);
  }

  public Settings setProperty(String key, @Nullable Date date, boolean includeTime) {
    if (date == null) {
      return removeProperty(key);
    }
    return setProperty(key, includeTime ? DateUtils.formatDateTime(date) : DateUtils.formatDate(date));
  }

  public Settings removeProperty(String key) {
    return setProperty(key, (String) null);
  }

  public Settings clear() {
    properties.clear();
    doOnClearProperties();
    return this;
  }

  /**
   * @return immutable properties
   */
  public Map<String, String> getProperties() {
    return ImmutableMap.copyOf(properties);
  }

  public PropertyDefinitions getDefinitions() {
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
    // can be overridden
  }

  protected void doOnRemoveProperty(String key) {
    // can be overridden
  }

  protected void doOnClearProperties() {
    // can be overridden
  }

  protected void doOnGetProperties(String key) {
    // can be overridden
  }
}
