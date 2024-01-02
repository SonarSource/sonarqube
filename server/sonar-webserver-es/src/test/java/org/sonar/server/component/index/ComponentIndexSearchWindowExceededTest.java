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
package org.sonar.server.component.index;

import java.util.stream.IntStream;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_COMPONENT;

public class ComponentIndexSearchWindowExceededTest {
  @Rule
  public EsTester es = EsTester.create();

  private final WebAuthorizationTypeSupport authorizationTypeSupport = mock(WebAuthorizationTypeSupport.class);
  private final ComponentIndex underTest = new ComponentIndex(es.client(), authorizationTypeSupport, System2.INSTANCE);

  @Test
  public void returns_correct_total_number_if_default_index_window_exceeded() {
    // bypassing the permission check, to have easily 12_000 elements searcheable without having to inserting them + permission.
    when(authorizationTypeSupport.createQueryFilter()).thenReturn(QueryBuilders.matchAllQuery());

    index(IntStream.range(0, 12_000)
      .mapToObj(i -> newDoc(ComponentTesting.newPublicProjectDto()))
      .toArray(ComponentDoc[]::new));

    SearchIdResult<String> result = underTest.search(ComponentQuery.builder().build(), new SearchOptions().setPage(2, 3));
    assertThat(result.getTotal()).isEqualTo(12_000);
  }

  private void index(ComponentDoc... componentDocs) {
    es.putDocuments(TYPE_COMPONENT.getMainType(), componentDocs);
  }

  private ComponentDoc newDoc(ComponentDto componentDoc) {
    return new ComponentDoc()
      .setId(componentDoc.uuid())
      .setKey(componentDoc.getKey())
      .setName(componentDoc.name())
      .setProjectUuid(componentDoc.branchUuid())
      .setQualifier(componentDoc.qualifier());
  }
}
