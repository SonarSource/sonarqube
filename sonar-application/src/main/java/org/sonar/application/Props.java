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
package org.sonar.application;

import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * TODO support env substitution
 */
class Props {
  private final Properties props;

  Props(Properties props) {
    this.props = props;
  }

  String of(String key) {
    return props.getProperty(key);
  }

  String of(String key, @Nullable String defaultValue) {
    String s = of(key);
    return s == null ? defaultValue : s;
  }

  boolean booleanOf(String key) {
    String s = of(key);
    return s != null && Boolean.parseBoolean(s);
  }

  Integer intOf(String key) {
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

  int intOf(String key, int defaultValue) {
    Integer i = intOf(key);
    return i == null ? defaultValue : i;
  }

  static Props create(Env env) {
    File propsFile = env.file("conf/sonar.properties");
    Properties p = new Properties();
    FileReader reader = null;
    try {
      reader = new FileReader(propsFile);
      p.load(reader);
      p.putAll(System.getProperties());
      return new Props(p);

    } catch (Exception e) {
      throw new IllegalStateException("File does not exist or can't be open: " + propsFile, e);

    } finally {
      IOUtils.closeQuietly(reader);
    }
  }
}
