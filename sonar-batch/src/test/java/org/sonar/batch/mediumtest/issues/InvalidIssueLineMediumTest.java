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
package org.sonar.batch.mediumtest.issues;

import org.junit.Rule;

import org.junit.rules.ExpectedException;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.protocol.input.ActiveRule;
import org.sonar.xoo.XooPlugin;

public class InvalidIssueLineMediumTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  
  @Rule
  public ExpectedException exception = ExpectedException.none();

  public BatchMediumTester tester = BatchMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .activateRule(new ActiveRule("xoo", "InvalidIssueLine", null, "Invalid issue line", "MAJOR", "InvalidIssueLine.internal", "xoo"))
    .build();

  @Before
  public void prepare() {
    tester.start();
  }

  @After
  public void stop() {
    tester.stop();
  }

  @Test
  public void testInvalidIssueLine() throws Exception {
    File projectDir = new File(IssuesMediumTest.class.getResource("/mediumtest/xoo/sample").toURI());

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Invalid line 18 (must be <= 8) in issue for 'sample:xources/hello/HelloJava.xoo' created by the rule 'xoo:InvalidIssueLine'");
    tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .start();
  }
}
