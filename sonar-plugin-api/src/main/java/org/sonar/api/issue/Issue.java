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

import javax.annotation.CheckForNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @since 3.6
 */
public interface Issue extends Serializable {

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

  @CheckForNull
  Integer line();

  /**
   * Arbitrary distance to threshold for resolving the issue.
   * <p/>
   * For examples:
   * <ul>
   *   <li>for the rule "Avoid too complex methods" : current complexity - max allowed complexity</li>
   *   <li>for the rule "Avoid Duplications" : number of duplicated blocks</li>
   *   <li>for the rule "Insufficient Line Coverage" : number of lines to cover to reach the accepted threshold</li>
   * </ul>
   */
  @CheckForNull
  Double effortToFix();

  String status();

  String resolution();

  @CheckForNull
  String userLogin();

  @CheckForNull
  String assignee();

  boolean manual();

  Date creationDate();

  Date updateDate();

  @CheckForNull
  Date closeDate();

  @CheckForNull
  String attribute(String key);

  Map<String, String> attributes();

  @CheckForNull
  String authorLogin();

  @CheckForNull
  List<ActionPlan> actionPlans();

}
