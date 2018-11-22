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
package org.sonar.scanner.repository;

import com.google.common.collect.Maps;
import java.util.Date;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.bootstrap.ScannerProperties;
import org.sonar.scanner.scan.branch.BranchConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ProjectRepositoriesProviderTest {
  private ProjectRepositoriesProvider provider;
  private ProjectRepositories project;

  @Mock
  private ProjectRepositoriesLoader loader;
  @Mock
  private ScannerProperties props;
  @Mock
  private GlobalAnalysisMode mode;
  @Mock
  private BranchConfiguration branchConfiguration;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    Map<String, FileData> fileMap = Maps.newHashMap();

    project = new SingleProjectRepository(fileMap, new Date());
    provider = new ProjectRepositoriesProvider();

    when(props.getKeyWithBranch()).thenReturn("key");
  }

  @Test
  public void testValidation() {
    when(mode.isIssues()).thenReturn(true);
    when(loader.load(eq("key"), eq(true), any())).thenReturn(project);

    provider.provide(loader, props, mode, branchConfiguration);
  }

  @Test
  public void testAssociated() {
    when(mode.isIssues()).thenReturn(false);
    when(loader.load(eq("key"), eq(false), any())).thenReturn(project);

    ProjectRepositories repo = provider.provide(loader, props, mode, branchConfiguration);

    assertThat(repo.exists()).isEqualTo(true);
    assertThat(repo.lastAnalysisDate()).isNotNull();

    verify(mode, times(1)).isIssues();
    verify(props).getKeyWithBranch();
    verify(loader).load(eq("key"), eq(false), eq(null));
    verifyNoMoreInteractions(loader, props, mode);
  }
}
