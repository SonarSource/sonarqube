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
package org.sonar.server.issue.workflow.securityhotspot;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.server.issue.workflow.WorkflowTransition;

public enum SecurityHotspotWorkflowTransition implements WorkflowTransition {
  /**
   * @deprecated since 8.1, transition has no effect
   */
  @Deprecated
  SET_AS_IN_REVIEW("setinreview"),

  /**
   * @since 7.8
   * @deprecated since 8.1, security hotspots can no longer be converted to vulnerabilities
   */
  @Deprecated
  OPEN_AS_VULNERABILITY("openasvulnerability"),
  RESOLVE_AS_REVIEWED("resolveasreviewed"),
  RESOLVE_AS_SAFE("resolveassafe"),
  RESOLVE_AS_ACKNOWLEDGED("resolveasacknowledged"),
  RESET_AS_TO_REVIEW("resetastoreview");

  private static final Map<String, SecurityHotspotWorkflowTransition> KEY_TO_ENUM;

  // Static block to populate the Map
  static {
    KEY_TO_ENUM = Stream.of(values())
      .collect(Collectors.toMap(SecurityHotspotWorkflowTransition::getKey, transition -> transition));
  }

  private final String key;

  SecurityHotspotWorkflowTransition(String key) {
    this.key = key;
  }

  @Override
  public String getKey() {
    return key;
  }

  public static Optional<SecurityHotspotWorkflowTransition> fromKey(String value) {
    return Optional.ofNullable(KEY_TO_ENUM.get(value));
  }

  @Override
  public String toString() {
    return key;
  }
}
