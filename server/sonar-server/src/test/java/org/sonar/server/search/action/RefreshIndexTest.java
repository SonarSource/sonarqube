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
package org.sonar.server.search.action;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.junit.Before;
import org.junit.Test;
import org.sonar.server.search.Index;
import org.sonar.server.search.IndexDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RefreshIndexTest {
  IndexDefinition TEST_INDEX = IndexDefinition.createFor("TEST", "TESTING");

  Index index;

  @Before
  public void setUp() {
    index = mock(Index.class);
    when(index.getIndexName()).thenReturn(TEST_INDEX.getIndexName());
  }

  @Test
  public void get_delete_request() {
    RefreshIndex refreshAction = new RefreshIndex(TEST_INDEX.getIndexType());

    try {
      refreshAction.call();
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo(IndexAction.MISSING_INDEX_EXCEPTION);
    }

    try {
      refreshAction.getKey();
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Refresh Action has no key");
    }

    // Insert Index for action
    refreshAction.setIndex(index);

    List<RefreshRequest> requests = refreshAction.call();
    assertThat(requests).hasSize(1);

    RefreshRequest request = requests.get(0);
    assertThat(request.indices()).containsOnly(TEST_INDEX.getIndexName());
    assertThat(request.force()).isFalse();
  }
}
