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
package org.sonar.scanner.scm;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scm.git.ChangedFile;
import org.sonar.scm.git.GitScmProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ScmChangedFilesProviderTest {
  @Mock
  private ScmConfiguration scmConfiguration;
  @Mock
  private BranchConfiguration branchConfiguration;
  @Mock
  private ScmProvider scmProvider;

  private final Path rootBaseDir = Paths.get("root");
  private final DefaultInputProject project = mock(DefaultInputProject.class);

  private ScmChangedFilesProvider provider;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(project.getBaseDir()).thenReturn(rootBaseDir);
    provider = new ScmChangedFilesProvider();
  }

  @Test
  public void testNoScmProvider() {
    when(branchConfiguration.isPullRequest()).thenReturn(true);
    when(branchConfiguration.targetBranchName()).thenReturn("target");

    ScmChangedFiles scmChangedFiles = provider.provide(scmConfiguration, branchConfiguration, project);

    assertThat(scmChangedFiles.get()).isNull();
    verify(scmConfiguration).provider();
  }

  @Test
  public void testFailIfRelativePath() {
    when(branchConfiguration.targetBranchName()).thenReturn("target");
    when(branchConfiguration.isPullRequest()).thenReturn(true);
    when(scmConfiguration.provider()).thenReturn(scmProvider);
    when(scmProvider.branchChangedFiles("target", rootBaseDir)).thenReturn(Collections.singleton(Paths.get("changedFile")));

    assertThatThrownBy(() -> provider.provide(scmConfiguration, branchConfiguration, project))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("changed file with a relative path");
  }

  @Test
  public void testProviderDoesntSupport() {
    when(branchConfiguration.targetBranchName()).thenReturn("target");
    when(branchConfiguration.isPullRequest()).thenReturn(true);
    when(scmConfiguration.provider()).thenReturn(scmProvider);
    when(scmProvider.branchChangedFiles("target", rootBaseDir)).thenReturn(null);
    ScmChangedFiles scmChangedFiles = provider.provide(scmConfiguration, branchConfiguration, project);

    assertThat(scmChangedFiles.get()).isNull();
    verify(scmProvider).branchChangedFiles("target", rootBaseDir);
  }

  @Test
  public void testNoOpInBranch() {
    when(branchConfiguration.isPullRequest()).thenReturn(false);
    ScmChangedFiles scmChangedFiles = provider.provide(scmConfiguration, branchConfiguration, project);

    assertThat(scmChangedFiles.get()).isNull();
    verifyNoInteractions(scmConfiguration);
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
    when(branchConfiguration.isPullRequest()).thenReturn(true);
    when(branchConfiguration.targetBranchName()).thenReturn("target");

    ScmChangedFiles scmChangedFiles = provider.provide(scmConfiguration, branchConfiguration, project);

    assertThat(scmChangedFiles.get()).isNull();
    verify(scmConfiguration).provider();
  }

  @Test
  public void testGitScmProvider(){
    GitScmProvider gitScmProvider = mock(GitScmProvider.class);

    when(scmConfiguration.provider()).thenReturn(gitScmProvider);
    when(branchConfiguration.isPullRequest()).thenReturn(true);
    when(branchConfiguration.targetBranchName()).thenReturn("target");

    ScmChangedFiles scmChangedFiles = provider.provide(scmConfiguration, branchConfiguration, project);

    assertThat(scmChangedFiles.get()).isEmpty();
    verify(scmConfiguration).provider();

  }

  @Test
  public void testReturnChangedFiles() {
    when(branchConfiguration.targetBranchName()).thenReturn("target");
    when(branchConfiguration.isPullRequest()).thenReturn(true);
    when(scmConfiguration.provider()).thenReturn(scmProvider);
    when(scmProvider.branchChangedFiles("target", rootBaseDir)).thenReturn(Collections.singleton(Paths.get("changedFile").toAbsolutePath()));
    ScmChangedFiles scmChangedFiles = provider.provide(scmConfiguration, branchConfiguration, project);

    Path filePath = Paths.get("changedFile").toAbsolutePath();
    ChangedFile changedFile = ChangedFile.of(filePath);
    assertThat(scmChangedFiles.get()).containsOnly(changedFile);
    verify(scmProvider).branchChangedFiles("target", rootBaseDir);
  }

}
