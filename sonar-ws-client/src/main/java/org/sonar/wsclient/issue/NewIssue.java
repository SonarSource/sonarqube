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
package org.sonar.wsclient.issue;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 3.6
 */
public class NewIssue {
  static final String BASE_URL = "/api/issues/create";
  private final Map<String, Object> params = new HashMap<String, Object>();

  private NewIssue() {
  }

  public static NewIssue create() {
    return new NewIssue();
  }

  Map<String, Object> urlParams() {
    return params;
  }

  public NewIssue severity(String s) {
    params.put("severity", s);
    return this;
  }

  public NewIssue component(String s) {
    params.put("component", s);
    return this;
  }

  public NewIssue rule(String s) {
    params.put("rule", s);
    return this;
  }

  /**
   * Optional line
   */
  public NewIssue line(int i) {
    params.put("line", i);
    return this;
  }

  public NewIssue description(String s) {
    params.put("desc", s);
    return this;
  }

  public NewIssue title(String s) {
    params.put("title", s);
    return this;
  }

  public NewIssue cost(Double d) {
    params.put("cost", d);
    return this;
  }

  public NewIssue attribute(String key, String value) {
    params.put("attr[" + key + "]", value);
    return this;
  }
}
