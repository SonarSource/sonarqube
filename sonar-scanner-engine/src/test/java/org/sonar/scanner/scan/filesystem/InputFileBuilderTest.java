/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.scan.filesystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.SensorStrategy;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.scanner.scan.DefaultInputModuleHierarchy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InputFileBuilderTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private Path baseDir;
  private Path workDir;
  private InputFileBuilder builder;

  private SensorStrategy sensorStrategy;

  @Before
  public void setUp() throws IOException {
    baseDir = temp.newFolder().toPath();
    workDir = temp.newFolder().toPath();
    DefaultInputModule root = new DefaultInputModule(ProjectDefinition.create()
      .setBaseDir(baseDir.toFile())
      .setWorkDir(workDir.toFile())
      .setKey("root"), 0);
    Path moduleBaseDir = baseDir.resolve("module1");
    Files.createDirectories(moduleBaseDir);
    DefaultInputModule module = new DefaultInputModule(ProjectDefinition.create()
      .setBaseDir(moduleBaseDir.toFile())
      .setWorkDir(workDir.toFile())
      .setKey("module1"), 0);

    MetadataGenerator metadataGenerator = mock(MetadataGenerator.class);
    BatchIdGenerator idGenerator = new BatchIdGenerator();
    MapSettings settings = new MapSettings();
    ModuleFileSystemInitializer moduleFileSystemInitializer = mock(ModuleFileSystemInitializer.class);
    when(moduleFileSystemInitializer.defaultEncoding()).thenReturn(StandardCharsets.UTF_8);
    sensorStrategy = new SensorStrategy();
    builder = new InputFileBuilder(module, metadataGenerator, idGenerator, settings.asConfig(), moduleFileSystemInitializer, new DefaultInputModuleHierarchy(root),
      sensorStrategy);
  }

  @Test
  public void testBuild() {
    Path filePath = baseDir.resolve("module1/src/File1.xoo");
    DefaultInputFile inputFile = builder.create(Type.MAIN, filePath, null);

    assertThat(inputFile.moduleKey()).isEqualTo("module1");
    assertThat(inputFile.absolutePath()).isEqualTo(filePath.toString().replaceAll("\\\\", "/"));
    assertThat(inputFile.relativePath()).isEqualTo("src/File1.xoo");
    assertThat(inputFile.path()).isEqualTo(filePath);
    assertThat(inputFile.key()).isEqualTo("module1:src/File1.xoo");
    assertThat(inputFile.isPublished()).isFalse();

    sensorStrategy.setGlobal(true);

    assertThat(inputFile.relativePath()).isEqualTo("module1/src/File1.xoo");
  }
}
