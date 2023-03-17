/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.junit.Test;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.WsTestUtil;
import org.sonar.scanner.bootstrap.DefaultScannerWsClient;
import org.sonarqube.ws.Qualityprofiles;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.client.HttpException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class DefaultQualityProfileLoaderTest {

  private final DefaultScannerWsClient wsClient = mock(DefaultScannerWsClient.class);
  private final DefaultQualityProfileLoader underTest = new DefaultQualityProfileLoader(wsClient);

  @Test
  public void load_gets_all_profiles_for_specified_project() throws IOException {
    prepareCallWithResults();
    underTest.load("foo");
    verifyCalledPath("/api/qualityprofiles/search.protobuf?project=foo");
  }

  @Test
  public void load_encodes_url_parameters() throws IOException {
    WsTestUtil.mockStream(wsClient, "/api/qualityprofiles/search.protobuf?project=foo%232", createStreamOfProfiles("qp"));
    underTest.load("foo#2");
    verifyCalledPath("/api/qualityprofiles/search.protobuf?project=foo%232");
  }

  @Test
  public void load_tries_default_if_no_profiles_found_for_project() throws IOException {
    HttpException e = new HttpException("", 404, "{\"errors\":[{\"msg\":\"No project found with key 'foo'\"}]}");
    WsTestUtil.mockException(wsClient, "/api/qualityprofiles/search.protobuf?project=foo", e);
    WsTestUtil.mockStream(wsClient, "/api/qualityprofiles/search.protobuf?defaults=true", createStreamOfProfiles("qp"));

    underTest.load("foo");

    verifyCalledPath("/api/qualityprofiles/search.protobuf?project=foo");
    verifyCalledPath("/api/qualityprofiles/search.protobuf?defaults=true");
  }

  @Test
  public void load_throws_MessageException_if_no_profiles_are_available_for_specified_project() throws IOException {
    prepareCallWithEmptyResults();

    assertThatThrownBy(() -> underTest.load("project"))
      .isInstanceOf(MessageException.class)
      .hasMessageContaining("No quality profiles");
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
