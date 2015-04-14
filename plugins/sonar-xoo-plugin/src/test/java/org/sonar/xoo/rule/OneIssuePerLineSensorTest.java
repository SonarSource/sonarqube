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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.config.Settings;
import org.sonar.xoo.Xoo;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OneIssuePerLineSensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private OneIssuePerLineSensor sensor = new OneIssuePerLineSensor();

  @Test
  public void testDescriptor() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    assertThat(descriptor.ruleRepositories()).containsOnly(XooRulesDefinition.XOO_REPOSITORY);
  }

  @Test
  public void testRule() throws IOException {
    DefaultFileSystem fs = new DefaultFileSystem(temp.newFolder().toPath());
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/Foo.xoo").setLanguage(Xoo.KEY).setLines(10);
    fs.add(inputFile);

    SensorContext context = mock(SensorContext.class);
    final SensorStorage sensorStorage = mock(SensorStorage.class);
    when(context.settings()).thenReturn(new Settings());
    when(context.fileSystem()).thenReturn(fs);
    when(context.newIssue()).thenAnswer(new Answer<Issue>() {
      @Override
      public Issue answer(InvocationOnMock invocation) throws Throwable {
        return new DefaultIssue(sensorStorage);
      }
    });
    sensor.execute(context);

    ArgumentCaptor<DefaultIssue> argCaptor = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(sensorStorage, times(10)).store(argCaptor.capture());
    assertThat(argCaptor.getAllValues()).hasSize(10); // One issue per line
    assertThat(argCaptor.getValue().overridenSeverity()).isNull();
  }

  @Test
  public void testForceSeverity() throws IOException {
    DefaultFileSystem fs = new DefaultFileSystem(temp.newFolder().toPath());
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/Foo.xoo").setLanguage(Xoo.KEY).setLines(10);
    fs.add(inputFile);

    SensorContext context = mock(SensorContext.class);
    final SensorStorage sensorStorage = mock(SensorStorage.class);
    Settings settings = new Settings();
    settings.setProperty(OneIssuePerLineSensor.FORCE_SEVERITY_PROPERTY, "MINOR");
    when(context.settings()).thenReturn(settings);
    when(context.fileSystem()).thenReturn(fs);
    when(context.newIssue()).thenAnswer(new Answer<Issue>() {
      @Override
      public Issue answer(InvocationOnMock invocation) throws Throwable {
        return new DefaultIssue(sensorStorage);
      }
    });
    sensor.execute(context);

    ArgumentCaptor<DefaultIssue> argCaptor = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(sensorStorage, times(10)).store(argCaptor.capture());
    assertThat(argCaptor.getAllValues()).hasSize(10); // One issue per line
    assertThat(argCaptor.getValue().overridenSeverity()).isEqualTo(Severity.MINOR);
  }

}
