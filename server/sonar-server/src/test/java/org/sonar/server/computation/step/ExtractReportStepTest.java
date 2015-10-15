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
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.ZipUtils;
import org.sonar.api.utils.internal.JUnitTempFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.computation.batch.MutableBatchReportDirectoryHolder;
import org.sonar.server.computation.queue.CeTask;
import org.sonar.server.computation.queue.report.ReportFiles;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExtractReportStepTest {

  public static final String TASK_UUID = "1";
  @Rule
  public JUnitTempFolder tempFolder = new JUnitTempFolder();

  @Rule
  public LogTester logTester = new LogTester().setLevel(LoggerLevel.INFO);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  MutableBatchReportDirectoryHolder reportDirectoryHolder = mock(MutableBatchReportDirectoryHolder.class);
  ReportFiles reportFiles = mock(ReportFiles.class);
  CeTask ceTask = new CeTask.Builder().setType(CeTaskTypes.REPORT).setUuid(TASK_UUID).build();

  ExtractReportStep underTest = new ExtractReportStep(reportFiles, ceTask, tempFolder, reportDirectoryHolder);

  @Test
  public void fail_if_report_zip_does_not_exist() throws Exception {
    File zip = tempFolder.newFile();
    FileUtils.forceDelete(zip);
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to unzip " + zip.getPath());

    when(reportFiles.fileForUuid(TASK_UUID)).thenReturn(zip);

    underTest.execute();
  }

  @Test
  public void unzip_report() throws Exception {
    File zipDir = tempFolder.newDir();
    final File metadataFile = new File(zipDir, "metadata.pb");
    FileUtils.write(metadataFile, "{report}");
    File zip = tempFolder.newFile();
    ZipUtils.zipDir(zipDir, zip);
    when(reportFiles.fileForUuid(TASK_UUID)).thenReturn(zip);

    underTest.execute();

    verify(reportDirectoryHolder).setDirectory(argThat(new TypeSafeMatcher<File>() {
      @Override
      protected boolean matchesSafely(File dir) {
        try {
          return dir.isDirectory() && dir.exists() &&
            // directory contains the uncompressed report (which contains only metadata.pb in this test)
            dir.listFiles().length == 1 &&
            FileUtils.contentEquals(dir.listFiles()[0], metadataFile);
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }

      @Override
      public void describeTo(Description description) {

      }
    }));
  }
}
