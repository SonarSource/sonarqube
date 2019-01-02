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
package org.sonar.scanner.scan.branch;

import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectPullRequestsProviderTest {
  private ProjectPullRequestsProvider provider = new ProjectPullRequestsProvider();
  private ProjectPullRequestsLoader mockLoader;
  private ProjectPullRequests pullRequests;

  @Before
  public void setUp() {
    mockLoader = mock(ProjectPullRequestsLoader.class);
    pullRequests = new ProjectPullRequests(emptyList());
  }

  @Test
  public void cache_pull_requests() {
    ProjectPullRequests pullRequests = provider.provide(null, () -> "project");

    assertThat(provider.provide(null, () -> "project")).isSameAs(pullRequests);
  }

  @Test
  public void should_use_loader() {
    when(mockLoader.load("key")).thenReturn(pullRequests);

    ProjectPullRequests result = provider.provide(mockLoader, () -> "key");

    assertThat(result).isSameAs(pullRequests);
  }

  @Test
  public void should_return_default_if_no_loader() {
    ProjectPullRequests result = provider.provide(null, () -> "project");

    assertThat(result.isEmpty()).isTrue();
  }
}
