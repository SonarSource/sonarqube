/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.scanner.scm;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.scanner.scan.branch.BranchConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ScmChangedFilesProviderTest {
  @Mock
  private ScmConfiguration scmConfiguration;
  @Mock
  private BranchConfiguration branchConfiguration;
  @Mock
  private InputModuleHierarchy inputModuleHierarchy;
  @Mock
  private ScmProvider scmProvider;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private Path rootBaseDir = Paths.get("root");
  private ScmChangedFilesProvider provider;
  private DefaultInputProject project = mock(DefaultInputProject.class);

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(project.getBaseDir()).thenReturn(rootBaseDir);
    provider = new ScmChangedFilesProvider();
  }

  @Test
  public void testNoScmProvider() {
    when(branchConfiguration.isShortOrPullRequest()).thenReturn(true);
    when(branchConfiguration.targetScmBranch()).thenReturn("target");

    ScmChangedFiles scmChangedFiles = provider.provide(scmConfiguration, branchConfiguration, project);

    assertThat(scmChangedFiles.get()).isNull();
    verify(scmConfiguration).provider();
  }

  @Test
  public void testFailIfRelativePath() {
    when(branchConfiguration.targetScmBranch()).thenReturn("target");
    when(branchConfiguration.isShortOrPullRequest()).thenReturn(true);
    when(scmConfiguration.provider()).thenReturn(scmProvider);
    when(scmProvider.branchChangedFiles("target", rootBaseDir)).thenReturn(Collections.singleton(Paths.get("changedFile")));

    exception.expect(IllegalStateException.class);
    exception.expectMessage("changed file with a relative path");
    provider.provide(scmConfiguration, branchConfiguration, project);
  }

  @Test
  public void testProviderDoesntSupport() {
    when(branchConfiguration.targetScmBranch()).thenReturn("target");
    when(branchConfiguration.isShortOrPullRequest()).thenReturn(true);
    when(scmConfiguration.provider()).thenReturn(scmProvider);
    when(scmProvider.branchChangedFiles("target", rootBaseDir)).thenReturn(null);
    ScmChangedFiles scmChangedFiles = provider.provide(scmConfiguration, branchConfiguration, project);

    assertThat(scmChangedFiles.get()).isNull();
    verify(scmProvider).branchChangedFiles("target", rootBaseDir);
  }

  @Test
  public void testNoOpInNonShortLivedBranch() {
    when(branchConfiguration.isShortOrPullRequest()).thenReturn(false);
    ScmChangedFiles scmChangedFiles = provider.provide(scmConfiguration, branchConfiguration, project);

    assertThat(scmChangedFiles.get()).isNull();
    verifyZeroInteractions(scmConfiguration);
  }

  @Test
  public void testLegacyScmProvider() {
    ScmProvider legacy = new ScmProvider() {
      @Override
      public String key() {
        return null;
      }
    };

    when(scmConfiguration.provider()).thenReturn(legacy);
    when(branchConfiguration.isShortOrPullRequest()).thenReturn(true);
    when(branchConfiguration.targetScmBranch()).thenReturn("target");

    ScmChangedFiles scmChangedFiles = provider.provide(scmConfiguration, branchConfiguration, project);

    assertThat(scmChangedFiles.get()).isNull();
    verify(scmConfiguration).provider();
  }

  @Test
  public void testReturnChangedFiles() {
    when(branchConfiguration.targetScmBranch()).thenReturn("target");
    when(branchConfiguration.isShortOrPullRequest()).thenReturn(true);
    when(scmConfiguration.provider()).thenReturn(scmProvider);
    when(scmProvider.branchChangedFiles("target", rootBaseDir)).thenReturn(Collections.singleton(Paths.get("changedFile").toAbsolutePath()));
    ScmChangedFiles scmChangedFiles = provider.provide(scmConfiguration, branchConfiguration, project);

    assertThat(scmChangedFiles.get()).containsOnly(Paths.get("changedFile").toAbsolutePath());
    verify(scmProvider).branchChangedFiles("target", rootBaseDir);
  }

  @Test
  public void testCacheObject() {
    provider.provide(scmConfiguration, branchConfiguration, project);
    provider.provide(scmConfiguration, branchConfiguration, project);
    verify(branchConfiguration).isShortOrPullRequest();
  }

}
