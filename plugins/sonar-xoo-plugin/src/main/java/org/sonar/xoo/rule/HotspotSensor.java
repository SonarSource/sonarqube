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
package org.sonar.xoo.rule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;

/**
 * Generate issues on all the occurrences of tag Hotspot in xoo sources.
 */
public class HotspotSensor extends AbstractXooRuleSensor {

  public static final String RULE_KEY = "Hotspot";
  public static final String TAG = "HOTSPOT";

  public HotspotSensor(FileSystem fs, ActiveRules activeRules) {
    super(fs, activeRules);
  }

  @Override
  protected String getRuleKey() {
    return RULE_KEY;
  }

  @Override
  protected void processFile(InputFile inputFile, SensorContext context, RuleKey ruleKey, String languageKey) {
    try {
      int[] lineCounter = {1};
      try (InputStreamReader isr = new InputStreamReader(inputFile.inputStream(), inputFile.charset());
        BufferedReader reader = new BufferedReader(isr)) {
        reader.lines().forEachOrdered(lineStr -> {
          int startIndex = -1;
          while ((startIndex = lineStr.indexOf(TAG, startIndex + 1)) != -1) {
            NewIssue newIssue = context.newIssue();
            newIssue
              .forRule(ruleKey)
              .at(newIssue.newLocation()
                .on(inputFile)
                .at(inputFile.newRange(lineCounter[0], startIndex, lineCounter[0], startIndex + TAG.length())))
              .save();
          }
          lineCounter[0]++;
        });
      }
    } catch (IOException e) {
      throw new IllegalStateException("Fail to process " + inputFile, e);
    }
  }
}
