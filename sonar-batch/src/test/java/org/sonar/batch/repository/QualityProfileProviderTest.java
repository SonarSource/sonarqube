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

import static org.mockito.Mockito.when;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.sonar.api.batch.bootstrap.ProjectKey;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.sonarqube.ws.QualityProfiles.WsSearchResponse.QualityProfile;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.isNull;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import org.sonar.batch.rule.ModuleQProfiles;
import org.junit.Test;
import org.sonar.batch.analysis.AnalysisProperties;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;
import org.junit.Before;

public class QualityProfileProviderTest {
  private QualityProfileProvider qualityProfileProvider;

  @Mock
  private QualityProfileLoader loader;
  @Mock
  private DefaultAnalysisMode mode;
  @Mock
  private AnalysisProperties props;
  @Mock
  private ProjectKey key;
  @Mock
  private ProjectRepositories projectRepo;

  private List<QualityProfile> response;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    qualityProfileProvider = new QualityProfileProvider();

    when(key.get()).thenReturn("project");
    when(projectRepo.exists()).thenReturn(true);

    response = new ArrayList<QualityProfile>(1);
    response.add(QualityProfile.newBuilder().setKey("profile").setName("profile").setLanguage("lang").build());
  }

  @Test
  public void testProvide() {
    when(mode.isNotAssociated()).thenReturn(false);
    when(loader.load(eq("project"), isNull(String.class), any(MutableBoolean.class))).thenReturn(response);
    ModuleQProfiles qps = qualityProfileProvider.provide(key, loader, projectRepo, props, mode);
    assertResponse(qps);

    verify(loader).load(eq("project"), isNull(String.class), any(MutableBoolean.class));
    verifyNoMoreInteractions(loader);
  }

  @Test
  public void testNonAssociated() {
    when(mode.isNotAssociated()).thenReturn(true);
    when(loader.loadDefault(any(MutableBoolean.class))).thenReturn(response);
    ModuleQProfiles qps = qualityProfileProvider.provide(key, loader, projectRepo, props, mode);
    assertResponse(qps);

    verify(loader).loadDefault(any(MutableBoolean.class));
    verifyNoMoreInteractions(loader);
  }

  @Test
  public void testProjectDoesntExist() {
    when(mode.isNotAssociated()).thenReturn(false);
    when(projectRepo.exists()).thenReturn(false);
    when(loader.loadDefault(any(MutableBoolean.class))).thenReturn(response);
    ModuleQProfiles qps = qualityProfileProvider.provide(key, loader, projectRepo, props, mode);
    assertResponse(qps);

    verify(loader).loadDefault(any(MutableBoolean.class));
    verifyNoMoreInteractions(loader);
  }

  @Test
  public void testProfileProp() {
    when(mode.isNotAssociated()).thenReturn(false);
    when(loader.load(eq("project"), eq("custom"), any(MutableBoolean.class))).thenReturn(response);
    when(props.property(ModuleQProfiles.SONAR_PROFILE_PROP)).thenReturn("custom");

    ModuleQProfiles qps = qualityProfileProvider.provide(key, loader, projectRepo, props, mode);
    assertResponse(qps);

    verify(loader).load(eq("project"), eq("custom"), any(MutableBoolean.class));
    verifyNoMoreInteractions(loader);
  }

  private void assertResponse(ModuleQProfiles qps) {
    assertThat(qps.findAll()).hasSize(1);
    assertThat(qps.findAll()).extracting("key").containsExactly("profile");

  }
}
