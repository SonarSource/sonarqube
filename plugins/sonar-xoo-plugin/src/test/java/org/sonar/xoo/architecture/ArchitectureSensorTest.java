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
package org.sonar.xoo.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.xoo.Xoo;

public class ArchitectureSensorTest {

  @Test
  public void whenDescribeCalled_thenNameAndLanguageAreSet() {
    // given
    SensorDescriptor descriptor = mock(SensorDescriptor.class);
    when(descriptor.name(anyString())).thenReturn(descriptor);
    when(descriptor.onlyOnLanguage(anyString())).thenReturn(descriptor);

    ArchitectureSensor sensor = new ArchitectureSensor();

    // when
    sensor.describe(descriptor);

    // then
    verify(descriptor).onlyOnLanguage(Xoo.KEY);
    verify(descriptor).name(anyString());
  }

  @Test
  public void whenExecuteCalled_thenArchitectureDataIsSaved() {
    // given
    final int nbFileSensor = 5;
    SensorContext context = mock(SensorContext.class, RETURNS_DEEP_STUBS);

    when(context.fileSystem().inputFiles(any())).thenReturn(
      Stream.generate(() -> mock(InputFile.class)).limit(nbFileSensor).toList()
    );

    ArchitectureSensor sensor = new ArchitectureSensor();

    // when
    sensor.execute(context);

    // then
    ArgumentCaptor<InputStream> inputStreamCaptor = ArgumentCaptor.forClass(InputStream.class);
    verify(context).addAnalysisData(eq("architecture.graph.xoo.file_graph"), contains("application/graph+json"), inputStreamCaptor.capture());
    try {
      String capturedData = new String(inputStreamCaptor.getValue().readAllBytes(), StandardCharsets.UTF_8);
      assertThat(capturedData).contains("\"fileCount\":" + nbFileSensor);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
