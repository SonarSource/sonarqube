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
import java.nio.charset.StandardCharsets;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.ce.common.scanner.ScannerReportReaderRule;
import org.sonar.ce.task.step.TestComputationStepContext;

import static org.assertj.core.api.Assertions.assertThat;

class BuildSoftwareCompositionAnalysisStepIT {
  @RegisterExtension
  public final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final MockWebServer server = new MockWebServer();

  ScannerReportReaderRule reportReader = new ScannerReportReaderRule();
  OkHttpClient okHttpClient = new OkHttpClient();

  private final BuildSoftwareCompositionAnalysisStep underTest = new BuildSoftwareCompositionAnalysisStep(reportReader, okHttpClient);

  @BeforeEach
  public void prepare() throws IOException {
    logTester.setLevel(Level.DEBUG);
    server.start();
  }

  @AfterEach
  public void stopServer() throws IOException {
    server.shutdown();
  }

  @Test
  void buildSoftwareCompositionAnalysisStep_shouldBuildDeps() throws Exception {
    var zipFile = getResourceFile("/org/sonar/ce/task/projectanalysis/step/BuildSoftwareCompositionAnalysisStepIT/textbelt/dependency-files.zip");
    reportReader.putDependencyFilesZip(zipFile);

    var tideliftResponse = getResourceContentAsString("/org/sonar/ce/task/projectanalysis/step/BuildSoftwareCompositionAnalysisStepIT/textbelt/tidelift-response.json");
    MockResponse mockedResponse = new MockResponse()
      .setBody(tideliftResponse) // Sample
      .addHeader("Content-Type", "application/json");

    server.enqueue(mockedResponse);

    var url = server.url("/sonar-api/v1/releases/parse-dependency-files").toString();
    underTest.setTideliftUploadUrl(url);
    underTest.setTideliftApiKey("v2/user/fake-key");

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(logTester.logs(Level.INFO)).contains("Release: pkg:npm/nodemailer@5.1.1 VulnerabilityCount: 2");
    assertThat(logTester.logs(Level.INFO).stream()
      .filter(l -> l.matches("Release: pkg:.*/.*@.* VulnerabilityCount: .*"))
      .count()).isEqualTo(253);
  }

  @Test
  void buildSoftwareCompositionAnalysisStep_withNoApiKey_Throws() throws Exception {
    TestComputationStepContext context = new TestComputationStepContext();

    underTest.execute(context);

    assertThat(logTester.logs(Level.WARN)).contains("TIDELIFT_API_KEY is not set");
  }

  @Test
  void buildSoftwareCompositionAnalysisStep_whenReportDoesNotContainDependencyFiles_LogsWarning() throws Exception {
    var url = server.url("/sonar-api/v1/releases/parse-dependency-files").toString();
    underTest.setTideliftUploadUrl(url);
    underTest.setTideliftApiKey("v2/user/fake-key");

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(logTester.logs(Level.WARN)).contains("No dependency files found");
  }

  @Test
  void buildSoftwareCompositionAnalysisStep_WithErrorFromTidelift_Logs() throws Exception {
    var zipFile = getResourceFile("/org/sonar/ce/task/projectanalysis/step/BuildSoftwareCompositionAnalysisStepIT/textbelt/dependency-files.zip");
    reportReader.putDependencyFilesZip(zipFile);

    MockResponse mockedResponse = new MockResponse().setResponseCode(500);

    server.enqueue(mockedResponse);

    var url = server.url("/sonar-api/v1/releases/parse-dependency-files").toString();
    underTest.setTideliftUploadUrl(url);
    underTest.setTideliftApiKey("v2/user/fake-key");

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertThat(logTester.logs(Level.WARN)).contains("Processing failed. Tidelift Response code:500 message:Server Error");
  }

  private File getResourceFile(String path) {
    return new File(getClass().getResource(path).getFile());
  }

  private String getResourceContentAsString(String path) throws IOException {
    var stream = getClass().getResourceAsStream(path);
    return IOUtils.toString(stream, StandardCharsets.UTF_8);
  }
}
