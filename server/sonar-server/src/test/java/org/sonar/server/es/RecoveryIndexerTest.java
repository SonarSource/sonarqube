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

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.es.EsQueueDto;
import org.sonar.server.es.IndexType.IndexMainType;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.user.index.UserIndexer;

import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.sonar.api.utils.log.LoggerLevel.ERROR;
import static org.sonar.api.utils.log.LoggerLevel.INFO;
import static org.sonar.api.utils.log.LoggerLevel.TRACE;

public class RecoveryIndexerTest {

  private static final long PAST = 1_000L;
  private static final IndexMainType FOO_TYPE = IndexType.main(Index.simple("foos"), "foo");

  private TestSystem2 system2 = new TestSystem2().setNow(PAST);
  private MapSettings emptySettings = new MapSettings();

  @Rule
  public EsTester es = EsTester.createCustom();
  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public LogTester logTester = new LogTester().setLevel(TRACE);
  @Rule
  public TestRule safeguard = new DisableOnDebug(Timeout.builder().withTimeout(60, TimeUnit.SECONDS).withLookingForStuckThread(true).build());

  private RecoveryIndexer underTest;

  @After
  public void tearDown() {
    if (underTest != null) {
      underTest.stop();
    }
  }

  @Test
  public void display_default_configuration_at_startup() {
    underTest = newRecoveryIndexer(emptySettings.asConfig());

    underTest.start();
    underTest.stop();

    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains(
      "Elasticsearch recovery - sonar.search.recovery.delayInMs=300000",
      "Elasticsearch recovery - sonar.search.recovery.minAgeInMs=300000");
  }

  @Test
  public void start_triggers_recovery_run_at_fixed_rate() throws Exception {
    MapSettings settings = new MapSettings()
      .setProperty("sonar.search.recovery.initialDelayInMs", "0")
      .setProperty("sonar.search.recovery.delayInMs", "1");
    underTest = spy(new RecoveryIndexer(system2, settings.asConfig(), db.getDbClient()));
    AtomicInteger calls = new AtomicInteger(0);
    doAnswer(invocation -> {
      calls.incrementAndGet();
      return null;
    }).when(underTest).recover();

    underTest.start();

    // wait for 2 runs
    while (calls.get() < 2) {
      Thread.sleep(1L);
    }
  }

  @Test
  public void successfully_recover_indexing_requests() {
    EsQueueDto item1a = insertItem(FOO_TYPE, "f1");
    EsQueueDto item1b = insertItem(FOO_TYPE, "f2");
    IndexMainType type2 = IndexType.main(Index.simple("bars"), "bar");
    EsQueueDto item2 = insertItem(type2, "b1");

    SuccessfulFakeIndexer indexer1 = new SuccessfulFakeIndexer(FOO_TYPE);
    SuccessfulFakeIndexer indexer2 = new SuccessfulFakeIndexer(type2);
    advanceInTime();

    underTest = newRecoveryIndexer(indexer1, indexer2);
    underTest.recover();

    assertThatQueueHasSize(0);
    assertThatLogsContain(INFO, "Elasticsearch recovery - 3 documents processed [0 failures]");

    assertThat(indexer1.called).hasSize(1);
    assertThat(indexer1.called.get(0))
      .extracting(EsQueueDto::getUuid)
      .containsExactlyInAnyOrder(item1a.getUuid(), item1b.getUuid());
    assertThatLogsContain(TRACE, "Elasticsearch recovery - processing 2 [foos/foo]");

    assertThat(indexer2.called).hasSize(1);
    assertThat(indexer2.called.get(0))
      .extracting(EsQueueDto::getUuid)
      .containsExactlyInAnyOrder(item2.getUuid());
    assertThatLogsContain(TRACE, "Elasticsearch recovery - processing 1 [bars/bar]");
  }

