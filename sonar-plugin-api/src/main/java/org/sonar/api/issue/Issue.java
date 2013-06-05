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

import com.google.common.collect.ImmutableList;
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

  /**
   * Maximum number of characters in the message.
   */
  int MESSAGE_MAX_SIZE = 4000;

  /**
   * Default status when creating an issue.
   */
  String STATUS_OPEN = "OPEN";
  String STATUS_CONFIRMED = "CONFIRMED";
  String STATUS_REOPENED = "REOPENED";
  String STATUS_RESOLVED = "RESOLVED";
  String STATUS_CLOSED = "CLOSED";

  String RESOLUTION_FIXED = "FIXED";

  /**
   * Resolution when issue is flagged as false positive.
   */
  String RESOLUTION_FALSE_POSITIVE = "FALSE-POSITIVE";

  /**
   * Resolution when rule has been uninstalled or disabled in the Quality profile.
    */
  String RESOLUTION_REMOVED = "REMOVED";

  List<String> RESOLUTIONS = ImmutableList.of(RESOLUTION_FALSE_POSITIVE, RESOLUTION_FIXED, RESOLUTION_REMOVED);

  /**
   * Unique generated key. It looks like "d2de809c-1512-4ae2-9f34-f5345c9f1a13".
   */
  String key();

  /**
   * Components are modules ("my_project"), directories ("my_project:my/dir") or files ("my_project:my/file.c").
   * Keys of Java packages and classes are currently in a special format: "my_project:com.company" and "my_project:com.company.Foo".
   */
  String componentKey();

  RuleKey ruleKey();

  /**
   * See constants in {@link org.sonar.api.rule.Severity}.
   */
  String severity();

  @CheckForNull
  String message();

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

  /**
   * The type of resolution, or null if the issue is not resolved.
   */
  @CheckForNull
  String resolution();

  @CheckForNull
  String reporter();

  @CheckForNull
  String assignee();

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
  String actionPlanKey();

  /**
   * Non-null list of comments, ordered by chronological order.
   * <p/>
   * IMPORTANT: existing comments are not loaded when this method is called when analyzing project
   * (from {@link org.sonar.api.BatchExtension}).
   */
  List<IssueComment> comments();
}
