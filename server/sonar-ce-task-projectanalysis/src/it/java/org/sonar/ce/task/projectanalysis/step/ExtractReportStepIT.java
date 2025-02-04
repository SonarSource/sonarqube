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
package org.sonar.ce.task.projectanalysis.step;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
import org.sonar.ce.task.projectanalysis.scanner.ScannerReportDirectoryHolderImpl;
import org.sonar.ce.task.projectanalysis.scanner.MutableScannerReportDirectoryHolder;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeTaskTypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExtractReportStepIT {

  private static final String TASK_UUID = "1";

  @Rule
  public JUnitTempFolder tempFolder = new JUnitTempFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private MutableScannerReportDirectoryHolder reportDirectoryHolder = new ScannerReportDirectoryHolderImpl();
  private CeTask ceTask = new CeTask.Builder()
    .setType(CeTaskTypes.REPORT)
    .setUuid(TASK_UUID)
    .build();

  private ExtractReportStep underTest = new ExtractReportStep(dbTester.getDbClient(), ceTask, tempFolder, reportDirectoryHolder);

  @Test
  public void fail_if_report_zip_does_not_exist() {
    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(MessageException.class)
      .hasMessage("Analysis report 1 is missing in database");
  }

  @Test
  public void unzip_report() throws Exception {
    logTester.setLevel(Level.DEBUG);
    File reportFile = generateReport();
    try (InputStream input = FileUtils.openInputStream(reportFile)) {
      dbTester.getDbClient().ceTaskInputDao().insert(dbTester.getSession(), TASK_UUID, input);
    }
    dbTester.getSession().commit();
    dbTester.getSession().close();

    underTest.execute(new TestComputationStepContext());

    // directory contains the uncompressed report (which contains only metadata.pb in this test)
    File unzippedDir = reportDirectoryHolder.getDirectory();
    assertThat(unzippedDir).isDirectory().exists();
    assertThat(unzippedDir.listFiles()).hasSize(1);
    assertThat(new File(unzippedDir, "metadata.pb")).hasContent("{metadata}");

    assertThat(logTester.logs(Level.DEBUG)).anyMatch(log -> log.matches("Analysis report is \\d+ bytes uncompressed"));
  }

  @Test
  public void unzip_report_should_fail_if_unzip_size_exceed_threshold() throws Exception {
    logTester.setLevel(Level.DEBUG);
    URL zipBombFile = getClass().getResource("/org/sonar/ce/task/projectanalysis/step/ExtractReportStepIT/zip-bomb.zip");
    try (InputStream input = zipBombFile.openStream()) {
      dbTester.getDbClient().ceTaskInputDao().insert(dbTester.getSession(), TASK_UUID, input);
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
    FileUtils.write(metadataFile, "{metadata}");
    File zip = tempFolder.newFile();
    ZipUtils.zipDir(zipDir, zip);
    return zip;
  }
}