  @Test
  public void recent_records_are_not_recovered() {
    EsQueueDto item = insertItem(FOO_TYPE, "f1");

    SuccessfulFakeIndexer indexer = new SuccessfulFakeIndexer(FOO_TYPE);
    // do not advance in time

    underTest = newRecoveryIndexer(indexer);
    underTest.recover();

    assertThatQueueHasSize(1);
    assertThat(indexer.called).isEmpty();

    assertThatLogsDoNotContain(TRACE, "Elasticsearch recovery - processing 2 [foos/foo]");
    assertThatLogsDoNotContain(INFO, "documents processed");
  }

  @Test
  public void do_nothing_if_queue_is_empty() {
    underTest = newRecoveryIndexer();

    underTest.recover();

    assertThatNoLogsFromRecovery(INFO);
    assertThatNoLogsFromRecovery(ERROR);
    assertThatQueueHasSize(0);
  }

  @Test
  public void hard_failures_are_logged_and_do_not_stop_recovery_scheduling() throws Exception {
    insertItem(FOO_TYPE, "f1");

    HardFailingFakeIndexer indexer = new HardFailingFakeIndexer(FOO_TYPE);
    advanceInTime();

    underTest = newRecoveryIndexer(indexer);
    underTest.start();

    // all runs fail, but they are still scheduled
    // -> waiting for 2 runs
    while (indexer.called.size() < 2) {
      Thread.sleep(1L);
    }

    underTest.stop();

    // No rows treated
    assertThatQueueHasSize(1);
    assertThatLogsContain(ERROR, "Elasticsearch recovery - fail to recover documents");
  }

  @Test
  public void soft_failures_are_logged_and_do_not_stop_recovery_scheduling() throws Exception {
    insertItem(FOO_TYPE, "f1");

    SoftFailingFakeIndexer indexer = new SoftFailingFakeIndexer(FOO_TYPE);
    advanceInTime();

    underTest = newRecoveryIndexer(indexer);
    underTest.start();

    // all runs fail, but they are still scheduled
    // -> waiting for 2 runs
    while (indexer.called.size() < 2) {
      Thread.sleep(1L);
    }

    underTest.stop();

    // No rows treated
    assertThatQueueHasSize(1);
    assertThatLogsContain(INFO, "Elasticsearch recovery - 1 documents processed [1 failures]");
  }

  @Test
  public void unsupported_types_are_kept_in_queue_for_manual_fix_operation() {
    insertItem(FOO_TYPE, "f1");

    ResilientIndexer indexer = new SuccessfulFakeIndexer(IndexType.main(Index.simple("bars"), "bar"));
    advanceInTime();

    underTest = newRecoveryIndexer(indexer);
    underTest.recover();

    assertThatQueueHasSize(1);
    assertThatLogsContain(ERROR, "Elasticsearch recovery - ignore 1 items with unsupported type [foos/foo]");
  }

  @Test
  public void stop_run_if_too_many_failures() {
    IntStream.range(0, 10).forEach(i -> insertItem(FOO_TYPE, "" + i));
    advanceInTime();

    // 10 docs to process, by groups of 3.
    // The first group successfully recovers only 1 docs --> above 30% of failures --> stop run
    PartiallyFailingIndexer indexer = new PartiallyFailingIndexer(FOO_TYPE, 1);
    MapSettings settings = new MapSettings()
      .setProperty("sonar.search.recovery.loopLimit", "3");
    underTest = newRecoveryIndexer(settings.asConfig(), indexer);
    underTest.recover();

    assertThatLogsContain(ERROR, "Elasticsearch recovery - too many failures [2/3 documents], waiting for next run");
    assertThatQueueHasSize(9);

    // The indexer must have been called once and only once.
    assertThat(indexer.called).hasSize(3);
  }

