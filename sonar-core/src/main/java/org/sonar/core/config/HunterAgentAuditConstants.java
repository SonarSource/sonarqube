/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.core.config;

public final class HunterAgentAuditConstants {
  /**
   * Originates from {@code HunterAgentProperties#HUNTER_AGENT_ENABLED_PROPERTY} in the
   * sonarqube-unification repository. There is no code dependency between the repositories, so the key
   * is duplicated across them by design — same as {@link RemediationAgentAuditConstants}.
   *
   * <p>Within this repository it is the single source of truth: every consumer imports it from here
   * rather than holding its own literal, so a rename cannot silently drop the key out of
   * {@code AuditPersisterImpl#TRACKED_PROPERTIES}.
   */
  public static final String HUNTER_AGENT_ENABLED_PROPERTY = "sonar.ai.hunterAgent.enabled";

  private HunterAgentAuditConstants() {
  }

}
