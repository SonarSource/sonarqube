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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Properties;

public class Props {

  private final Properties props;
  private final Encryption encryption;

  public Props(Properties props) {
    this.props = props;
    this.encryption = new Encryption(props.getProperty(AesCipher.ENCRYPTION_SECRET_KEY_PATH));
  }

  @CheckForNull
  public String of(String key) {
    String value = props.getProperty(key);
    if (value != null && encryption.isEncrypted(value)) {
      value = encryption.decrypt(value);
    }
    return value;
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

  public Properties cryptedProperties() {
    return props;
  }

  public Props set(String key, @Nullable String value) {
    props.setProperty(key, value);
    return this;
  }
}
