/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.xoo.rule;

import org.sonar.api.rule.Severity;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.xoo.Xoo;

public class XooSonarWayProfile implements BuiltInQualityProfilesDefinition {
  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile qProfile = context.createBuiltInQualityProfile("Sonar way", Xoo.KEY);
    qProfile.activateRule(XooRulesDefinition.XOO_REPOSITORY, HasTagSensor.RULE_KEY).overrideSeverity(Severity.MAJOR);
    qProfile.activateRule(XooRulesDefinition.XOO_REPOSITORY, OneIssuePerLineSensor.RULE_KEY).overrideSeverity(Severity.INFO);
    qProfile.activateRule(XooRulesDefinition.XOO_REPOSITORY, OneIssuePerFileSensor.RULE_KEY).overrideSeverity(Severity.CRITICAL);
    qProfile.activateRule(XooRulesDefinition.XOO_REPOSITORY, HotspotSensor.RULE_KEY).overrideSeverity(Severity.CRITICAL);
    qProfile.done();
  }
}
