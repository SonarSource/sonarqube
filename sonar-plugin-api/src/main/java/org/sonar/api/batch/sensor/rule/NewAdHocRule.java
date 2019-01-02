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
package org.sonar.api.batch.sensor.rule;

import javax.annotation.Nullable;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.rules.RuleType;

/**
 * Builder for a rule imported from an external rule engine by a {@link Sensor}. This allows to provide more metadata
 * for rules associated to {@link org.sonar.api.batch.sensor.issue.ExternalIssue}.
 * Don't forget to {@link #save()} after setting the fields.
 * 
 * @since 7.4
 */
public interface NewAdHocRule {

  /**
   * Unique identifier of the external analyzer (e.g. eslint, pmd, ...)
   */
  NewAdHocRule engineId(String engineId);

  /**
   * Unique rule identifier for a given {@link #engineId(String)}
   */
  NewAdHocRule ruleId(String ruleId);

  /**
   * The name of the rule.
   */
  NewAdHocRule name(String name);

  /**
   * The description of the rule.
   */
  NewAdHocRule description(@Nullable String description);

  /**
   * Type of the rule.
   */
  NewAdHocRule type(RuleType type);

  /**
   * Set the severity of the rule.
   */
  NewAdHocRule severity(Severity severity);

  /**
   * Save the rule. There is almost no validation, except that no duplicated ad hoc rule keys are permitted.
   */
  void save();

}