  @Test
  public void do_not_stop_run_if_success_rate_is_greater_than_circuit_breaker() {
    IntStream.range(0, 10).forEach(i -> insertItem(FOO_TYPE, "" + i));
    advanceInTime();

    // 10 docs to process, by groups of 5.
    // Each group successfully recovers 4 docs --> below 30% of failures --> continue run
    PartiallyFailingIndexer indexer = new PartiallyFailingIndexer(FOO_TYPE, 4, 4, 2);
    MapSettings settings = new MapSettings()
      .setProperty("sonar.search.recovery.loopLimit", "5");
    underTest = newRecoveryIndexer(settings.asConfig(), indexer);
    underTest.recover();

    assertThatLogsDoNotContain(ERROR, "too many failures");
    assertThatQueueHasSize(0);
    assertThat(indexer.indexed).hasSize(10);
    assertThat(indexer.called).hasSize(10 + 2 /* retries */);
  }

  @Test
  public void failing_always_on_same_document_does_not_generate_infinite_loop() {
    EsQueueDto buggy = insertItem(FOO_TYPE, "buggy");
    IntStream.range(0, 10).forEach(i -> insertItem(FOO_TYPE, "" + i));
    advanceInTime();

    FailingAlwaysOnSameElementIndexer indexer = new FailingAlwaysOnSameElementIndexer(FOO_TYPE, buggy);
    underTest = newRecoveryIndexer(indexer);
    underTest.recover();

    assertThatLogsContain(ERROR, "Elasticsearch recovery - too many failures [1/1 documents], waiting for next run");
    assertThatQueueHasSize(1);
  }

  @Test
  public void recover_multiple_times_the_same_document() {
    EsQueueDto item1 = insertItem(FOO_TYPE, "f1");
    EsQueueDto item2 = insertItem(FOO_TYPE, item1.getDocId());
    EsQueueDto item3 = insertItem(FOO_TYPE, item1.getDocId());
    advanceInTime();

    SuccessfulFakeIndexer indexer = new SuccessfulFakeIndexer(FOO_TYPE);
    underTest = newRecoveryIndexer(indexer);
    underTest.recover();

    assertThatQueueHasSize(0);
    assertThat(indexer.called).hasSize(1);
    assertThat(indexer.called.get(0)).extracting(EsQueueDto::getUuid)
      .containsExactlyInAnyOrder(item1.getUuid(), item2.getUuid(), item3.getUuid());

    assertThatLogsContain(TRACE, "Elasticsearch recovery - processing 3 [foos/foo]");
    assertThatLogsContain(INFO, "Elasticsearch recovery - 3 documents processed [0 failures]");
  }

  private class FailingAlwaysOnSameElementIndexer implements ResilientIndexer {
    private final IndexType indexType;
    private final EsQueueDto failing;

    FailingAlwaysOnSameElementIndexer(IndexType indexType, EsQueueDto failing) {
      this.indexType = indexType;
      this.failing = failing;
    }

    @Override
    public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
      IndexingResult result = new IndexingResult();
      items.forEach(item -> {
        result.incrementRequests();
        if (!item.getUuid().equals(failing.getUuid())) {
          result.incrementSuccess();
          db.getDbClient().esQueueDao().delete(dbSession, item);
          dbSession.commit();
        }
      });
      return result;
    }

