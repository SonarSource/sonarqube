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
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;

public class NoOpNewExternalIssue implements NewExternalIssue {

  @Override
  public NewExternalIssue forRule(RuleKey ruleKey) {
    // no op
    return this;
  }

  @Override
  public NewExternalIssue type(RuleType type) {
    // no op
    return this;
  }

  @Override
  public NewExternalIssue remediationEffortMinutes(Long effort) {
    // no op
    return this;
  }

  @Override
  public NewExternalIssue severity(Severity severity) {
    // no op
    return this;
  }

  @Override
  public NewExternalIssue at(NewIssueLocation primaryLocation) {
    // no op
    return this;
  }

  @Override
  public NewExternalIssue addLocation(NewIssueLocation secondaryLocation) {
    // no op
    return this;
  }

  @Override
  public NewExternalIssue addFlow(Iterable<NewIssueLocation> flowLocations) {
    // no op
    return this;
  }

  @Override
  public NewIssueLocation newLocation() {
    // no op
    return new DefaultIssueLocation();
  }

  @Override
  public void save() {
    // no op
  }

}
