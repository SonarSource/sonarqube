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
package org.sonar.server.qualityprofile.builtin.sonarwayvariants;

import java.util.Map;
import java.util.Set;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;

/**
 * Content of the "Sonar way essentials" variant, derived from "Sonar way" by
 * {@code org.sonar.server.qualityprofile.builtin.SonarWayVariants}. This class holds only the selection criteria
 * (data); the derivation mechanism itself lives in that class.
 */
public final class SonarWayEssentialsProfileDefinition {
  public static final String NAME = "Sonar way essentials";

  /**
   * Minimum impact severity, per software quality, for a rule to be kept in "Sonar way essentials". A rule is kept
   * if any one of its impacts meets its quality's threshold; rules with no recorded impact are dropped.
   * Security has no real floor ({@link Severity#INFO}, the lowest severity) since every Security impact is kept
   * regardless of severity.
   */
  public static final Map<SoftwareQuality, Severity> MIN_IMPACT_SEVERITY = Map.of(
    SoftwareQuality.SECURITY, Severity.INFO,
    SoftwareQuality.RELIABILITY, Severity.MEDIUM,
    SoftwareQuality.MAINTAINABILITY, Severity.BLOCKER);

  /**
   * Rule keys forced into "Sonar way essentials" regardless of {@link #MIN_IMPACT_SEVERITY}. A key only has an
   * effect if the rule is already active in "Sonar way" for that language.
   */
  public static final Set<RuleKey> FORCE_INCLUDED_RULE_KEYS = Set.of();

  /**
   * Rule keys always dropped from "Sonar way essentials", even if they meet {@link #MIN_IMPACT_SEVERITY}.
   */
  public static final Set<RuleKey> FORCE_EXCLUDED_RULE_KEYS = Set.of();

  private SonarWayEssentialsProfileDefinition() {
    // constants only
  }
}
