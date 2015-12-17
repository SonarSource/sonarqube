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
package org.sonar.batch.cpd;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Duplication;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.core.util.CloseableIterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaCpdEngineTest {

  private static final String JAVA = "java";
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void testJavaCodeWithTwoCloneGroupAtSameLines() throws Exception {

    File baseDir = temp.newFolder();
    DefaultFileSystem fs = new DefaultFileSystem(baseDir);
    DefaultInputFile file = new DefaultInputFile("foo", "src/ManyStatements.java").setLanguage(JAVA);
    fs.add(file);
    BatchComponentCache batchComponentCache = new BatchComponentCache();
    batchComponentCache.add(org.sonar.api.resources.File.create("src/Foo.java").setEffectiveKey("foo:src/ManyStatements.java"), null).setInputComponent(file);
    File ioFile = file.file();
    FileUtils.copyURLToFile(this.getClass().getResource("ManyStatements.java"), ioFile);

    File reportOut = temp.newFolder();
    ReportPublisher reportPublisher = mock(ReportPublisher.class);
    when(reportPublisher.getWriter()).thenReturn(new BatchReportWriter(reportOut));
    JavaCpdEngine engine = new JavaCpdEngine(fs, new Settings(), reportPublisher, batchComponentCache);
    engine.analyse(JAVA, mock(SensorContext.class));

    BatchReportReader reader = new BatchReportReader(reportOut);
    try (CloseableIterator<BatchReport.Duplication> it = reader.readComponentDuplications(1)) {
      Duplication dupGroup1 = it.next();
      Duplication dupGroup2 = it.next();

      assertThat(dupGroup1.getOriginPosition().getStartLine()).isEqualTo(6);
      assertThat(dupGroup1.getOriginPosition().getEndLine()).isEqualTo(6);
      assertThat(dupGroup1.getDuplicateCount()).isEqualTo(1);
      assertThat(dupGroup1.getDuplicate(0).getRange().getStartLine()).isEqualTo(8);
      assertThat(dupGroup1.getDuplicate(0).getRange().getEndLine()).isEqualTo(8);

      assertThat(dupGroup2.getOriginPosition().getStartLine()).isEqualTo(6);
      assertThat(dupGroup2.getOriginPosition().getEndLine()).isEqualTo(6);
      assertThat(dupGroup2.getDuplicateCount()).isEqualTo(2);
      assertThat(dupGroup2.getDuplicate(0).getRange().getStartLine()).isEqualTo(7);
      assertThat(dupGroup2.getDuplicate(0).getRange().getEndLine()).isEqualTo(7);
      assertThat(dupGroup2.getDuplicate(1).getRange().getStartLine()).isEqualTo(8);
      assertThat(dupGroup2.getDuplicate(1).getRange().getEndLine()).isEqualTo(8);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

  }

}
