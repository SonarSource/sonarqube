/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.local;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.platform.Server;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.batch.bootstrap.DryRun;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.java.api.JavaClass;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DryRunExporterTest {
  DryRunExporter dryRunExporter;

  DryRun dryRun = mock(DryRun.class);
  DefaultIndex sonarIndex = mock(DefaultIndex.class);
  SensorContext sensorContext = mock(SensorContext.class);
  Resource resource = JavaClass.create("KEY");
  Violation violation = mock(Violation.class);
  ProjectFileSystem projectFileSystem = mock(ProjectFileSystem.class);
  Server server = mock(Server.class);

  @org.junit.Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() {
    dryRunExporter = spy(new DryRunExporter(dryRun, sonarIndex, projectFileSystem, server));
  }

  @Test
  public void should_disable_if_no_dry_run() {
    dryRunExporter.execute(sensorContext);

    verifyZeroInteractions(sensorContext, sonarIndex);
  }

  @Test
  public void should_export_violations() {
    when(dryRun.isEnabled()).thenReturn(true);
    when(server.getVersion()).thenReturn("3.4");
    when(violation.getResource()).thenReturn(resource);
    when(violation.getLineId()).thenReturn(1);
    when(violation.getMessage()).thenReturn("VIOLATION");
    when(violation.getRule()).thenReturn(Rule.create("pmd", "RULE_KEY").setName("RULE_NAME"));
    when(violation.getSeverity()).thenReturn(RulePriority.INFO);
    doReturn(Arrays.asList(violation)).when(dryRunExporter).getViolations(resource);

    StringWriter output = new StringWriter();
    dryRunExporter.writeJson(ImmutableSet.of(resource), output);
    String json = output.toString();

    assertThat(json)
        .isEqualTo(
            "{\"version\":\"3.4\",\"violations_per_resource\":{\"KEY\":[{\"line\":1,\"message\":\"VIOLATION\",\"severity\":\"INFO\",\"rule_key\":\"RULE_KEY\",\"rule_name\":\"RULE_NAME\"}]}}");
  }

  @Test
  public void should_export_violation_with_no_line() {
    when(dryRun.isEnabled()).thenReturn(true);
    when(server.getVersion()).thenReturn("3.4");
    when(violation.getResource()).thenReturn(resource);
    when(violation.getLineId()).thenReturn(null);
    when(violation.getMessage()).thenReturn("VIOLATION");
    when(violation.getRule()).thenReturn(Rule.create("pmd", "RULE_KEY").setName("RULE_NAME"));
    when(violation.getSeverity()).thenReturn(RulePriority.INFO);
    doReturn(Arrays.asList(violation)).when(dryRunExporter).getViolations(resource);

    StringWriter output = new StringWriter();
    dryRunExporter.writeJson(ImmutableSet.of(resource), output);
    String json = output.toString();

    assertThat(json).isEqualTo(
        "{\"version\":\"3.4\",\"violations_per_resource\":{\"KEY\":[{\"message\":\"VIOLATION\",\"severity\":\"INFO\",\"rule_key\":\"RULE_KEY\",\"rule_name\":\"RULE_NAME\"}]}}");
  }

  @Test
  public void should_ignore_resources_without_violations() {
    when(dryRun.isEnabled()).thenReturn(true);
    when(server.getVersion()).thenReturn("3.4");
    doReturn(Arrays.<Violation> asList()).when(dryRunExporter).getViolations(resource);

    StringWriter output = new StringWriter();
    dryRunExporter.writeJson(ImmutableSet.of(resource), output);
    String json = output.toString();

    assertThat(json).isEqualTo("{\"version\":\"3.4\",\"violations_per_resource\":{}}");
  }

  @Test
  public void should_export_violations_to_file() throws IOException {
    File sonarDirectory = temporaryFolder.newFolder("sonar");
    when(dryRun.isEnabled()).thenReturn(true);
    when(dryRun.isEnabled()).thenReturn(true);
    when(server.getVersion()).thenReturn("3.4");
    doReturn(Arrays.<Violation> asList()).when(dryRunExporter).getViolations(resource);
    when(dryRun.getExportPath()).thenReturn("output.json");
    when(projectFileSystem.getSonarWorkingDirectory()).thenReturn(sonarDirectory);

    dryRunExporter.execute(sensorContext);

    assertThat(new File(sonarDirectory, "output.json")).exists();
  }
}
