/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.issue;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;

import static java.util.Arrays.asList;

/**
 * @since 3.6
 */
public interface Issue extends Serializable {

  /**
   * Maximum number of characters in the message.
   * In theory it should be 4_000 UTF-8 characters but unfortunately
   * Oracle DB does not support more than 4_000 bytes, even if column
   * issues.message is created with type VARCHAR2(4000 CHAR).
   * In order to have the same behavior on all databases, message
   * is truncated to 4_000 / 3 (maximum bytes per UTF-8 character)
   * = 1_333 characters.
   */
  int MESSAGE_MAX_SIZE = 1_333;

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

  /**
   * Issue is irrelevant in the context and was muted by user.
   * @since 5.1
   */
  String RESOLUTION_WONT_FIX = "WONTFIX";

  List<String> RESOLUTIONS = asList(RESOLUTION_FALSE_POSITIVE, RESOLUTION_WONT_FIX, RESOLUTION_FIXED, RESOLUTION_REMOVED);

  /**
   * Return all available statuses
   *
   * @since 4.4
   */
  List<String> STATUSES = asList(STATUS_OPEN, STATUS_CONFIRMED, STATUS_REOPENED, STATUS_RESOLVED, STATUS_CLOSED);

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

  String language();

  /**
   * See constants in {@link org.sonar.api.rule.Severity}.
   */
  String severity();

  @CheckForNull
  String message();

  /**
   * Optional line number. If set, then it's greater than or equal 1.
   */
  @CheckForNull
  Integer line();

  /**
   * Arbitrary distance to threshold for resolving the issue.
   * <br>
   * For examples:
   * <ul>
   *   <li>for the rule "Avoid too complex methods" : current complexity - max allowed complexity</li>
   *   <li>for the rule "Avoid Duplications" : number of duplicated blocks</li>
   *   <li>for the rule "Insufficient Line Coverage" : number of lines to cover to reach the accepted threshold</li>
   * </ul>
   *
   * @since 5.5
   */
  @CheckForNull
  Double gap();

  /**
   * See constant values in {@link Issue}.
   */
  String status();

  /**
   * The type of resolution, or null if the issue is not resolved. See constant values in {@link Issue}.
   */
  @CheckForNull
  String resolution();

  /**
   * UUID of the user who is assigned to this issue. Null if the issue is not assigned.
   */
  @CheckForNull
  String assignee();

  Date creationDate();

  Date updateDate();

  /**
   * Date when status was set to {@link Issue#STATUS_CLOSED}, else null.
   */
  @CheckForNull
  Date closeDate();

  @CheckForNull
  String attribute(String key);

  /**
   * Empty on batch side since version 5.2. Attributes are moved to server's Compute Engine. No use-case for keeping them
   * on batch side for now
   */
  Map<String, String> attributes();

  /**
   * Login of the SCM account that introduced this issue. Requires the
   * <a href="http://www.sonarsource.com/products/plugins/developer-tools/developer-cockpit/">Developer Cockpit Plugin</a> to be installed.
   */
  @CheckForNull
  String authorLogin();

  /**
   * Non-null list of comments, ordered by chronological order.
   *
   * @deprecated since 7.2, comments are not more available
   */
  @Deprecated
  List<IssueComment> comments();

  /**
   * During a scan return if the current issue is a new one.
   * @return always false on server side
   * @since 4.0
   */
  boolean isNew();

  /**
   * During a scan returns true if the issue is copied from another branch.
   * @since 6.6
   */
  boolean isCopied();

  /**
   * @since 5.5
   */
  @CheckForNull
  Duration effort();

  /**
   * @since 5.0
   */
  String projectKey();

  /**
   * @since 5.0
   */
  String projectUuid();

  /**
   * @since 5.0
   */
  String componentUuid();

  /**
   * @since 5.1
   */
  Collection<String> tags();
}
