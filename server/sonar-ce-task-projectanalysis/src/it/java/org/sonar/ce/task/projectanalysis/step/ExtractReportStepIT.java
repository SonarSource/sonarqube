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
package org.sonar.ce.task.projectanalysis.step;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.impl.utils.JUnitTempFolder;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ZipUtils;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.projectanalysis.scanner.MutableScannerReportDirectoryHolder;
import org.sonar.ce.task.projectanalysis.scanner.ScannerReportDirectoryHolderImpl;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeTaskTypes;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExtractReportStepIT {

  private static final String TASK_UUID = "1";
  private static final String CONTENT = "{metadata}";

  @Rule
  public JUnitTempFolder tempFolder = new JUnitTempFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final MutableScannerReportDirectoryHolder reportDirectoryHolder = new ScannerReportDirectoryHolderImpl();
  private CeTask ceTask = new CeTask.Builder()
    .setType(CeTaskTypes.REPORT)
    .setUuid(TASK_UUID)
    .build();

  private ExtractReportStep underTest = new ExtractReportStep(dbTester.getDbClient(), ceTask, tempFolder, reportDirectoryHolder);

  @Test
  public void fail_if_report_zip_does_not_exist() {
    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(MessageException.class)
      .hasMessage("Analysis report 1 part 1 is missing in database");
  }

  @Test
  public void unzip_report() throws Exception {
    logTester.setLevel(Level.DEBUG);
    File reportFile = generateReport();
    try (InputStream input = FileUtils.openInputStream(reportFile)) {
      dbTester.getDbClient().ceTaskInputDao().insert(dbTester.getSession(), TASK_UUID, 1, input);
    }
    dbTester.getSession().commit();
    dbTester.getSession().close();

    underTest.execute(new TestComputationStepContext());

    // directory contains the uncompressed report (which contains only metadata.pb in this test)
    File unzippedDir = reportDirectoryHolder.getDirectory();
    assertThat(unzippedDir).isDirectory().exists();
    assertThat(unzippedDir.listFiles()).hasSize(1);
    assertThat(new File(unzippedDir, "metadata.pb")).hasContent(CONTENT);

    assertThat(logTester.logs(Level.DEBUG)).anyMatch(log -> log.matches("Analysis report is \\d+ bytes uncompressed"));
  }

  @Test
  public void unzip_report_in_several_parts() throws Exception {
    logTester.setLevel(Level.DEBUG);
    File reportFile = generateReport();
    List<File> parts = splitFile(reportFile);
    assertThat(parts).hasSizeGreaterThan(1);

    for (int i = 0; i < parts.size(); i++) {
      File f = parts.get(i);
      try (InputStream input = FileUtils.openInputStream(f)) {
        dbTester.getDbClient().ceTaskInputDao().insert(dbTester.getSession(), TASK_UUID, i + 1, input);
      }
    }
    dbTester.getSession().commit();
    dbTester.getSession().close();

    ceTask = new CeTask.Builder()
      .setType(CeTaskTypes.REPORT)
      .setUuid(TASK_UUID)
      .setReportPartCount(parts.size())
      .build();

    underTest = new ExtractReportStep(dbTester.getDbClient(), ceTask, tempFolder, reportDirectoryHolder);
    underTest.execute(new TestComputationStepContext());

    // directory contains the uncompressed report (which contains only metadata.pb in this test)
    File unzippedDir = reportDirectoryHolder.getDirectory();
    assertThat(unzippedDir).isDirectory().exists();
    assertThat(unzippedDir.listFiles()).hasSize(1);
    assertThat(new File(unzippedDir, "metadata.pb")).hasContent(CONTENT);

    assertThat(logTester.logs(Level.DEBUG))
      .contains("Finished reading report files in " + parts.size() + " parts")
      .anyMatch(log -> log.matches("Analysis report is \\d+ bytes uncompressed"));
  }

  @Test
  public void unzip_report_should_fail_if_unzip_size_exceed_threshold() throws Exception {
    logTester.setLevel(Level.DEBUG);
    URL zipBombFile = getClass().getResource("/org/sonar/ce/task/projectanalysis/step/ExtractReportStepIT/zip-bomb.zip");
    try (InputStream input = zipBombFile.openStream()) {
      dbTester.getDbClient().ceTaskInputDao().insert(dbTester.getSession(), TASK_UUID, 1, input);
    }
    dbTester.getSession().commit();
    dbTester.getSession().close();

    TestComputationStepContext testComputationStepContext = new TestComputationStepContext();
    assertThatThrownBy(() -> underTest.execute(testComputationStepContext))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Decompression failed because unzipped size reached threshold: 4000000000 bytes");
  }

  private File generateReport() throws IOException {
    File zipDir = tempFolder.newDir();
    File metadataFile = new File(zipDir, "metadata.pb");
    FileUtils.write(metadataFile, CONTENT, UTF_8);
    File zip = tempFolder.newFile();
    ZipUtils.zipDir(zipDir, zip);
    return zip;
  }

  private List<File> splitFile(File report) {
    int position = 0;
    int reportPartMaxSizeBytes = CONTENT.getBytes(UTF_8).length / 2;
    List<File> parts = new ArrayList<>();

    try (RandomAccessFile sourceFile = new RandomAccessFile(report, "r");
         FileChannel sourceChannel = sourceFile.getChannel()) {
      final long sourceSize = Files.size(report.toPath());
      final long numSplits = sourceSize / reportPartMaxSizeBytes;
      final long remainingBytes = sourceSize % reportPartMaxSizeBytes;

      for (; position < numSplits; position++) {
        //write multipart files.
        parts.add(writePart(reportPartMaxSizeBytes, position, sourceChannel, reportPartMaxSizeBytes));
      }
      if (remainingBytes > 0) {
        parts.add(writePart(remainingBytes, position, sourceChannel, reportPartMaxSizeBytes));
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to split report", e);
    }
    return parts;
  }

  private File writePart(long byteSize, long partIndex, FileChannel sourceChannel, int reportPartMaxSizeBytes) throws IOException {
    File file = tempFolder.newFile();
    try (RandomAccessFile toFile = new RandomAccessFile(file, "rw");
         FileChannel toChannel = toFile.getChannel()
    ) {
      sourceChannel.position(partIndex * reportPartMaxSizeBytes);
      toChannel.transferFrom(sourceChannel, 0, byteSize);
    }
    return file;
  }
}
