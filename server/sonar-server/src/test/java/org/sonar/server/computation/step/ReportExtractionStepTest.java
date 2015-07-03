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
package org.sonar.server.computation.step;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.internal.JUnitTempFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.compute.AnalysisReportDto;
import org.sonar.server.computation.ReportQueue;
import org.sonar.server.computation.batch.MutableBatchReportDirectoryHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ReportExtractionStepTest {

  @Rule
  public JUnitTempFolder tempFolder = new JUnitTempFolder();
  @Rule
  public LogTester logTester = new LogTester().setLevel(LoggerLevel.INFO);

  private MutableBatchReportDirectoryHolder reportDirectoryHolder = mock(MutableBatchReportDirectoryHolder.class);
  private AnalysisReportDto dto = newDefaultReport();
  private ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);

  @Test
  public void fail_if_corrupted_zip() throws Exception {
    File zip = tempFolder.newFile();
    FileUtils.write(zip, "not a file");

    ReportExtractionStep underTest = new ReportExtractionStep(new ReportQueue.Item(dto, zip), tempFolder, reportDirectoryHolder);

    try {
      underTest.execute();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).startsWith("Fail to unzip " + zip.getAbsolutePath() + " into ");
    }
    verifyNoMoreInteractions(reportDirectoryHolder);
  }

  @Test
  public void verify_zip_decompression() throws URISyntaxException, IOException {
    new ReportExtractionStep(new ReportQueue.Item(dto, demoZipFile()), tempFolder, reportDirectoryHolder).execute();

    verify(reportDirectoryHolder).setDirectory(fileCaptor.capture());
    verifyNoMoreInteractions(reportDirectoryHolder);

    File createDir = fileCaptor.getValue();
    assertThat(createDir.exists()).isTrue();
    assertThat(createDir.isDirectory()).isTrue();
    verifyFile(createDir, "1.txt", "1\n");
    verifyFile(createDir, "2.txt", "2\n");
    File subDir1 = verifyDir(createDir, "subdir1");
    verifyFile(subDir1, "3.txt", "3\n");
    verifyFile(subDir1, "4.txt", "4\n");
    File subDir2 = verifyDir(createDir, "subdir2");
    verifyFile(subDir2, "5.txt", "5\n");
    File subdir3 = verifyDir(subDir2, "subdir3");
    verifyFile(subdir3, "6.txt", "6\n");
  }

  @Test
  public void verify_show_log_at_DEBUG_level() throws URISyntaxException {
    logTester.setLevel(LoggerLevel.DEBUG);

    new ReportExtractionStep(new ReportQueue.Item(dto, demoZipFile()), tempFolder, reportDirectoryHolder).execute();

    List<String> logs = logTester.logs();
    assertThat(logs).hasSize(1);
    String log = logs.get(0);
    assertThat(log.startsWith("Report extracted | size=")).isTrue();
    assertThat(log.contains(" | project=P1 | time=")).isTrue();
  }

  private File demoZipFile() throws URISyntaxException {
    return new File(getClass().getResource(getClass().getSimpleName() + "/" + "demozip.zip").toURI());
  }

  @Test
  public void no_log_at_INFO_level() throws URISyntaxException {
    logTester.setLevel(LoggerLevel.INFO);

    new ReportExtractionStep(new ReportQueue.Item(dto, demoZipFile()), tempFolder, reportDirectoryHolder).execute();

    assertThat(logTester.logs()).isEmpty();
  }

  private File verifyDir(File dir, String subDir) {
    File file = new File(dir, subDir);
    assertThat(file.exists()).isTrue();
    assertThat(file.isDirectory()).isTrue();
    return file;
  }

  private void verifyFile(File dir, String filename, String content) throws IOException {
    File file = new File(dir, filename);
    assertThat(file.exists()).isTrue();
    assertThat(file.isDirectory()).isFalse();
    assertThat(IOUtils.toString(new FileInputStream(file), "UTF-8")).isEqualTo(content);
  }

  private static AnalysisReportDto newDefaultReport() {
    return AnalysisReportDto.newForTests(1L).setProjectKey("P1").setUuid("U1").setStatus(AnalysisReportDto.Status.PENDING);
  }
}
