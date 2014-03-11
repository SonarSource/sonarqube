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

/**
 * @since 3.6
 */
public interface ActionPlan {

  /**
   * Unique key
   */
  String key();

  String project();

  String name();

  @CheckForNull
  String description();

  String status();

  /**
   * Login of the user who created the action plan.
   */
  String userLogin();

  @CheckForNull
  Date deadLine();

  Date createdAt();

  Date updatedAt();

  @CheckForNull
  Integer totalIssues();

  @CheckForNull
  Integer unresolvedIssues();

}
