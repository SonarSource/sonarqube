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
package org.sonar.process;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Properties;

public class Props {

  private final Properties props;

  Props(Properties props) {
    this.props = props;
  }

  public String of(String key) {
    return props.getProperty(key);
  }

  public String of(String key, @Nullable String defaultValue) {
    String s = of(key);
    return s == null ? defaultValue : s;
  }

  public boolean booleanOf(String key) {
    String s = of(key);
    return s != null && Boolean.parseBoolean(s);
  }

  public boolean booleanOf(String key, boolean defaultValue) {
    String s = of(key);
    return s != null ? Boolean.parseBoolean(s) : defaultValue;
  }

  public Integer intOf(String key) {
    String s = of(key);
    if (s != null && !"".equals(s)) {
      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException e) {
        throw new IllegalStateException("Value of property " + key + " is not an integer: " + s, e);
      }
    }
    return null;
  }

  public int intOf(String key, int defaultValue) {
    Integer i = intOf(key);
    return i == null ? defaultValue : i;
  }

  public Properties properties() {
    return props;
  }

  public static Props create(Properties properties) {
    Properties p = new Properties();

    // order is important : the last override the first
    p.putAll(System.getenv());
    p.putAll(System.getProperties());
    p.putAll(properties);

    p = ConfigurationUtils.interpolateEnvVariables(p);
    p = decrypt(p);

    // Set all properties as system properties to pass them to PlatformServletContextListener
    // System.setProperties(p);

    return new Props(p);
  }

  static Properties decrypt(Properties properties) {
    Encryption encryption = new Encryption(properties.getProperty(AesCipher.ENCRYPTION_SECRET_KEY_PATH));
    Properties result = new Properties();

    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String key = (String) entry.getKey();
      String value = (String) entry.getValue();
      if (encryption.isEncrypted(value)) {
        value = encryption.decrypt(value);
      }
      result.setProperty(key, value);
    }
    return result;
  }
}
