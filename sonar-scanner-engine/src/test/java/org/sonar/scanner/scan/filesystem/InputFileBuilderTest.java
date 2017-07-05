/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.scan.filesystem.PathResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class InputFileBuilderTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private Path baseDir;
  private InputFileBuilder builder;

  @Before
  public void setUp() throws IOException {
    baseDir = temp.newFolder().toPath();
    DefaultInputModule module = new DefaultInputModule(ProjectDefinition.create()
      .setKey("module1")
      .setBaseDir(baseDir.toFile()), 0);

    PathResolver pathResolver = new PathResolver();
    LanguageDetection langDetection = mock(LanguageDetection.class);
    MetadataGenerator metadataGenerator = mock(MetadataGenerator.class);
    BatchIdGenerator idGenerator = new BatchIdGenerator();
    MapSettings settings = new MapSettings();
    builder = new InputFileBuilder(module, pathResolver, langDetection, metadataGenerator, idGenerator, settings.asConfig());
  }

  @Test
  public void testBuild() {
    Path filePath = baseDir.resolve("src/File1.xoo");
    DefaultInputFile inputFile = builder.create(filePath, Type.MAIN, StandardCharsets.UTF_8);

    assertThat(inputFile.moduleKey()).isEqualTo("module1");
    assertThat(inputFile.absolutePath()).isEqualTo(filePath.toString().replaceAll("\\\\", "/"));
    assertThat(inputFile.key()).isEqualTo("module1:src/File1.xoo");
    assertThat(inputFile.publish()).isFalse();
  }
}
