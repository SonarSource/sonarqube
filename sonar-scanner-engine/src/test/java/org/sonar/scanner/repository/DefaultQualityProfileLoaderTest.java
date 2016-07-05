/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.sonar.api.utils.MessageException;

import org.sonarqube.ws.QualityProfiles;
import com.google.common.io.Resources;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse.QualityProfile;
import org.sonar.scanner.WsTestUtil;
import org.sonar.scanner.bootstrap.BatchWsClient;
import org.sonar.scanner.repository.DefaultQualityProfileLoader;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultQualityProfileLoaderTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  private DefaultQualityProfileLoader qpLoader;
  private BatchWsClient wsClient;
  private InputStream is;

  @Before
  public void setUp() throws IOException {
    wsClient = mock(BatchWsClient.class);
    is = mock(InputStream.class);
    when(is.read()).thenReturn(-1);
    WsTestUtil.mockStream(wsClient, "/api/qualityprofiles/search.protobuf?projectKey=foo%232&profileName=my-profile%232", is);
    qpLoader = new DefaultQualityProfileLoader(wsClient);
  }

  @Test
  public void testEncoding() throws IOException {
    InputStream is = createEncodedQP("qp");
    WsTestUtil.mockStream(wsClient, "/api/qualityprofiles/search.protobuf?projectKey=foo%232&profileName=my-profile%232", is);

    List<QualityProfile> loaded = qpLoader.load("foo#2", "my-profile#2");
    WsTestUtil.verifyCall(wsClient, "/api/qualityprofiles/search.protobuf?projectKey=foo%232&profileName=my-profile%232");
    verifyNoMoreInteractions(wsClient);
    assertThat(loaded).hasSize(1);
  }

  @Test
  public void testNoProfile() throws IOException {
    InputStream is = createEncodedQP();
    WsTestUtil.mockStream(wsClient, is);

    exception.expect(MessageException.class);
    exception.expectMessage("No quality profiles");

    qpLoader.load("project", null);
    verifyNoMoreInteractions(wsClient);
  }

  @Test
  public void use_real_response() throws IOException {
    InputStream is = getTestResource("quality_profile_search_default");
    WsTestUtil.mockStream(wsClient, "/api/qualityprofiles/search.protobuf?defaults=true", is);

    List<QualityProfile> loaded = qpLoader.loadDefault(null);
    WsTestUtil.verifyCall(wsClient, "/api/qualityprofiles/search.protobuf?defaults=true");
    verifyNoMoreInteractions(wsClient);
    assertThat(loaded).hasSize(1);
  }

  private InputStream getTestResource(String name) throws IOException {
    return Resources.asByteSource(this.getClass().getResource(this.getClass().getSimpleName() + "/" + name))
      .openBufferedStream();
  }

  private static InputStream createEncodedQP(String... names) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    QualityProfiles.SearchWsResponse.Builder responseBuilder = QualityProfiles.SearchWsResponse.newBuilder();

    for (String n : names) {
      QualityProfile qp = QualityProfile.newBuilder().setKey(n).setName(n).setLanguage("lang").build();
      responseBuilder.addProfiles(qp);
    }

    responseBuilder.build().writeTo(os);
    return new ByteArrayInputStream(os.toByteArray());
  }
}
