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

import javax.annotation.CheckForNull;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @since 3.6
 */
public interface Issue {

  /**
   * Unique key
   */
  String key();

  String componentKey();

  /**
   * @deprecated since 4.4. Use {@link #componentKey()} instead
   */
  @Deprecated
  Long componentId();

  String projectKey();

  String ruleKey();

  String severity();

  @CheckForNull
  String message();

  @CheckForNull
  Integer line();

  @CheckForNull
  String debt();

  String status();

  /**
   * The resolution type. Null if the issue is not resolved.
   */
  @CheckForNull
  String resolution();

  @CheckForNull
  String reporter();

  /**
   * Login of assignee. Null if issue is not assigned.
   */
  @CheckForNull
  String assignee();

  /**
   * SCM account
   */
  @CheckForNull
  String author();

  @CheckForNull
  String actionPlan();

  Date creationDate();

  Date updateDate();

  @CheckForNull
  Date closeDate();

  @CheckForNull
  String attribute(String key);

  Map<String, String> attributes();

  /**
   * Non-null list of comments
   */
  List<IssueComment> comments();
}
