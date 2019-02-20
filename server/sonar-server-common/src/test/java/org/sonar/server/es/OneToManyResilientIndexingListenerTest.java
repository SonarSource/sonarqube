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
package org.sonar.server.es;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.es.EsQueueDto;
import org.sonar.server.component.index.ComponentIndexDefinition;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.issue.index.IssueIndexDefinition.TYPE_ISSUE;

public class OneToManyResilientIndexingListenerTest {

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void ES_QUEUE_rows_are_deleted_when_all_docs_are_successfully_indexed() {
    EsQueueDto item1 = insertInQueue(TYPE_ISSUE, "P1");
    EsQueueDto item2 = insertInQueue(TYPE_ISSUE, "P2");
    EsQueueDto outOfScopeItem = insertInQueue(ComponentIndexDefinition.TYPE_COMPONENT, "P1");
    db.commit();

    // does not contain outOfScopeItem
    IndexingListener underTest = newListener(asList(item1, item2));

    DocId issue1 = newDocId(TYPE_ISSUE, "I1");
    DocId issue2 = newDocId(TYPE_ISSUE, "I2");
    underTest.onSuccess(asList(issue1, issue2));
    assertThatEsTableContainsOnly(item1, item2, outOfScopeItem);

    // onFinish deletes all items
    IndexingResult result = new IndexingResult();
    result.incrementSuccess().incrementRequests();
    result.incrementSuccess().incrementRequests();
    underTest.onFinish(result);

    assertThatEsTableContainsOnly(outOfScopeItem);
  }

  @Test
  public void ES_QUEUE_rows_are_not_deleted_on_partial_error() {
    EsQueueDto item1 = insertInQueue(TYPE_ISSUE, "P1");
    EsQueueDto item2 = insertInQueue(TYPE_ISSUE, "P2");
    EsQueueDto outOfScopeItem = insertInQueue(ComponentIndexDefinition.TYPE_COMPONENT, "P1");
    db.commit();

    // does not contain outOfScopeItem
    IndexingListener underTest = newListener(asList(item1, item2));

    DocId issue1 = newDocId(TYPE_ISSUE, "I1");
    DocId issue2 = newDocId(TYPE_ISSUE, "I2");
    underTest.onSuccess(asList(issue1, issue2));
    assertThatEsTableContainsOnly(item1, item2, outOfScopeItem);

    // one failure among the 2 indexing requests of issues
    IndexingResult result = new IndexingResult();
    result.incrementSuccess().incrementRequests();
    result.incrementRequests();
    underTest.onFinish(result);

    assertThatEsTableContainsOnly(item1, item2, outOfScopeItem);
  }

  private static DocId newDocId(IndexType.IndexRelationType indexType, String id) {
    IndexType.IndexMainType mainType = indexType.getMainType();
    return new DocId(mainType.getIndex().getName(), mainType.getType(), id);
  }

  private IndexingListener newListener(Collection<EsQueueDto> items) {
    return new OneToManyResilientIndexingListener(db.getDbClient(), db.getSession(), items);
  }

  private EsQueueDto insertInQueue(IndexType indexType, String id) {
    EsQueueDto item = EsQueueDto.create(indexType.format(), id);
    db.getDbClient().esQueueDao().insert(db.getSession(), singletonList(item));
    return item;
  }

  private void assertThatEsTableContainsOnly(EsQueueDto... expected) {
    try (DbSession otherSession = db.getDbClient().openSession(false)) {
      List<String> uuidsInDb = db.getDbClient().esQueueDao().selectForRecovery(otherSession, Long.MAX_VALUE, 10)
        .stream().map(EsQueueDto::getUuid).collect(toList());
      String expectedUuids[] = Arrays.stream(expected).map(EsQueueDto::getUuid).toArray(String[]::new);
      assertThat(uuidsInDb).containsExactlyInAnyOrder(expectedUuids);
    }
  }
}
