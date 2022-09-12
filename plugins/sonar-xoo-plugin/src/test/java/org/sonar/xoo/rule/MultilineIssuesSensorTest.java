/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueFlow;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueFlow.Type;
import org.sonar.api.internal.apachecommons.io.IOUtils;
import org.sonar.xoo.Xoo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MultilineIssuesSensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final ActiveRules activeRules = mock(ActiveRules.class);

  private final MultilineIssuesSensor sensor = new MultilineIssuesSensor();

  @Before
  public void before() {
    when(activeRules.find(any())).thenReturn(mock(ActiveRule.class));
  }

  @Test
  public void execute_dataAndExecutionFlowsAreDetected() throws IOException {
    DefaultInputFile inputFile = newTestFile(IOUtils.toString(getClass().getResource("dataflow.xoo"), StandardCharsets.UTF_8));

    DefaultFileSystem fs = new DefaultFileSystem(temp.newFolder());
    fs.add(inputFile);

    SensorContextTester sensorContextTester = SensorContextTester.create(fs.baseDir());
    sensorContextTester.setFileSystem(fs);
    sensor.execute(sensorContextTester);

    assertThat(sensorContextTester.allIssues()).hasSize(1);

    List<Issue.Flow> flows = sensorContextTester.allIssues().iterator().next().flows();
    assertThat(flows).hasSize(2);

    List<DefaultIssueFlow> defaultIssueFlows = flows.stream().map(DefaultIssueFlow.class::cast).collect(Collectors.toList());

    assertThat(defaultIssueFlows).extracting(DefaultIssueFlow::getType).containsExactlyInAnyOrder(Type.DATA, Type.EXECUTION);
  }

  private DefaultInputFile newTestFile(String content) {
    return new TestInputFileBuilder("foo", "src/Foo.xoo")
      .setLanguage(Xoo.KEY)
      .setType(InputFile.Type.MAIN)
      .setContents(content)
      .build();
  }
}
