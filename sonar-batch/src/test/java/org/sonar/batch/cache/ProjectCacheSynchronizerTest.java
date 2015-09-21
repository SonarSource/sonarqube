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
package org.sonar.batch.cache;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.batch.analysis.AnalysisProperties;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.sonar.batch.repository.DefaultProjectRepositoriesLoader;
import org.sonar.batch.repository.DefaultQualityProfileLoader;
import org.sonar.batch.repository.DefaultServerIssuesLoader;
import org.sonar.batch.repository.ProjectRepositories;
import org.sonar.batch.repository.ProjectRepositoriesLoader;
import org.sonar.batch.repository.QualityProfileLoader;
import org.sonar.batch.repository.ServerIssuesLoader;
import org.sonar.batch.repository.user.UserRepositoryLoader;
import org.sonar.batch.rule.ActiveRulesLoader;
import org.sonar.batch.rule.DefaultActiveRulesLoader;
import org.sonarqube.ws.QualityProfiles.WsSearchResponse.QualityProfile;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import static org.mockito.Matchers.anyBoolean;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ProjectCacheSynchronizerTest {
  private static final String PROJECT_KEY = "org.codehaus.sonar-plugins:sonar-scm-git-plugin";

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private ProjectDefinition project;
  @Mock
  private ProjectReactor projectReactor;
  @Mock
  private ProjectCacheStatus cacheStatus;
  @Mock
  private DefaultAnalysisMode analysisMode;
  @Mock
  private AnalysisProperties properties;

  private ServerIssuesLoader issuesLoader;
  private UserRepositoryLoader userRepositoryLoader;
  private QualityProfileLoader qualityProfileLoader;
  private ActiveRulesLoader activeRulesLoader;
  private ProjectRepositoriesLoader projectRepositoriesLoader;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);

    when(analysisMode.isIssues()).thenReturn(true);
    when(properties.properties()).thenReturn(new HashMap<String, String>());
  }

  private ProjectCacheSynchronizer createMockedLoaders(boolean projectExists, Date lastAnalysisDate) {
    issuesLoader = mock(DefaultServerIssuesLoader.class);
    userRepositoryLoader = mock(UserRepositoryLoader.class);
    qualityProfileLoader = mock(DefaultQualityProfileLoader.class);
    activeRulesLoader = mock(DefaultActiveRulesLoader.class);
    projectRepositoriesLoader = mock(DefaultProjectRepositoriesLoader.class);

    QualityProfile pf = QualityProfile.newBuilder().setKey("profile").setName("profile").setLanguage("lang").build();
    org.sonarqube.ws.Rules.Rule ar = org.sonarqube.ws.Rules.Rule.newBuilder().build();
    ProjectRepositories repo = mock(ProjectRepositories.class);

    when(qualityProfileLoader.load(PROJECT_KEY, null, null)).thenReturn(ImmutableList.of(pf));
    when(qualityProfileLoader.loadDefault(null)).thenReturn(ImmutableList.of(pf));
    when(activeRulesLoader.load("profile", null)).thenReturn(ImmutableList.of(ar));
    when(repo.lastAnalysisDate()).thenReturn(lastAnalysisDate);
    when(repo.exists()).thenReturn(projectExists);
    when(projectRepositoriesLoader.load(anyString(), anyBoolean(), any(MutableBoolean.class))).thenReturn(repo);

    return new ProjectCacheSynchronizer(qualityProfileLoader, projectRepositoriesLoader, activeRulesLoader, issuesLoader, userRepositoryLoader, cacheStatus);
  }

  @Test
  public void testLoadersUsage() {
    ProjectCacheSynchronizer synchronizer = createMockedLoaders(true, new Date());
    synchronizer.load(PROJECT_KEY, false);

    verify(issuesLoader).load(eq(PROJECT_KEY), any(Function.class));
    verify(qualityProfileLoader).load(PROJECT_KEY, null, null);
    verify(activeRulesLoader).load("profile", null);
    verify(projectRepositoriesLoader).load(eq(PROJECT_KEY), eq(true), any(MutableBoolean.class));

    verifyNoMoreInteractions(issuesLoader, userRepositoryLoader, qualityProfileLoader, activeRulesLoader, projectRepositoriesLoader);
  }

  @Test
  public void testLoadersUsage_NoLastAnalysis() {
    ProjectCacheSynchronizer synchronizer = createMockedLoaders(true, null);
    synchronizer.load(PROJECT_KEY, false);

    verify(projectRepositoriesLoader).load(eq(PROJECT_KEY), eq(true), any(MutableBoolean.class));
    verify(qualityProfileLoader).load(PROJECT_KEY, null, null);
    verify(activeRulesLoader).load("profile", null);

    verifyNoMoreInteractions(issuesLoader, userRepositoryLoader, qualityProfileLoader, activeRulesLoader, projectRepositoriesLoader);
  }

  @Test
  public void testLoadersUsage_ProjectDoesntExist() {
    ProjectCacheSynchronizer synchronizer = createMockedLoaders(false, null);
    synchronizer.load(PROJECT_KEY, false);

    verify(projectRepositoriesLoader).load(eq(PROJECT_KEY), eq(true), any(MutableBoolean.class));
    verify(qualityProfileLoader).loadDefault(null);
    verify(activeRulesLoader).load("profile", null);

    verifyNoMoreInteractions(issuesLoader, userRepositoryLoader, qualityProfileLoader, activeRulesLoader, projectRepositoriesLoader);
  }
}
