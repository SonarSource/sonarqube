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
package org.sonar.wsclient.issue;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 3.6
 */
public class NewIssue {

  private final Map<String, Object> params = new HashMap<String, Object>();

  private NewIssue() {
  }

  public static NewIssue create() {
    return new NewIssue();
  }

  public Map<String, Object> urlParams() {
    return params;
  }

  /**
   * Optionally set the severity, in INFO, MINOR, MAJOR, CRITICAL or BLOCKER. Default value is MAJOR.
   */
  public NewIssue severity(@Nullable String s) {
    params.put("severity", s);
    return this;
  }

  public NewIssue component(String s) {
    params.put("component", s);
    return this;
  }

  /**
   * Rule key prefixed by the repository, for example "checkstyle:AvoidCycle".
   */
  public NewIssue rule(String s) {
    params.put("rule", s);
    return this;
  }

  /**
   * Optional line, starting from 1.
   */
  public NewIssue line(@Nullable Integer i) {
    params.put("line", i);
    return this;
  }

  public NewIssue message(@Nullable String s) {
    params.put("message", s);
    return this;
  }

  public NewIssue effortToFix(@Nullable Double d) {
    params.put("effortToFix", d);
    return this;
  }

  public NewIssue attribute(String key, String value) {
    params.put("attr[" + key + "]", value);
    return this;
  }
}
