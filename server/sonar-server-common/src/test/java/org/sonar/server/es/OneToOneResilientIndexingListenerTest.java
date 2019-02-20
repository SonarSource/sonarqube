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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.issue.index.IssueIndexDefinition.TYPE_ISSUE;

public class OneToOneResilientIndexingListenerTest {

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void onSuccess_deletes_rows_from_ES_QUEUE_table() {
    EsQueueDto item1 = insertInQueue(TYPE_ISSUE, "foo");
    EsQueueDto item2 = insertInQueue(TYPE_ISSUE, "bar");
    EsQueueDto item3 = insertInQueue(TYPE_ISSUE, "baz");
    db.commit();

    IndexingListener underTest = newListener(asList(item1, item2, item3));

    underTest.onSuccess(emptyList());
    assertThatEsTableContainsOnly(item1, item2, item3);

    underTest.onSuccess(asList(toDocId(item1), toDocId(item3)));
    assertThatEsTableContainsOnly(item2);

    // onFinish does nothing
    underTest.onFinish(new IndexingResult());
    assertThatEsTableContainsOnly(item2);
  }

  /**
   * ES_QUEUE can contain multiple times the same document, for instance
   * when an issue has been updated multiple times in a row without
   * being successfully indexed.
   * Elasticsearch response does not make difference between the different
   * occurrences (and nevertheless it would be useless). So all the
   * occurrences are marked as successfully indexed if a single request
   * passes.
   */
  @Test
  public void onSuccess_deletes_all_the_rows_with_same_doc_id() {
    EsQueueDto item1 = insertInQueue(TYPE_ISSUE, "foo");
    // same id as item1
    EsQueueDto item2 = insertInQueue(TYPE_ISSUE, item1.getDocId());
    EsQueueDto item3 = insertInQueue(TYPE_ISSUE, "bar");
    db.commit();

    IndexingListener underTest = newListener(asList(item1, item2, item3));

    underTest.onSuccess(asList(toDocId(item1)));
    assertThatEsTableContainsOnly(item3);
  }

  private static DocId toDocId(EsQueueDto dto) {
    IndexType.SimpleIndexMainType mainType = IndexType.parseMainType(dto.getDocType());
    return new DocId(mainType.getIndex(), mainType.getType(), dto.getDocId());
  }

  private IndexingListener newListener(Collection<EsQueueDto> items) {
    return new OneToOneResilientIndexingListener(db.getDbClient(), db.getSession(), items);
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
