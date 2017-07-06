/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;

import static java.util.Arrays.asList;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.sonar.api.utils.log.LoggerLevel.ERROR;
import static org.sonar.api.utils.log.LoggerLevel.INFO;
import static org.sonar.api.utils.log.LoggerLevel.TRACE;
import static org.sonar.core.util.stream.MoreCollectors.toArrayList;

public class RecoveryIndexerTest {

  private static final long PAST = 1_000L;
  private TestSystem2 system2 = new TestSystem2().setNow(PAST);
  private MapSettings emptySettings = new MapSettings();

  @Rule
  public final EsTester es = new EsTester(new UserIndexDefinition(emptySettings.asConfig()), new RuleIndexDefinition(emptySettings.asConfig()));
  @Rule
  public final DbTester db = DbTester.create(system2);
  @Rule
  public final LogTester logTester = new LogTester().setLevel(TRACE);
  @Rule
  public TestRule safeguard = new DisableOnDebug(Timeout.builder().withTimeout(60, TimeUnit.SECONDS).withLookingForStuckThread(true).build());

  private UserIndexer mockedUserIndexer = mock(UserIndexer.class);
  private RuleIndexer mockedRuleIndexer = mock(RuleIndexer.class);
  private ActiveRuleIndexer mockedActiveRuleIndexer = mock(ActiveRuleIndexer.class);
  private RecoveryIndexer underTest;

  @After
  public void tearDown() {
    if (underTest != null) {
      underTest.stop();
    }
  }

  @Test
  public void display_default_configuration_at_startup() {
    UserIndexer userIndexer = new UserIndexer(db.getDbClient(), es.client());
    RuleIndexer ruleIndexer = new RuleIndexer(es.client(), db.getDbClient());
    underTest = newRecoveryIndexer(userIndexer, ruleIndexer, emptySettings);

    underTest.start();

    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains(
      "Elasticsearch recovery - sonar.search.recovery.delayInMs=300000",
      "Elasticsearch recovery - sonar.search.recovery.minAgeInMs=300000");
  }

