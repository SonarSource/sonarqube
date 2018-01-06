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
package org.sonar.xoo.lang;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.sourceforge.pmd.cpd.SourceCode;
import net.sourceforge.pmd.cpd.TokenEntry;
import net.sourceforge.pmd.cpd.Tokens;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.duplications.cpd.FileCodeLoaderWithoutCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XooTokenizerTest {

  private SensorContext context = mock(SensorContext.class);
  private DefaultFileSystem fileSystem;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private File baseDir;
  private Settings settings;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    fileSystem = new DefaultFileSystem(baseDir.toPath());
    when(context.fileSystem()).thenReturn(fileSystem);
    settings = new MapSettings();
    when(context.settings()).thenReturn(settings);
  }

  @Test
  public void testExecution() throws IOException {
    File source = new File(baseDir, "src/foo.xoo");
    FileUtils.write(source, "token1 token2 token3\ntoken4", StandardCharsets.UTF_8);
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo")
      .setLanguage("xoo")
      .setModuleBaseDir(baseDir.toPath())
      .setCharset(StandardCharsets.UTF_8)
      .build();
    fileSystem.add(inputFile);

    XooTokenizer tokenizer = new XooTokenizer(fileSystem);
    Tokens cpdTokens = new Tokens();
    try (InputStreamReader reader = new InputStreamReader(inputFile.inputStream(), inputFile.charset())) {
      SourceCode sourceCode = new SourceCode(new FileCodeLoaderWithoutCache(inputFile.absolutePath(), reader));
      tokenizer.tokenize(sourceCode, cpdTokens);
    }

    // 4 tokens + EOF
    assertThat(cpdTokens.getTokens()).hasSize(5);
    assertThat(cpdTokens.getTokens().get(3)).isEqualTo(new TokenEntry("token4", "src/foo.xoo", 2));
  }
}
