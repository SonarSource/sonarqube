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
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
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
import org.sonar.batch.protocol.input.ActiveRule;
import org.sonar.batch.protocol.input.ProjectRepositories;
import org.sonar.batch.protocol.input.QProfile;
import org.sonar.batch.repository.DefaultProjectRepositoriesFactory;
import org.sonar.batch.repository.DefaultProjectRepositoriesLoader;
import org.sonar.batch.repository.DefaultProjectSettingsLoader;
import org.sonar.batch.repository.DefaultQualityProfileLoader;
import org.sonar.batch.repository.DefaultServerIssuesLoader;
import org.sonar.batch.repository.ProjectRepositoriesFactory;
import org.sonar.batch.repository.ProjectRepositoriesLoader;
import org.sonar.batch.repository.ProjectSettingsLoader;
import org.sonar.batch.repository.ProjectSettingsRepo;
import org.sonar.batch.repository.QualityProfileLoader;
import org.sonar.batch.repository.ServerIssuesLoader;
import org.sonar.batch.repository.user.UserRepositoryLoader;
import org.sonar.batch.rule.ActiveRulesLoader;
import org.sonar.batch.rule.DefaultActiveRulesLoader;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ProjectCacheSynchronizerTest {
  private static final String BATCH_PROJECT = "/scanner/project?key=org.codehaus.sonar-plugins%3Asonar-scm-git-plugin&preview=true";
  private static final String ISSUES = "/scanner/issues?key=org.codehaus.sonar-plugins%3Asonar-scm-git-plugin";
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
  @Mock
  private WSLoader ws;

  private ProjectRepositoriesLoader projectRepositoryLoader;
  private ServerIssuesLoader issuesLoader;
  private UserRepositoryLoader userRepositoryLoader;
  private QualityProfileLoader qualityProfileLoader;
  private ActiveRulesLoader activeRulesLoader;
  private ProjectSettingsLoader projectSettingsLoader;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);

    String batchProject = getResourceAsString("batch_project.json");
    ByteSource issues = getResourceAsByteSource("batch_issues.protobuf");

    when(ws.loadString(BATCH_PROJECT)).thenReturn(new WSLoaderResult<>(batchProject, false));
    when(ws.loadSource(ISSUES)).thenReturn(new WSLoaderResult<>(issues, false));

    when(analysisMode.isIssues()).thenReturn(true);
    when(properties.properties()).thenReturn(new HashMap<String, String>());
  }

  private ProjectCacheSynchronizer create(ProjectRepositories projectRepositories) {
    if (projectRepositories == null) {
      projectRepositoryLoader = new DefaultProjectRepositoriesLoader(ws, analysisMode);
    } else {
      projectRepositoryLoader = mock(ProjectRepositoriesLoader.class);
      when(projectRepositoryLoader.load(anyString(), anyString(), any(MutableBoolean.class))).thenReturn(projectRepositories);
    }

    ProjectReactor reactor = mock(ProjectReactor.class);
    ProjectDefinition root = mock(ProjectDefinition.class);
    when(root.getKeyWithBranch()).thenReturn(PROJECT_KEY);
    when(reactor.getRoot()).thenReturn(root);

    ProjectRepositoriesFactory projectRepositoriesFactory = new DefaultProjectRepositoriesFactory(reactor, analysisMode, projectRepositoryLoader, properties);

    issuesLoader = new DefaultServerIssuesLoader(ws);
    userRepositoryLoader = new UserRepositoryLoader(ws);
    qualityProfileLoader = new DefaultQualityProfileLoader(projectRepositoriesFactory);
    activeRulesLoader = new DefaultActiveRulesLoader(projectRepositoriesFactory);
    projectSettingsLoader = new DefaultProjectSettingsLoader(projectRepositoriesFactory);

    return new ProjectCacheSynchronizer(qualityProfileLoader, projectSettingsLoader, activeRulesLoader, issuesLoader, userRepositoryLoader, cacheStatus);
  }

  private ProjectCacheSynchronizer createMockedLoaders(Date lastAnalysisDate) {
    issuesLoader = mock(DefaultServerIssuesLoader.class);
    userRepositoryLoader = mock(UserRepositoryLoader.class);
    qualityProfileLoader = mock(DefaultQualityProfileLoader.class);
    activeRulesLoader = mock(DefaultActiveRulesLoader.class);
    projectSettingsLoader = mock(DefaultProjectSettingsLoader.class);

    QProfile pf = new QProfile("profile", "profile", "lang", new Date(1000));
    ActiveRule ar = mock(ActiveRule.class);
    ProjectSettingsRepo repo = mock(ProjectSettingsRepo.class);

    when(qualityProfileLoader.load(PROJECT_KEY, null)).thenReturn(ImmutableList.of(pf));
    when(activeRulesLoader.load(ImmutableList.of("profile"), PROJECT_KEY)).thenReturn(ImmutableList.of(ar));
    when(repo.lastAnalysisDate()).thenReturn(lastAnalysisDate);
    when(projectSettingsLoader.load(anyString(), any(MutableBoolean.class))).thenReturn(repo);

    return new ProjectCacheSynchronizer(qualityProfileLoader, projectSettingsLoader, activeRulesLoader, issuesLoader, userRepositoryLoader, cacheStatus);
  }

  @Test
  public void testSync() {
    ProjectCacheSynchronizer sync = create(null);

    sync.load(PROJECT_KEY, false);

    verify(ws).loadString(BATCH_PROJECT);
    verify(ws).loadSource(ISSUES);
    verifyNoMoreInteractions(ws);

    verify(cacheStatus).save(anyString());
  }

  @Test
  public void testLoadersUsage() {
    ProjectCacheSynchronizer synchronizer = createMockedLoaders(new Date());
    synchronizer.load(PROJECT_KEY, false);

    verify(issuesLoader).load(eq(PROJECT_KEY), any(Function.class));
    verify(qualityProfileLoader).load(PROJECT_KEY, null);
    verify(activeRulesLoader).load(ImmutableList.of("profile"), PROJECT_KEY);
    verify(projectSettingsLoader).load(eq(PROJECT_KEY), any(MutableBoolean.class));

    verifyNoMoreInteractions(issuesLoader, userRepositoryLoader, qualityProfileLoader, activeRulesLoader, projectSettingsLoader);
  }

  @Test
  public void testLoadersUsage_NoLastAnalysis() {
    ProjectCacheSynchronizer synchronizer = createMockedLoaders(null);
    synchronizer.load(PROJECT_KEY, false);

    verify(projectSettingsLoader).load(eq(PROJECT_KEY), any(MutableBoolean.class));

    verifyNoMoreInteractions(issuesLoader, userRepositoryLoader, qualityProfileLoader, activeRulesLoader, projectSettingsLoader);
  }

  @Test
  public void testSyncNoLastAnalysis() {
    ProjectRepositories mockedProjectRepositories = mock(ProjectRepositories.class);
    when(mockedProjectRepositories.lastAnalysisDate()).thenReturn(null);

    ProjectCacheSynchronizer sync = create(mockedProjectRepositories);
    sync.load(PROJECT_KEY, true);
    verify(cacheStatus).save(PROJECT_KEY);
  }

  @Test
  public void testDontSyncIfNotForce() {
    when(cacheStatus.getSyncStatus(PROJECT_KEY)).thenReturn(new Date());
    ProjectCacheSynchronizer sync = create(null);
    sync.load(PROJECT_KEY, false);

    verifyNoMoreInteractions(ws);
  }

  private String getResourceAsString(String name) throws IOException {
    URL resource = this.getClass().getResource(getClass().getSimpleName() + "/" + name);
    return Resources.toString(resource, StandardCharsets.UTF_8);
  }

  private ByteSource getResourceAsByteSource(String name) throws IOException {
    URL resource = this.getClass().getResource(getClass().getSimpleName() + "/" + name);
    return Resources.asByteSource(resource);
  }
}