  @Test
  public void start_triggers_recovery_run_at_fixed_rate() throws Exception {
    MapSettings settings = new MapSettings()
      .setProperty("sonar.search.recovery.initialDelayInMs", "0")
      .setProperty("sonar.search.recovery.delayInMs", "1");
    underTest = spy(new RecoveryIndexer(system2, settings.asConfig(), db.getDbClient(), mockedUserIndexer, mockedRuleIndexer, mockedActiveRuleIndexer));
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
  public void successfully_index_RULE_records() {
    EsQueueDto item1 = createUnindexedRule();
    EsQueueDto item2 = createUnindexedRule();

    ProxyRuleIndexer ruleIndexer = new ProxyRuleIndexer();
    advanceInTime();

    underTest = newRecoveryIndexer(mockedUserIndexer, ruleIndexer);
    underTest.recover();

    assertThatQueueHasSize(0);
    assertThat(ruleIndexer.called)
      .extracting(EsQueueDto::getUuid)
      .containsExactlyInAnyOrder(item1.getUuid(), item2.getUuid());

    assertThatLogsContain(TRACE, "Elasticsearch recovery - processing 2 RULE");
    assertThatLogsContain(INFO, "Elasticsearch recovery - 4 documents processed [0 failures]");
  }

  @Test
  public void successfully_index_USER_records() {
    EsQueueDto item1 = createUnindexedUser();
    EsQueueDto item2 = createUnindexedUser();

    ProxyUserIndexer userIndexer = new ProxyUserIndexer();
    advanceInTime();
    underTest = newRecoveryIndexer(userIndexer, mockedRuleIndexer);
    underTest.recover();

    assertThatQueueHasSize(0);
    assertThat(userIndexer.called)
      .extracting(EsQueueDto::getUuid)
      .containsExactlyInAnyOrder(item1.getUuid(), item2.getUuid());

    assertThatLogsContain(TRACE, "Elasticsearch recovery - processing 2 USER");
    assertThatLogsContain(INFO, "Elasticsearch recovery - 2 documents processed [0 failures]");
  }

  @Test
  public void recent_records_are_not_recovered() {
    createUnindexedUser();
    createUnindexedUser();

    ProxyUserIndexer userIndexer = new ProxyUserIndexer();
    // do not advance in time
    underTest = newRecoveryIndexer(userIndexer, mockedRuleIndexer);
    underTest.recover();

    assertThatQueueHasSize(2);
    assertThat(userIndexer.called).isEmpty();

    assertThatLogsDoNotContain(TRACE, "Elasticsearch recovery - processing 2 USER");
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
  public void log_exception_on_recovery_failure() {
    createUnindexedUser();
    FailingOnceUserIndexer failingOnceUserIndexer = new FailingOnceUserIndexer();
    advanceInTime();

    underTest = newRecoveryIndexer(failingOnceUserIndexer, mockedRuleIndexer);
    underTest.recover();

    // No rows treated
    assertThatQueueHasSize(1);
    assertThatLogsContain(ERROR, "Elasticsearch recovery - fail to recover documents");
  }

  @Test
  public void scheduler_is_not_stopped_on_failures() throws Exception {
    createUnindexedUser();
    advanceInTime();
    FailingUserIndexer userIndexer = new FailingUserIndexer();

    underTest = newRecoveryIndexer(userIndexer, mockedRuleIndexer);
    underTest.start();

    // all runs fail, but they are still scheduled
    // -> waiting for 2 runs
    while (userIndexer.called.size() < 2) {
      Thread.sleep(1L);
    }
  }

  @Test
  public void recovery_retries_on_next_run_if_failure() throws Exception {
    createUnindexedUser();
    advanceInTime();
    FailingOnceUserIndexer userIndexer = new FailingOnceUserIndexer();

    underTest = newRecoveryIndexer(userIndexer, mockedRuleIndexer);
    underTest.start();

    // first run fails, second run succeeds
    userIndexer.counter.await(30, TimeUnit.SECONDS);

    // First we expecting an exception at first run
    // Then the second run must have treated all records
    assertThatLogsContain(ERROR, "Elasticsearch recovery - fail to recover documents");
    assertThatQueueHasSize(0);
  }

  @Test
  public void stop_run_if_too_many_failures() {
    IntStream.range(0, 10).forEach(i -> createUnindexedUser());
    advanceInTime();

    // 10 docs to process, by groups of 3.
    // The first group successfully recovers only 1 docs --> above 30% of failures --> stop run
    PartiallyFailingUserIndexer failingAboveRatioUserIndexer = new PartiallyFailingUserIndexer(1);
    MapSettings settings = new MapSettings()
      .setProperty("sonar.search.recovery.loopLimit", "3");
    underTest = newRecoveryIndexer(failingAboveRatioUserIndexer, mockedRuleIndexer, settings);
    underTest.recover();

    assertThatLogsContain(ERROR, "Elasticsearch recovery - too many failures [2/3 documents], waiting for next run");
    assertThatQueueHasSize(9);

    // The indexer must have been called once and only once.
    assertThat(failingAboveRatioUserIndexer.called).hasSize(3);
  }

  @Test
  public void do_not_stop_run_if_success_rate_is_greater_than_ratio() {
    IntStream.range(0, 10).forEach(i -> createUnindexedUser());
    advanceInTime();

    // 10 docs to process, by groups of 5.
    // Each group successfully recovers 4 docs --> below 30% of failures --> continue run
    PartiallyFailingUserIndexer failingAboveRatioUserIndexer = new PartiallyFailingUserIndexer(4, 4, 2);
    MapSettings settings = new MapSettings()
      .setProperty("sonar.search.recovery.loopLimit", "5");
    underTest = newRecoveryIndexer(failingAboveRatioUserIndexer, mockedRuleIndexer, settings);
    underTest.recover();

    assertThatLogsDoNotContain(ERROR, "too many failures");
    assertThatQueueHasSize(0);
    assertThat(failingAboveRatioUserIndexer.indexed).hasSize(10);
    assertThat(failingAboveRatioUserIndexer.called).hasSize(10 + 2 /* retries */);
  }

  @Test
  public void failing_always_on_same_document_does_not_generate_infinite_loop() {
    EsQueueDto buggy = createUnindexedUser();
    IntStream.range(0, 10).forEach(i -> createUnindexedUser());
    advanceInTime();

    FailingAlwaysOnSameElementIndexer indexer = new FailingAlwaysOnSameElementIndexer(buggy);
    underTest = newRecoveryIndexer(indexer, mockedRuleIndexer);
    underTest.recover();

    assertThatLogsContain(ERROR, "Elasticsearch recovery - too many failures [1/1 documents], waiting for next run");
    assertThatQueueHasSize(1);
  }

  @Test
  public void recover_multiple_times_the_same_document() {
    UserDto user = db.users().insertUser();
    EsQueueDto item1 = EsQueueDto.create(EsQueueDto.Type.USER, user.getLogin());
    EsQueueDto item2 = EsQueueDto.create(EsQueueDto.Type.USER, user.getLogin());
    EsQueueDto item3 = EsQueueDto.create(EsQueueDto.Type.USER, user.getLogin());
    db.getDbClient().esQueueDao().insert(db.getSession(), asList(item1, item2, item3));
    db.commit();

    ProxyUserIndexer userIndexer = new ProxyUserIndexer();
    advanceInTime();
    underTest = newRecoveryIndexer(userIndexer, mockedRuleIndexer);
    underTest.recover();

    assertThatQueueHasSize(0);
    assertThat(userIndexer.called)
      .extracting(EsQueueDto::getUuid)
      .containsExactlyInAnyOrder(item1.getUuid(), item2.getUuid(), item3.getUuid());

    assertThatLogsContain(TRACE, "Elasticsearch recovery - processing 3 USER");
    assertThatLogsContain(INFO, "Elasticsearch recovery - 3 documents processed [0 failures]");
  }

  private class ProxyUserIndexer extends UserIndexer {
    private final List<EsQueueDto> called = new ArrayList<>();

    ProxyUserIndexer() {
      super(db.getDbClient(), es.client());
    }

    @Override
    public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
      called.addAll(items);
      return super.index(dbSession, items);
    }
  }

  private class ProxyRuleIndexer extends RuleIndexer {
    private final List<EsQueueDto> called = new ArrayList<>();

    ProxyRuleIndexer() {
      super(es.client(), db.getDbClient());
    }

    @Override
    public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
      called.addAll(items);
      return super.index(dbSession, items);
    }
  }

