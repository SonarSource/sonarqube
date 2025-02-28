/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.StreamSupport;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.sonar.xoo.Xoo;

public class ArchitectureSensor implements ProjectSensor {
  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("architecture-xoo-sensor")
      .onlyOnLanguage(Xoo.KEY);
  }

  @Override
  public void execute(SensorContext context) {
    final String mimeType = "application/file-graph+json;version=1.0;source=xoo";

    long count = StreamSupport.stream(
        context.fileSystem().inputFiles(
          context.fileSystem().predicates().hasLanguage(Xoo.KEY)).spliterator(), false)
      .count();

    context.addAnalysisData(
      Xoo.NAME + ".class_file_graph",
      mimeType,
      new ByteArrayInputStream(("{graph:\"data\", \"classCount\":" + count + "}")
        .getBytes(StandardCharsets.UTF_8))
    );

    context.addAnalysisData(
      Xoo.NAME + ".file_graph",
      mimeType,
      new ByteArrayInputStream(("{graph:\"data\", \"fileCount\":" + count + "}")
        .getBytes(StandardCharsets.UTF_8))
    );
  }
}
