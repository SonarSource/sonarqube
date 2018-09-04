/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.sensor.noop;

import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.rules.RuleType;

public class NoOpNewAdHocRule implements NewAdHocRule {

  @Override
  public NoOpNewAdHocRule engineId(String engineId) {
    // no op
    return this;
  }

  @Override
  public NoOpNewAdHocRule ruleId(String ruleId) {
    // no op
    return this;
  }

  @Override
  public NewAdHocRule name(String name) {
    // no op
    return this;
  }

  @Override
  public NewAdHocRule description(String description) {
    // no op
    return this;
  }

  @Override
  public NoOpNewAdHocRule type(RuleType type) {
    // no op
    return this;
  }

  @Override
  public NoOpNewAdHocRule severity(Severity severity) {
    // no op
    return this;
  }

  @Override
  public void save() {
    // no op
  }

}
