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
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.batch.bootstrap.DryRun;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.java.api.JavaClass;

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
  Resource resource =  JavaClass.create("KEY");
  Violation violation = mock(Violation.class);

  @Before
  public void setUp() {
    dryRunExporter = spy(new DryRunExporter(dryRun, sonarIndex));
  }

  @Test
  public void should_disable_if_no_dry_run() {
    dryRunExporter.execute(sensorContext);

    verifyZeroInteractions(sensorContext, sonarIndex);
  }

  @Test
  public void should_export_violations() {
    when(dryRun.isEnabled()).thenReturn(true);
    when(violation.getResource()).thenReturn(resource);
    when(violation.getLineId()).thenReturn(1);
    when(violation.getMessage()).thenReturn("VIOLATION");
    doReturn(Arrays.asList(violation)).when(dryRunExporter).getViolations(resource);

    String json = dryRunExporter.getResultsAsJson(ImmutableSet.of(resource));

    assertThat(json).isEqualTo("[{\"resource\":\"KEY\",\"line\":1,\"message\":\"VIOLATION\"}]");
  }
}
