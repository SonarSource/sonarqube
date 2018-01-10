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
package org.sonar.server.computation.task.projectanalysis.step;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ZipUtils;
import org.sonar.api.utils.internal.JUnitTempFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.queue.CeTask;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportDirectoryHolderImpl;
import org.sonar.server.computation.task.projectanalysis.batch.MutableBatchReportDirectoryHolder;

import static org.assertj.core.api.Assertions.assertThat;

public class ExtractReportStepTest {

  private static final String TASK_UUID = "1";

  @Rule
  public JUnitTempFolder tempFolder = new JUnitTempFolder();

  @Rule
  public LogTester logTester = new LogTester().setLevel(LoggerLevel.INFO);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private MutableBatchReportDirectoryHolder reportDirectoryHolder = new BatchReportDirectoryHolderImpl();
  private CeTask ceTask = new CeTask.Builder().setOrganizationUuid("org1").setType(CeTaskTypes.REPORT).setUuid(TASK_UUID).build();

  private ExtractReportStep underTest = new ExtractReportStep(dbTester.getDbClient(), ceTask, tempFolder, reportDirectoryHolder);

  @Test
  public void fail_if_report_zip_does_not_exist() {
    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Analysis report 1 is missing in database");

    underTest.execute();
  }

  @Test
  public void unzip_report() throws Exception {
    File reportFile = generateReport();
    try (InputStream input = FileUtils.openInputStream(reportFile)) {
      dbTester.getDbClient().ceTaskInputDao().insert(dbTester.getSession(), TASK_UUID, input);
    }
    dbTester.getSession().commit();
    dbTester.getSession().close();

    underTest.execute();

    // directory contains the uncompressed report (which contains only metadata.pb in this test)
    File unzippedDir = reportDirectoryHolder.getDirectory();
    assertThat(unzippedDir).isDirectory().exists();
    assertThat(unzippedDir.listFiles()).hasSize(1);
    assertThat(new File(unzippedDir, "metadata.pb")).hasContent("{metadata}");
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
