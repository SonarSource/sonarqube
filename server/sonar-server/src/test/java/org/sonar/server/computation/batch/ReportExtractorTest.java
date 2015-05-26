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
package org.sonar.server.computation.batch;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.internal.JUnitTempFolder;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.server.computation.ReportQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ReportExtractorTest {

  @Rule
  public JUnitTempFolder tempFolder = new JUnitTempFolder();

  ReportExtractor underTest = new ReportExtractor(tempFolder);


  @Test
  public void fail_if_corrupted_zip() throws Exception {
    AnalysisReportDto dto = newDefaultReport();
    File zip = tempFolder.newFile();
    FileUtils.write(zip, "not a file");

    try {
      underTest.extractReportInDir(new ReportQueue.Item(dto, zip));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).startsWith("Fail to unzip " + zip.getAbsolutePath() + " into ");
    }
  }

  private AnalysisReportDto newDefaultReport() {
    return AnalysisReportDto.newForTests(1L).setProjectKey("P1").setUuid("U1").setStatus(AnalysisReportDto.Status.PENDING);
  }
}
