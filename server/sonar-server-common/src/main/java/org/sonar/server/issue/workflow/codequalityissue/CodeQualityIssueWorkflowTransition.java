/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.issue.workflow.codequalityissue;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.server.issue.workflow.WorkflowTransition;

public enum CodeQualityIssueWorkflowTransition implements WorkflowTransition {

  /**
   * @deprecated since 10.4, use {@link #ACCEPT} instead
   */
  @Deprecated(since = "10.4")
  CONFIRM("confirm"),
  /**
   * @deprecated since 10.4. There is no replacement as CONFIRM is subject to removal in the future.
   */
  @Deprecated(since = "10.4")
  UNCONFIRM("unconfirm"),
  REOPEN("reopen"),
  RESOLVE("resolve"),
  FALSE_POSITIVE("falsepositive"),
  /**
   * @since 5.1
   * @deprecated since 10.3, use {@link #ACCEPT} instead
   */
  @Deprecated(since = "10.3")
  WONT_FIX("wontfix"),
  /**
   * @since 10.3
   */
  ACCEPT("accept");

  private static final Map<String, CodeQualityIssueWorkflowTransition> KEY_TO_ENUM;

  // Static block to populate the Map
  static {
    KEY_TO_ENUM = Stream.of(values())
      .collect(Collectors.toMap(CodeQualityIssueWorkflowTransition::getKey, transition -> transition));
  }

  private final String key;

  CodeQualityIssueWorkflowTransition(String key) {
    this.key = key;
  }

  @Override
  public String getKey() {
    return key;
  }

  public static Optional<CodeQualityIssueWorkflowTransition> fromKey(String value) {
    return Optional.ofNullable(KEY_TO_ENUM.get(value));
  }

  @Override
  public String toString() {
    return key;
  }
}