    @Override
    public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<IndexType> getIndexTypes() {
      return ImmutableSet.of(indexType);
    }
  }

  private class PartiallyFailingIndexer implements ResilientIndexer {
    private final IndexType indexType;
    private final List<EsQueueDto> called = new ArrayList<>();
    private final List<EsQueueDto> indexed = new ArrayList<>();
    private final Iterator<Integer> successfulReturns;

    PartiallyFailingIndexer(IndexType indexType, int... successfulReturns) {
      this.indexType = indexType;
      this.successfulReturns = IntStream.of(successfulReturns).iterator();
    }

    @Override
    public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
      called.addAll(items);
      int success = successfulReturns.next();
      IndexingResult result = new IndexingResult();
      items.stream().limit(success).forEach(i -> {
        db.getDbClient().esQueueDao().delete(dbSession, i);
        result.incrementSuccess();
        indexed.add(i);
      });

      rangeClosed(1, items.size()).forEach(i -> result.incrementRequests());
      dbSession.commit();
      return result;
    }

    @Override
    public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<IndexType> getIndexTypes() {
      return ImmutableSet.of(indexType);
    }
  }

  private void advanceInTime() {
    system2.setNow(system2.now() + 100_000_000L);
  }

  private void assertThatLogsContain(LoggerLevel loggerLevel, String message) {
    assertThat(logTester.logs(loggerLevel)).filteredOn(m -> m.contains(message)).isNotEmpty();
  }

  private void assertThatLogsDoNotContain(LoggerLevel loggerLevel, String message) {
    assertThat(logTester.logs(loggerLevel)).filteredOn(m -> m.contains(message)).isEmpty();
  }

  private void assertThatNoLogsFromRecovery(LoggerLevel loggerLevel) {
    assertThat(logTester.logs(loggerLevel)).filteredOn(m -> m.contains("Elasticsearch recovery - ")).isEmpty();
  }

  private void assertThatQueueHasSize(int number) {
    assertThat(db.countRowsOfTable(db.getSession(), "es_queue")).isEqualTo(number);
  }

  private RecoveryIndexer newRecoveryIndexer() {
    UserIndexer userIndexer = new UserIndexer(db.getDbClient(), es.client());
    RuleIndexer ruleIndexer = new RuleIndexer(es.client(), db.getDbClient());
    return newRecoveryIndexer(userIndexer, ruleIndexer);
  }

  private RecoveryIndexer newRecoveryIndexer(ResilientIndexer... indexers) {
    MapSettings settings = new MapSettings()
      .setProperty("sonar.search.recovery.initialDelayInMs", "0")
      .setProperty("sonar.search.recovery.delayInMs", "1")
      .setProperty("sonar.search.recovery.minAgeInMs", "1");
    return newRecoveryIndexer(settings.asConfig(), indexers);
  }

  private RecoveryIndexer newRecoveryIndexer(Configuration config, ResilientIndexer... indexers) {
    return new RecoveryIndexer(system2, config, db.getDbClient(), indexers);
  }

  private EsQueueDto insertItem(IndexType indexType, String docUuid) {
    EsQueueDto item = EsQueueDto.create(indexType.format(), docUuid);
    db.getDbClient().esQueueDao().insert(db.getSession(), item);
    db.commit();
    return item;
  }

  private class SuccessfulFakeIndexer implements ResilientIndexer {
    private final Set<IndexType> types;
    private final List<Collection<EsQueueDto>> called = new ArrayList<>();

    private SuccessfulFakeIndexer(IndexType type) {
      this.types = ImmutableSet.of(type);
    }

    @Override
    public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<IndexType> getIndexTypes() {
      return types;
    }

    @Override
    public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
      called.add(items);
      IndexingResult result = new IndexingResult();
      items.forEach(i -> result.incrementSuccess().incrementRequests());
      db.getDbClient().esQueueDao().delete(dbSession, items);
      dbSession.commit();
      return result;
    }
  }

  private class HardFailingFakeIndexer implements ResilientIndexer {
    private final Set<IndexType> types;
    private final List<Collection<EsQueueDto>> called = new ArrayList<>();

    private HardFailingFakeIndexer(IndexType type) {
      this.types = ImmutableSet.of(type);
    }

    @Override
    public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<IndexType> getIndexTypes() {
      return types;
    }

    @Override
    public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
      called.add(items);
      // MessageException is used just to reduce noise in test logs
      throw MessageException.of("BOOM");
    }
  }

  private class SoftFailingFakeIndexer implements ResilientIndexer {
    private final Set<IndexType> types;
    private final List<Collection<EsQueueDto>> called = new ArrayList<>();

    private SoftFailingFakeIndexer(IndexType type) {
      this.types = ImmutableSet.of(type);
    }

    @Override
    public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<IndexType> getIndexTypes() {
      return types;
    }

    @Override
    public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
      called.add(items);
      IndexingResult result = new IndexingResult();
      items.forEach(i -> result.incrementRequests());
      return result;
    }
  }
}
