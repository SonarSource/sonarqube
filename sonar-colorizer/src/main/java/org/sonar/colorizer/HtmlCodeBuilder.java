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
package org.sonar.colorizer;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated since 4.5.2 replace by highlighting mechanism
 */
@Deprecated
public class HtmlCodeBuilder implements Appendable {

  private StringBuilder colorizedCode = new StringBuilder();
  private Map variables = new HashMap();

  @Override
  public Appendable append(CharSequence csq) {
    for (int i = 0; i < csq.length(); i++) {
      append(csq.charAt(i));
    }
    return this;
  }

  @Override
  public Appendable append(char c) {
    if (c == '<') {
      colorizedCode.append("&lt;");
    } else if (c == '>') {
      colorizedCode.append("&gt;");
    } else if (c == '&') {
      colorizedCode.append("&amp;");
    } else {
      colorizedCode.append(c);
    }
    return this;
  }

  @Override
  public Appendable append(CharSequence csq, int start, int end) {
    for (int i = start; i < end; i++) {
      append(csq.charAt(i));
    }
    return this;
  }

  public void appendWithoutTransforming(String htmlTag) {
    colorizedCode.append(htmlTag);
  }

  @Override
  public String toString() {
    return colorizedCode.toString();
  }

  public StringBuilder getColorizedCode() {
    return colorizedCode;
  }

  /**
   * Save a stateful variable.
   *
   * @param key
   *          can NOT be null
   * @param value
   *          can be null
   */
  public void setVariable(Object key, @Nullable Object value) {
    variables.put(key, value);
  }

  /**
   * Get a stateful variable. Return null if not found.
   */
  @CheckForNull
  public Object getVariable(Object key) {
    return variables.get(key);
  }

  /**
   * Get a stateful variable. Return the default value if not found.
   */
  public Object getVariable(Object key, Object defaultValue) {
    Object result = variables.get(key);
    if (result == null) {
      result = defaultValue;
    }
    return result;
  }

  /**
   * All stateful variables
   */
  public Map getVariables() {
    return variables;
  }

}
