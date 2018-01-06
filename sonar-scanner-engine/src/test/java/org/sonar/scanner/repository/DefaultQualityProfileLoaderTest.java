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
package org.sonar.scanner.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.WsTestUtil;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonarqube.ws.Qualityprofiles;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse.QualityProfile;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class DefaultQualityProfileLoaderTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  private ScannerWsClient wsClient = mock(ScannerWsClient.class);
  private MapSettings settings = new MapSettings();
  private DefaultQualityProfileLoader underTest = new DefaultQualityProfileLoader(settings.asConfig(), wsClient);

  @Test
  public void load_gets_profiles_for_specified_project_and_profile_name() throws IOException {
    WsTestUtil.mockStream(wsClient, "/api/qualityprofiles/search.protobuf?projectKey=foo", createStreamOfProfiles("qp"));
    WsTestUtil.mockStream(wsClient, "/api/qualityprofiles/search.protobuf?profileName=bar", createStreamOfProfiles("qp"));
    underTest.load("foo", "bar");
    verifyCalledPath("/api/qualityprofiles/search.protobuf?projectKey=foo");
    verifyCalledPath("/api/qualityprofiles/search.protobuf?profileName=bar");
  }

  @Test
  public void load_gets_all_profiles_for_specified_project() throws IOException {
    prepareCallWithResults();
    underTest.load("foo", null);
    verifyCalledPath("/api/qualityprofiles/search.protobuf?projectKey=foo");
  }

  @Test
  public void load_encodes_url_parameters() throws IOException {
    WsTestUtil.mockStream(wsClient, "/api/qualityprofiles/search.protobuf?projectKey=foo%232", createStreamOfProfiles("qp"));
    WsTestUtil.mockStream(wsClient, "/api/qualityprofiles/search.protobuf?profileName=bar%232", createStreamOfProfiles("qp"));
    underTest.load("foo#2", "bar#2");
    verifyCalledPath("/api/qualityprofiles/search.protobuf?projectKey=foo%232");
    verifyCalledPath("/api/qualityprofiles/search.protobuf?profileName=bar%232");
  }

  @Test
  public void load_sets_organization_parameter_if_defined_in_settings() throws IOException {
    settings.setProperty("sonar.organization", "my-org");
    prepareCallWithResults();
    underTest.load("foo", null);
    verifyCalledPath("/api/qualityprofiles/search.protobuf?projectKey=foo&organization=my-org");
  }

  @Test
  public void loadDefault_gets_profiles_with_specified_name() throws IOException {
    WsTestUtil.mockStream(wsClient, "/api/qualityprofiles/search.protobuf?defaults=true", createStreamOfProfiles("qp"));
    WsTestUtil.mockStream(wsClient, "/api/qualityprofiles/search.protobuf?profileName=foo", createStreamOfProfiles("qp"));
    underTest.loadDefault("foo");
    verifyCalledPath("/api/qualityprofiles/search.protobuf?defaults=true");
    verifyCalledPath("/api/qualityprofiles/search.protobuf?profileName=foo");
  }

  @Test
  public void loadDefault_gets_all_default_profiles() throws IOException {
    prepareCallWithResults();
    underTest.loadDefault(null);
    verifyCalledPath("/api/qualityprofiles/search.protobuf?defaults=true");
  }

  @Test
  public void loadDefault_sets_organization_parameter_if_defined_in_settings() throws IOException {
    settings.setProperty("sonar.organization", "my-org");
    WsTestUtil.mockStream(wsClient, "/api/qualityprofiles/search.protobuf?defaults=true&organization=my-org", createStreamOfProfiles("qp"));
    WsTestUtil.mockStream(wsClient, "/api/qualityprofiles/search.protobuf?profileName=foo&organization=my-org", createStreamOfProfiles("qp"));
    underTest.loadDefault("foo");
    verifyCalledPath("/api/qualityprofiles/search.protobuf?defaults=true&organization=my-org");
    verifyCalledPath("/api/qualityprofiles/search.protobuf?profileName=foo&organization=my-org");
  }

  @Test
  public void load_throws_MessageException_if_no_profiles_are_available_for_specified_project() throws IOException {
    prepareCallWithEmptyResults();

    exception.expect(MessageException.class);
    exception.expectMessage("No quality profiles");

    underTest.load("project", null);
    verifyNoMoreInteractions(wsClient);
  }

  private void verifyCalledPath(String expectedPath) {
    WsTestUtil.verifyCall(wsClient, expectedPath);
  }

  private void prepareCallWithResults() throws IOException {
    WsTestUtil.mockStream(wsClient, createStreamOfProfiles("qp"));
  }

  private void prepareCallWithEmptyResults() throws IOException {
    WsTestUtil.mockStream(wsClient, createStreamOfProfiles());
  }

  private static InputStream createStreamOfProfiles(String... names) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    Qualityprofiles.SearchWsResponse.Builder responseBuilder = Qualityprofiles.SearchWsResponse.newBuilder();

    for (String n : names) {
      QualityProfile qp = QualityProfile.newBuilder().setKey(n).setName(n).setLanguage("lang").build();
      responseBuilder.addProfiles(qp);
    }

    responseBuilder.build().writeTo(os);
    return new ByteArrayInputStream(os.toByteArray());
  }
}
