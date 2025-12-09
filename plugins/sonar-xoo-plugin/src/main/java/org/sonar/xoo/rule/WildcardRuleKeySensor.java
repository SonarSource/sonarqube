/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.Set;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

/**
 * The rules in this sensor are intended to match the wildcard rule keys listed in the compliance reports metadata files.
 */
public class WildcardRuleKeySensor implements Sensor {

  public static final String RULE_KEY_S1493 = "S1493";
  public static final String RULE_KEY_S2184 = "S2184";
  public static final String RULE_KEY_S4639 = "S4639";
  public static final String RULE_KEY_S5144 = "S5144";
  public static final String RULE_KEY_S8233 = "S8233";

  private static final Set<String> ALL_RULE_KEYS = Set.of(
    RULE_KEY_S1493,
    RULE_KEY_S2184,
    RULE_KEY_S4639,
    RULE_KEY_S5144,
    RULE_KEY_S8233
  );

  private final FileSystem fs;
  private final ActiveRules activeRules;

  public WildcardRuleKeySensor(FileSystem fs, ActiveRules activeRules) {
    this.fs = fs;
    this.activeRules = activeRules;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage(Xoo.KEY)
      .createIssuesForRuleRepository(XooRulesDefinition.XOO_REPOSITORY);
  }

  @Override
  public void execute(SensorContext context) {
    for (InputFile inputFile : fs.inputFiles(fs.predicates().hasLanguage(Xoo.KEY))) {
      for (String ruleKeyFragment : ALL_RULE_KEYS) {
        var ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, ruleKeyFragment);
        if (activeRules.find(ruleKey) == null) {
          return;
        }
        NewIssue issue = context.newIssue();
        issue
          .at(issue.newLocation().on(inputFile).message("This issue is generated on each file. Severity is blocker, whatever quality profile"))
          .forRule(ruleKey)
          .overrideSeverity(Severity.BLOCKER)
          .save();
      }
    }
  }
}
