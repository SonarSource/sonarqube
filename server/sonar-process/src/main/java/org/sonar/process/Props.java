/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.process;

import java.io.File;
import java.util.Properties;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

public class Props {

  private final Properties properties;
  private final Encryption encryption;

  public Props(Properties props) {
    this.properties = new Properties();
    props.forEach((k, v) -> this.properties.put(k.toString().trim(), v == null ? null : v.toString().trim()));
    this.encryption = new Encryption(props.getProperty(AesCipher.ENCRYPTION_SECRET_KEY_PATH));
  }

  public boolean contains(String key) {
    return properties.containsKey(key);
  }

  @CheckForNull
  public String value(String key) {
    String value = properties.getProperty(key);
    if (value != null && encryption.isEncrypted(value)) {
      value = encryption.decrypt(value);
    }
    return value;
  }

  public String nonNullValue(String key) {
    String value = value(key);
    if (value == null) {
      throw new IllegalArgumentException("Missing property: " + key);
    }
    return value;
  }

  @CheckForNull
  public String value(String key, @Nullable String defaultValue) {
    String s = value(key);
    return s == null ? defaultValue : s;
  }

  public boolean valueAsBoolean(String key) {
    String s = value(key);
    return s != null && Boolean.parseBoolean(s);
  }

  public boolean valueAsBoolean(String key, boolean defaultValue) {
    String s = value(key);
    return s != null ? Boolean.parseBoolean(s) : defaultValue;
  }

  public File nonNullValueAsFile(String key) {
    String s = value(key);
    if (s == null) {
      throw new IllegalArgumentException("Property " + key + " is not set");
    }
    return new File(s);
  }

  @CheckForNull
  public Integer valueAsInt(String key) {
    String s = value(key);
    if (s != null && !"".equals(s)) {
      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException e) {
        throw new IllegalStateException("Value of property " + key + " is not an integer: " + s, e);
      }
    }
    return null;
  }

  public int valueAsInt(String key, int defaultValue) {
    Integer i = valueAsInt(key);
    return i == null ? defaultValue : i;
  }

  public Properties rawProperties() {
    return properties;
  }

  public Props set(String key, @Nullable String value) {
    if (value != null) {
      properties.setProperty(key, value);
    }
    return this;
  }

  public void setDefault(String key, String value) {
    String s = properties.getProperty(key);
    if (StringUtils.isBlank(s)) {
      properties.setProperty(key, value);
    }
  }

}
