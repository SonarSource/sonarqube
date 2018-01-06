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

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.bootstrap.ProjectKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.scanner.analysis.AnalysisProperties;
import org.sonar.scanner.rule.ModuleQProfiles;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse.QualityProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class QualityProfileProviderTest {

  @Rule
  public LogTester logTester = new LogTester();

  private QualityProfileProvider qualityProfileProvider;

  @Mock
  private QualityProfileLoader loader;
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

    response = new ArrayList<>(1);
    response.add(QualityProfile.newBuilder().setKey("profile").setName("profile").setLanguage("lang").setRulesUpdatedAt(DateUtils.formatDateTime(new Date())).build());
  }

  @Test
  public void testProvide() {
    when(loader.load("project", null)).thenReturn(response);
    ModuleQProfiles qps = qualityProfileProvider.provide(key, loader, projectRepo, props);
    assertResponse(qps);

    verify(loader).load("project", null);
    verifyNoMoreInteractions(loader);
  }

  @Test
  public void testProjectDoesntExist() {
    when(projectRepo.exists()).thenReturn(false);
    when(loader.loadDefault(anyString())).thenReturn(response);
    when(props.property(ModuleQProfiles.SONAR_PROFILE_PROP)).thenReturn("profile");
    ModuleQProfiles qps = qualityProfileProvider.provide(key, loader, projectRepo, props);
    assertResponse(qps);

    verify(loader).loadDefault(anyString());
    verifyNoMoreInteractions(loader);
  }

  @Test
  public void testProfileProp() {
    when(loader.load(eq("project"), eq("custom"))).thenReturn(response);
    when(props.property(ModuleQProfiles.SONAR_PROFILE_PROP)).thenReturn("custom");
    when(props.properties()).thenReturn(ImmutableMap.of(ModuleQProfiles.SONAR_PROFILE_PROP, "custom"));

    ModuleQProfiles qps = qualityProfileProvider.provide(key, loader, projectRepo, props);
    assertResponse(qps);

    verify(loader).load(eq("project"), eq("custom"));
    verifyNoMoreInteractions(loader);
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Ability to set quality profile from command line using '" + ModuleQProfiles.SONAR_PROFILE_PROP
      + "' is deprecated and will be dropped in a future SonarQube version. Please configure quality profile used by your project on SonarQube server.");
  }

  @Test
  public void testProfilePropDefault() {
    when(projectRepo.exists()).thenReturn(false);
    when(loader.loadDefault(eq("custom"))).thenReturn(response);
    when(props.property(ModuleQProfiles.SONAR_PROFILE_PROP)).thenReturn("custom");
    when(props.properties()).thenReturn(ImmutableMap.of(ModuleQProfiles.SONAR_PROFILE_PROP, "custom"));

    ModuleQProfiles qps = qualityProfileProvider.provide(key, loader, projectRepo, props);
    assertResponse(qps);

    verify(loader).loadDefault(eq("custom"));
    verifyNoMoreInteractions(loader);
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Ability to set quality profile from command line using '" + ModuleQProfiles.SONAR_PROFILE_PROP
      + "' is deprecated and will be dropped in a future SonarQube version. Please configure quality profile used by your project on SonarQube server.");
  }

  private void assertResponse(ModuleQProfiles qps) {
    assertThat(qps.findAll()).extracting("key").containsExactly("profile");

  }
}