  private class FailingUserIndexer extends UserIndexer {
    private final List<EsQueueDto> called = new ArrayList<>();

    FailingUserIndexer() {
      super(db.getDbClient(), es.client());
    }

    @Override
    public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
      called.addAll(items);
      throw new RuntimeException("boom");
    }

  }

  private class FailingOnceUserIndexer extends UserIndexer {
    private final CountDownLatch counter = new CountDownLatch(2);

    FailingOnceUserIndexer() {
      super(db.getDbClient(), es.client());
    }

    @Override
    public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
      try {
        if (counter.getCount() == 2) {
          throw new RuntimeException("boom");
        }
        return super.index(dbSession, items);
      } finally {
        counter.countDown();
      }
    }
  }

  private class FailingAlwaysOnSameElementIndexer extends UserIndexer {
    private final EsQueueDto failing;

    FailingAlwaysOnSameElementIndexer(EsQueueDto failing) {
      super(db.getDbClient(), es.client());
      this.failing = failing;
    }

    @Override
    public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
      List<EsQueueDto> filteredItems = items.stream().filter(
        i -> !i.getUuid().equals(failing.getUuid())).collect(toArrayList());
      IndexingResult result = super.index(dbSession, filteredItems);
      if (result.getTotal() == items.size() - 1) {
        // the failing item was in the items list
        result.incrementRequests();
      }

      return result;
    }
  }

  private class PartiallyFailingUserIndexer extends UserIndexer {
    private final List<EsQueueDto> called = new ArrayList<>();
    private final List<EsQueueDto> indexed = new ArrayList<>();
    private final Iterator<Integer> successfulReturns;

    PartiallyFailingUserIndexer(int... successfulReturns) {
      super(db.getDbClient(), es.client());
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

  private RecoveryIndexer newRecoveryIndexer(UserIndexer userIndexer, RuleIndexer ruleIndexer) {
    MapSettings settings = new MapSettings()
      .setProperty("sonar.search.recovery.initialDelayInMs", "0")
      .setProperty("sonar.search.recovery.delayInMs", "1")
      .setProperty("sonar.search.recovery.minAgeInMs", "1");
    return newRecoveryIndexer(userIndexer, ruleIndexer, settings);
  }

  private RecoveryIndexer newRecoveryIndexer(UserIndexer userIndexer, RuleIndexer ruleIndexer, MapSettings settings) {
    return new RecoveryIndexer(system2, settings.asConfig(), db.getDbClient(), userIndexer, ruleIndexer, mockedActiveRuleIndexer);
  }

  private EsQueueDto createUnindexedUser() {
    UserDto user = db.users().insertUser();
    EsQueueDto item = EsQueueDto.create(EsQueueDto.Type.USER, user.getLogin());
    db.getDbClient().esQueueDao().insert(db.getSession(), item);
    db.commit();

    return item;
  }

  private EsQueueDto createUnindexedRule() {
    RuleDto rule = db.rules().insertRule();
    EsQueueDto item = EsQueueDto.create(EsQueueDto.Type.RULE, rule.getKey().toString());
    db.getDbClient().esQueueDao().insert(db.getSession(), item);
    db.commit();

    return item;
  }
}
