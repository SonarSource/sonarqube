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
package org.sonar.scanner.mediumtest.analysisdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.core.platform.PluginInfo;
import org.sonar.scanner.mediumtest.AnalysisResult;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.xoo.Xoo;
import org.sonar.xoo.XooPlugin;

public class AnalysisDataIT {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin(new PluginInfo("xoo").setOrganizationName("SonarSource"), new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way");

  @Test
  public void whenScanningWithXoo_thenArchitectureGraphIsInReport() throws Exception {
    // given
    File projectDir = new File("test-resources/mediumtest/xoo/sample");
    File tmpDir = temp.newFolder();
    FileUtils.copyDirectory(projectDir, tmpDir);

    // when
    AnalysisResult result = tester
      .newAnalysis(new File(tmpDir, "sonar-project.properties"))
      .execute();

    // then
    List<ScannerReport.AnalysisData> analysisData = result.analysisData();
    assertThat(analysisData)
      .anyMatch(data -> data.getKey().equals(Xoo.NAME + ".file_graph"));
  }
}
