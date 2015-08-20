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
package org.sonar.batch.mediumtest.cache;

import org.sonar.batch.protocol.input.FileData;

import org.junit.Test;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.sonar.api.CoreProperties;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.protocol.input.ActiveRule;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import java.util.Date;

public class CacheSyncTest {

  public BatchMediumTester tester;

  @After
  public void stop() {
    if (tester != null) {
      tester.stop();
      tester = null;
    }
  }

  @Test
  public void testSyncFirstTime() {
    FileData file1 = new FileData("hash", true);
    String[] hashes = new String[] {
      "line1", "line2"
    };

    tester = BatchMediumTester.builder()
      .bootstrapProperties(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ISSUES))
      .registerPlugin("xoo", new XooPlugin())
      .addRules(new XooRulesDefinition())
      .activateRule(new ActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", "my/internal/key", "xoo"))
      .setPreviousAnalysisDate(new Date())
      .addFileData("test-project", "file1", file1)
      .mockLineHashes("test-project:file1", hashes)
      .build();

    tester.start();
    tester.syncProject("test-project");

  }

}
