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

import org.elasticsearch.action.delete.DeleteRequest;
import org.junit.Before;
import org.junit.Test;
import org.sonar.server.search.Index;
import org.sonar.server.search.IndexDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeleteKeyTest {

  IndexDefinition TEST_INDEX = IndexDefinition.createFor("TEST", "TESTING");

  Index index;

  @Before
  public void setUp() {
    index = mock(Index.class);
    when(index.getIndexName()).thenReturn(TEST_INDEX.getIndexName());
  }

  @Test
  public void get_delete_request() {
    String key = "test_key";
    DeleteKey<String> deleteAction = new DeleteKey<>(TEST_INDEX.getIndexType(), key);

    try {
      deleteAction.call();
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo(IndexAction.MISSING_INDEX_EXCEPTION);
    }

    // Insert Index for action
    deleteAction.setIndex(index);

    List<DeleteRequest> requests = deleteAction.call();
    assertThat(requests).hasSize(1);

    DeleteRequest request = requests.get(0);
    assertThat(request.type()).isEqualTo(TEST_INDEX.getIndexType());
    assertThat(request.index()).isEqualTo(TEST_INDEX.getIndexName());
    assertThat(request.id()).isEqualTo(key);
    assertThat(request.refresh()).isTrue();
  }
}
