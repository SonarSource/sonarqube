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
package org.sonar.api.issue;

import org.sonar.api.rule.RuleKey;

import java.util.Date;
import java.util.Map;

/**
 * @since 3.6
 */
public interface Issue {

  int DESCRIPTION_MAX_SIZE = 4000;
  String STATUS_OPEN = "OPEN";
  String STATUS_REOPENED = "REOPENED";
  String STATUS_RESOLVED = "RESOLVED";
  String STATUS_CLOSED = "CLOSED";

  String RESOLUTION_OPEN = "OPEN";
  String RESOLUTION_FIXED = "FIXED";
  String RESOLUTION_FALSE_POSITIVE = "FALSE-POSITIVE";

  /**
   * Unique generated key
   */
  String key();

  String componentKey();

  RuleKey ruleKey();

  String severity();

  String description();

  Integer line();

  Double cost();

  String status();

  String resolution();

  String userLogin();

  String assignee();

  boolean manual();

  Date createdAt();

  Date updatedAt();

  Date closedAt();

  String attribute(String key);

  Map<String, String> attributes();

  String authorLogin();

  /**
   * Used only during project scan.
   */
  boolean isNew();
}
