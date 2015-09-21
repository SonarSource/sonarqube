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
package org.sonar.batch.repository;

import org.sonarqube.ws.QualityProfiles.WsSearchResponse;

import com.google.common.io.Resources;
import org.sonarqube.ws.QualityProfiles.WsSearchResponse.QualityProfile;
import org.sonar.batch.cache.WSLoaderResult;
import org.sonar.batch.cache.WSLoader;
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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultQualityProfileLoaderTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  private DefaultQualityProfileLoader qpLoader;
  private WSLoader ws;
  private InputStream is;

  @Before
  public void setUp() throws IOException {
    ws = mock(WSLoader.class);
    is = mock(InputStream.class);
    when(is.read()).thenReturn(-1);
    WSLoaderResult<InputStream> result = new WSLoaderResult<>(is, false);
    when(ws.loadStream(anyString())).thenReturn(result);
    qpLoader = new DefaultQualityProfileLoader(ws);
  }

  @Test
  public void testEncoding() throws IOException {
    WSLoaderResult<InputStream> result = new WSLoaderResult<>(createEncodedQP("qp"), false);
    when(ws.loadStream(anyString())).thenReturn(result);

    List<QualityProfile> loaded = qpLoader.load("foo#2", "my-profile#2", null);
    verify(ws).loadStream("/qualityprofiles/search?projectKey=foo%232&profileName=my-profile%232");
    verifyNoMoreInteractions(ws);
    assertThat(loaded).hasSize(1);
  }

  @Test
  public void testNoProfile() throws IOException {
    InputStream is = createEncodedQP();
    when(ws.loadStream(anyString())).thenReturn(new WSLoaderResult<InputStream>(is, false));

    exception.expect(IllegalStateException.class);
    exception.expectMessage("No quality profiles");

    qpLoader.load("project", null, null);
    verifyNoMoreInteractions(ws);
  }

  @Test
  public void use_real_response() throws IOException {
    InputStream is = getTestResource("quality_profile_search_default");
    when(ws.loadStream(anyString())).thenReturn(new WSLoaderResult<InputStream>(is, false));

    List<QualityProfile> loaded = qpLoader.loadDefault(null);
    verify(ws).loadStream("/qualityprofiles/search?defaults=true");
    verifyNoMoreInteractions(ws);
    assertThat(loaded).hasSize(1);
  }

  private InputStream getTestResource(String name) throws IOException {
    return Resources.asByteSource(this.getClass().getResource(this.getClass().getSimpleName() + "/" + name))
      .openBufferedStream();
  }

  private static InputStream createEncodedQP(String... names) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    WsSearchResponse.Builder responseBuilder = WsSearchResponse.newBuilder();

    for (String n : names) {
      QualityProfile qp = QualityProfile.newBuilder().setKey(n).setName(n).setLanguage("lang").build();
      responseBuilder.addProfiles(qp);
    }

    responseBuilder.build().writeTo(os);
    return new ByteArrayInputStream(os.toByteArray());
  }
}
