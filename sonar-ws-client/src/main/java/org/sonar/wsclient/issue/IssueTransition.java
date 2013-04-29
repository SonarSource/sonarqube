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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @since 3.6
 */
public class IssueTransition {
  static final String BASE_URL = "/api/issues/do_transition";
  private final Map<String, Object> params = new LinkedHashMap<String, Object>();

  private IssueTransition() {
  }

  public static IssueTransition create() {
    return new IssueTransition();
  }

  Map<String, Object> urlParams() {
    return params;
  }

  /**
   * Ask to apply a transition
   */
  public IssueTransition transition(String s) {
    params.put("transition", s);
    return this;
  }

}
