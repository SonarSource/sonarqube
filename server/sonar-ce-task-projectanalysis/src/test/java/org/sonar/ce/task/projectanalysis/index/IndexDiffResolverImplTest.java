/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.index;

import java.util.Collection;
import java.util.Set;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.issue.ChangedIssuesRepository;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.measure.index.ProjectMeasuresIndexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IndexDiffResolverImplTest {

  private final ChangedIssuesRepository changedIssuesRepository = mock(ChangedIssuesRepository.class);

  private final IndexDiffResolverImpl underTest = new IndexDiffResolverImpl(changedIssuesRepository);

  @Test
  public void resolve_whenIssueIndexer_shouldReturnChangedIssueKeys() {
    when(changedIssuesRepository.getChangedIssuesKeys()).thenReturn(Set.of("key1", "key2","key3"));
    Collection<String> resolvedDiff = underTest.resolve(IssueIndexer.class);
    
    assertThat(resolvedDiff)
      .containsExactlyInAnyOrder("key1", "key2","key3");
  }

  @Test
  public void resolve_whenUnsupportedIndexer_shouldThrowUPE() {
    when(changedIssuesRepository.getChangedIssuesKeys()).thenReturn(Set.of("key1", "key2","key3"));
    assertThatThrownBy(() ->underTest.resolve(ProjectMeasuresIndexer.class))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessage("Unsupported indexer: class org.sonar.server.measure.index.ProjectMeasuresIndexer");
  }
}
