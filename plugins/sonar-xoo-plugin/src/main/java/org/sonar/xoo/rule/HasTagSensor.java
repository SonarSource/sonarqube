/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.xoo.rule;

import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.rule.RuleKey;

/**
 * Generate issues on all the occurrences of a given tag in xoo sources.
 */
public class HasTagSensor extends AbstractDeprecatedXooRuleSensor {

  public static final String RULE_KEY = "HasTag";

  private static final String EFFORT_TO_FIX_PROPERTY = "sonar.hasTag.effortToFix";

  private final ResourcePerspectives perspectives;
  private final Settings settings;
  private final ActiveRules activeRules;

  private FileSystem fs;

  public HasTagSensor(FileSystem fs, ResourcePerspectives perspectives, Settings settings, ActiveRules activeRules) {
    super(fs, activeRules);
    this.fs = fs;
    this.perspectives = perspectives;
    this.settings = settings;
    this.activeRules = activeRules;
  }

  @Override
  protected String getRuleKey() {
    return RULE_KEY;
  }

  @Override
  protected void processFile(InputFile inputFile, org.sonar.api.resources.File sonarFile, SensorContext context, RuleKey ruleKey, String languageKey) {
    org.sonar.api.batch.rule.ActiveRule activeRule = activeRules.find(ruleKey);
    String tag = activeRule.param("tag");
    if (tag == null) {
      throw new IllegalStateException("Rule is badly configured. The parameter 'tag' is missing.");
    }
    try {
      Issuable issuable = perspectives.as(Issuable.class, sonarFile);
      List<String> lines = FileUtils.readLines(inputFile.file(), fs.encoding().name());
      for (int index = 0; index < lines.size(); index++) {
        if (lines.get(index).contains(tag)) {
          issuable.addIssue(issuable.newIssueBuilder()
            .effortToFix(settings.getDouble(EFFORT_TO_FIX_PROPERTY))
            .line(index + 1)
            .ruleKey(ruleKey)
            .build());
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Fail to process " + inputFile, e);
    }
  }
}
