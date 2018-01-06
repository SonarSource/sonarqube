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
package org.sonar.scanner.cpd.deprecated;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.duplications.block.Block;
import org.sonar.scanner.cpd.index.SonarCpdBlockIndex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class JavaCpdBlockIndexerTest {
  private static final String JAVA = "java";

  @Mock
  private SonarCpdBlockIndex index;

  @Captor
  private ArgumentCaptor<List<Block>> blockCaptor;

  private MapSettings settings;
  private JavaCpdBlockIndexer engine;
  private InputFile file;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);

    File baseDir = temp.newFolder();
    DefaultFileSystem fs = new DefaultFileSystem(baseDir);
    file = new TestInputFileBuilder("foo", "src/ManyStatements.java")
      .setModuleBaseDir(baseDir.toPath())
      .setCharset(StandardCharsets.UTF_8)
      .setLanguage(JAVA).build();
    fs.add(file);
    File ioFile = file.file();
    FileUtils.copyURLToFile(this.getClass().getResource("ManyStatements.java"), ioFile);

    settings = new MapSettings();
    engine = new JavaCpdBlockIndexer(fs, settings.asConfig(), index);
  }

  @Test
  public void languageSupported() {
    JavaCpdBlockIndexer engine = new JavaCpdBlockIndexer(mock(FileSystem.class), new MapSettings().asConfig(), index);
    assertThat(engine.isLanguageSupported(JAVA)).isTrue();
    assertThat(engine.isLanguageSupported("php")).isFalse();
  }

  @Test
  public void testExclusions() {
    settings.setProperty(CoreProperties.CPD_EXCLUSIONS, "**");
    engine.index(JAVA);
    verifyZeroInteractions(index);
  }

  @Test
  public void testJavaIndexing() throws Exception {
    engine.index(JAVA);

    verify(index).insert(eq(file), blockCaptor.capture());
    List<Block> blockList = blockCaptor.getValue();

    assertThat(blockList).hasSize(26);
  }
}
