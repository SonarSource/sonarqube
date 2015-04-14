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
package org.sonar.batch.report;

import com.google.common.base.Charsets;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.output.BatchReportWriter;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class SourcePublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private SourcePublisher publisher;

  private File sourceFile;

  private BatchReportWriter writer;

  private org.sonar.api.resources.File sampleFile;

  @Before
  public void prepare() throws IOException {
    Project p = new Project("foo").setAnalysisDate(new Date(1234567L));
    ResourceCache resourceCache = new ResourceCache();
    sampleFile = org.sonar.api.resources.File.create("src/Foo.php");
    sampleFile.setEffectiveKey("foo:src/Foo.php");
    resourceCache.add(p, null).setSnapshot(new Snapshot().setId(2));
    File baseDir = temp.newFolder();
    sourceFile = new File(baseDir, "src/Foo.php");
    resourceCache.add(sampleFile, null).setInputPath(new DefaultInputFile("foo", "src/Foo.php").setLines(5).setModuleBaseDir(baseDir.toPath()).setCharset(Charsets.ISO_8859_1));
    publisher = new SourcePublisher(resourceCache);
    File outputDir = temp.newFolder();
    writer = new BatchReportWriter(outputDir);
  }

  @Test
  public void publishEmptySource() throws Exception {
    FileUtils.write(sourceFile, "", Charsets.ISO_8859_1);

    publisher.publish(writer);

    File out = writer.getSourceFile(2);
    assertThat(FileUtils.readFileToString(out, Charsets.UTF_8)).isEqualTo("");
  }

  @Test
  public void publishSourceWithLastEmptyLine() throws Exception {
    FileUtils.write(sourceFile, "1\n2\n3\n4\n", Charsets.ISO_8859_1);

    publisher.publish(writer);

    File out = writer.getSourceFile(2);
    assertThat(FileUtils.readFileToString(out, Charsets.UTF_8)).isEqualTo("1\n2\n3\n4\n");
  }

  @Test
  public void publishTestSource() throws Exception {
    FileUtils.write(sourceFile, "1\n2\n3\n4\n", Charsets.ISO_8859_1);
    sampleFile.setQualifier(Qualifiers.UNIT_TEST_FILE);

    publisher.publish(writer);

    File out = writer.getSourceFile(2);
    assertThat(FileUtils.readFileToString(out, Charsets.UTF_8)).isEqualTo("1\n2\n3\n4\n");
  }

  @Test
  public void publishSourceWithLastLineNotEmpty() throws Exception {
    FileUtils.write(sourceFile, "1\n2\n3\n4\n5", Charsets.ISO_8859_1);

    publisher.publish(writer);

    File out = writer.getSourceFile(2);
    assertThat(FileUtils.readFileToString(out, Charsets.UTF_8)).isEqualTo("1\n2\n3\n4\n5");
  }

  @Test
  public void cleanLineEnds() throws Exception {
    FileUtils.write(sourceFile, "\n2\r\n3\n4\r5", Charsets.ISO_8859_1);

    publisher.publish(writer);

    File out = writer.getSourceFile(2);
    assertThat(FileUtils.readFileToString(out, Charsets.UTF_8)).isEqualTo("\n2\n3\n4\n5");
  }
}
