/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;

public class CustomMessageSensor extends AbstractXooRuleSensor {

  public static final String RULE_KEY = "CustomMessage";

  private static final String MESSAGE_PROPERTY = "sonar.customMessage.message";

  private final Configuration settings;

  public CustomMessageSensor(Configuration settings, FileSystem fs, ActiveRules activeRules) {
    super(fs, activeRules);
    this.settings = settings;
  }

  @Override
  protected String getRuleKey() {
    return RULE_KEY;
  }

  @Override protected void processFile(InputFile inputFile, SensorContext context, RuleKey ruleKey, String languageKey) {
    NewIssue newIssue = context.newIssue()
      .forRule(ruleKey);

    NewIssueLocation location = newIssue.newLocation().on(inputFile);
    settings.get(MESSAGE_PROPERTY).ifPresent(location::message);

    newIssue
      .at(location)
      .save();
  }
}
