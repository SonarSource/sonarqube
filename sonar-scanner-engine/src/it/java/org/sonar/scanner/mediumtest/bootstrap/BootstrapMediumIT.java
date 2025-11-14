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
package org.sonar.scanner.mediumtest.bootstrap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.scanner.bootstrap.ScannerMain;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.Qualityprofiles;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.scanner.mediumtest.bootstrap.BootstrapMediumIT.ScannerProperties.SONAR_HOST_URL;
import static org.sonar.scanner.mediumtest.bootstrap.BootstrapMediumIT.ScannerProperties.SONAR_PROJECT_BASE_DIR;
import static org.sonar.scanner.mediumtest.bootstrap.BootstrapMediumIT.ScannerProperties.SONAR_PROJECT_KEY;
import static org.sonar.scanner.mediumtest.bootstrap.BootstrapMediumIT.ScannerProperties.SONAR_VERBOSE;
import static testutils.TestUtils.protobufBody;

class BootstrapMediumIT {

  public static final String PROJECT_KEY = "my-project";
  public static final String QPROFILE_KEY = "profile123";
  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @RegisterExtension
  static WireMockExtension sonarqube = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort())
    .build();

  @BeforeEach
  void mockBareMinimalServerEndpoints() {
    sonarqube.stubFor(get("/api/plugins/installed")
      .willReturn(okJson("""
        {
          "plugins": []
        }
        """)));

    sonarqube.stubFor(get("/api/qualityprofiles/search.protobuf?project=" + PROJECT_KEY)
      .willReturn(aResponse()
        .withResponseBody(protobufBody(Qualityprofiles.SearchWsResponse.newBuilder()
          .addProfiles(Qualityprofiles.SearchWsResponse.QualityProfile.newBuilder()
            .setKey(QPROFILE_KEY)
            .setName("My Profile")
            .setRulesUpdatedAt("2021-01-01T00:00:00+0000")
            .build())
          .build()))));

    sonarqube.stubFor(get("/api/v2/analysis/active_rules?projectKey=" + PROJECT_KEY)
      .willReturn(okJson("""
        []
        """)));

    sonarqube.stubFor(get("/api/languages/list")
      .willReturn(okJson("""
        {
          "languages": []
        }
        """)));

    sonarqube.stubFor(get("/api/features/list")
      .willReturn(okJson("""
        []
        """)));

    sonarqube.stubFor(get("/api/metrics/search?ps=500&p=1")
      .willReturn(okJson("""
        {
          "metrics": [],
          "total": 0,
          "p": 1,
          "ps": 100
        }
        """)));

    sonarqube.stubFor(post(urlPathEqualTo("/api/ce/submit"))
      .withQueryParam("projectKey", equalTo(PROJECT_KEY))
      .willReturn(aResponse()
        .withResponseBody(protobufBody(Ce.SubmitResponse.newBuilder()
          .build()))));
  }

  @Test
  void should_fail_if_invalid_json_input() {
    var in = new ByteArrayInputStream("}".getBytes());

    var exitCode = ScannerMain.run(in, System.out);

    assertThat(exitCode).isEqualTo(1);
    assertThat(logTester.getLogs(Level.ERROR)).hasSize(1);
    assertThat(logTester.getLogs(Level.ERROR).get(0).getFormattedMsg()).isEqualTo("Error during SonarScanner Engine execution");
    assertThat(logTester.getLogs(Level.ERROR).get(0).getThrowable()).isInstanceOf(IllegalArgumentException.class).hasMessage("Failed to parse JSON input");
  }

  @Test
  void should_warn_if_null_property_key() {
    ScannerMain.run(new ByteArrayInputStream("""
      {"scannerProperties": [{"value": "aValueWithoutKey"}]}""".getBytes()), System.out);

    assertThat(logTester.logs(Level.WARN)).contains("Ignoring property with null key. Value='aValueWithoutKey'");
  }

  @Test
  void should_warn_if_null_property_value() {
    ScannerMain.run(new ByteArrayInputStream("""
      {"scannerProperties": [{"key": "aKey", "value": null}]}""".getBytes()), System.out);

    assertThat(logTester.logs(Level.WARN)).contains("Ignoring property with null value. Key='aKey'");
  }

  @Test
  void should_warn_if_not_provided_property_value() {
    ScannerMain.run(new ByteArrayInputStream("""
      {"scannerProperties": [{"key": "aKey"}]}""".getBytes()), System.out);

    assertThat(logTester.logs(Level.WARN)).contains("Ignoring property with null value. Key='aKey'");
  }

  @Test
  void should_warn_if_duplicate_property_keys() {
    ScannerMain.run(new ByteArrayInputStream("""
      {"scannerProperties": [{"key": "aKey", "value": "aValue"}, {"key": "aKey", "value": "aValue"}]}""".getBytes()), System.out);

    assertThat(logTester.logs(Level.WARN)).contains("Duplicated properties. Key='aKey'");
  }

  @Test
  void should_warn_if_null_property() {
    ScannerMain.run(new ByteArrayInputStream("""
      {"scannerProperties": [{"key": "aKey", "value": "aValue"},]}""".getBytes()), System.out);

    assertThat(logTester.logs(Level.WARN)).contains("Ignoring null or empty property");
  }

  @Test
  void should_warn_if_empty_property() {
    ScannerMain.run(new ByteArrayInputStream("""
      {"scannerProperties": [{}]}""".getBytes()), System.out);

    assertThat(logTester.logs(Level.WARN)).contains("Ignoring null or empty property");
  }

  /**
   * For now this test is just checking that the scanner completes successfully, with no input files, and mocking server responses to the bare minimum.
   */
  @Test
  void should_complete_successfully(@TempDir Path baseDir) {
    var exitCode = runScannerEngine(new ScannerProperties()
      .addProperty(SONAR_HOST_URL, sonarqube.baseUrl())
      .addProperty(SONAR_PROJECT_KEY, PROJECT_KEY)
      .addProperty(SONAR_PROJECT_BASE_DIR, baseDir.toString()));

    assertThat(exitCode).isZero();
    assertThat(logTester.logs()).contains("SonarScanner Engine completed successfully");
  }

  @Test
  void should_unwrap_message_exception_without_stacktrace(@TempDir Path baseDir) {
    int exitCode = runScannerEngine(new ScannerProperties()
      .addProperty(SONAR_HOST_URL, sonarqube.baseUrl())
      .addProperty(SONAR_PROJECT_BASE_DIR, baseDir.toString()));

    assertThat(exitCode).isEqualTo(1);
    assertThat(logTester.getLogs(Level.ERROR)).hasSize(1);
    assertThat(logTester.getLogs(Level.ERROR).get(0).getFormattedMsg()).isEqualTo("You must define the following mandatory properties for 'Unknown': sonar.projectKey");
    assertThat(logTester.getLogs(Level.ERROR).get(0).getThrowable()).isNull();
  }

  @Test
  void should_show_message_exception_stacktrace_in_debug(@TempDir Path baseDir) {
    int exitCode = runScannerEngine(new ScannerProperties()
      .addProperty(SONAR_HOST_URL, sonarqube.baseUrl())
      .addProperty(SONAR_PROJECT_BASE_DIR, baseDir.toString())
      .addProperty(SONAR_VERBOSE, "true"));

    assertThat(exitCode).isEqualTo(1);
    assertThat(logTester.getLogs(Level.ERROR)).hasSize(1);
    assertThat(logTester.getLogs(Level.ERROR).get(0).getFormattedMsg()).isEqualTo("You must define the following mandatory properties for 'Unknown': sonar.projectKey");
    assertThat(logTester.getLogs(Level.ERROR).get(0).getThrowable()).isNotNull();
  }

  @Test
  void should_enable_verbose(@TempDir Path baseDir) {
    var properties = new ScannerProperties()
      .addProperty(SONAR_HOST_URL, sonarqube.baseUrl())
      .addProperty(SONAR_PROJECT_KEY, PROJECT_KEY)
      .addProperty(SONAR_PROJECT_BASE_DIR, baseDir.toString());
    runScannerEngine(properties);

    assertThat(logTester.logs(Level.DEBUG)).isEmpty();

    properties.addProperty(SONAR_VERBOSE, "true");
    runScannerEngine(properties);

    assertThat(logTester.logs(Level.DEBUG)).isNotEmpty();
  }

  private int runScannerEngine(ScannerProperties scannerProperties) {
    return ScannerMain.run(new ByteArrayInputStream(scannerProperties.toJson().getBytes()), System.out);
  }

  static class ScannerProperties {
    static final String SONAR_HOST_URL = "sonar.host.url";
    static final String SONAR_PROJECT_KEY = "sonar.projectKey";
    static final String SONAR_PROJECT_BASE_DIR = "sonar.projectBaseDir";
    static final String SONAR_VERBOSE = "sonar.verbose";

    public List<ScannerProperty> scannerProperties = new ArrayList<>();

    ScannerProperties addProperty(String key, String value) {
      scannerProperties.add(new ScannerProperty(key, value));
      return this;
    }

    String toJson() {
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
      } catch (JsonProcessingException e) {
        fail("Failed to serialize scanner properties", e);
        return "";
      }
    }
  }

  record ScannerProperty(String key, String value) {

  }
}
