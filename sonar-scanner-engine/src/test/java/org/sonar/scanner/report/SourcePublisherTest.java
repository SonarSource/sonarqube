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
package org.sonar.scanner.report;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SourcePublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private SourcePublisher publisher;
  private File sourceFile;
  private ScannerReportWriter writer;
  private DefaultInputFile inputFile;

  @Before
  public void prepare() throws IOException {
    File baseDir = temp.newFolder();
    sourceFile = new File(baseDir, "src/Foo.php");
    String moduleKey = "foo";
    inputFile = new TestInputFileBuilder(moduleKey, "src/Foo.php")
      .setLines(5)
      .setModuleBaseDir(baseDir.toPath())
      .setCharset(StandardCharsets.ISO_8859_1)
      .build();

    DefaultInputModule rootModule = TestInputFileBuilder.newDefaultInputModule(moduleKey, baseDir);
    InputComponentStore componentStore = new InputComponentStore(rootModule, mock(BranchConfiguration.class));
    componentStore.put(inputFile);

    publisher = new SourcePublisher(componentStore);
    File outputDir = temp.newFolder();
    writer = new ScannerReportWriter(outputDir);
  }

  @Test
  public void publishEmptySource() throws Exception {
    FileUtils.write(sourceFile, "", StandardCharsets.ISO_8859_1);

    publisher.publish(writer);

    File out = writer.getSourceFile(inputFile.batchId());
    assertThat(FileUtils.readFileToString(out, StandardCharsets.UTF_8)).isEqualTo("");
  }

  @Test
  public void publishSourceWithLastEmptyLine() throws Exception {
    FileUtils.write(sourceFile, "1\n2\n3\n4\n", StandardCharsets.ISO_8859_1);

    publisher.publish(writer);

    File out = writer.getSourceFile(inputFile.batchId());
    assertThat(FileUtils.readFileToString(out, StandardCharsets.UTF_8)).isEqualTo("1\n2\n3\n4\n");
  }

  @Test
  public void publishTestSource() throws Exception {
    FileUtils.write(sourceFile, "1\n2\n3\n4\n", StandardCharsets.ISO_8859_1);
    // sampleFile.setQualifier(Qualifiers.UNIT_TEST_FILE);

    publisher.publish(writer);

    File out = writer.getSourceFile(inputFile.batchId());
    assertThat(FileUtils.readFileToString(out, StandardCharsets.UTF_8)).isEqualTo("1\n2\n3\n4\n");
  }

  @Test
  public void publishSourceWithLastLineNotEmpty() throws Exception {
    FileUtils.write(sourceFile, "1\n2\n3\n4\n5", StandardCharsets.ISO_8859_1);

    publisher.publish(writer);

    File out = writer.getSourceFile(inputFile.batchId());
    assertThat(FileUtils.readFileToString(out, StandardCharsets.UTF_8)).isEqualTo("1\n2\n3\n4\n5");
  }

  @Test
  public void cleanLineEnds() throws Exception {
    FileUtils.write(sourceFile, "\n2\r\n3\n4\r5", StandardCharsets.ISO_8859_1);

    publisher.publish(writer);

    File out = writer.getSourceFile(inputFile.batchId());
    assertThat(FileUtils.readFileToString(out, StandardCharsets.UTF_8)).isEqualTo("\n2\n3\n4\n5");
  }

}
