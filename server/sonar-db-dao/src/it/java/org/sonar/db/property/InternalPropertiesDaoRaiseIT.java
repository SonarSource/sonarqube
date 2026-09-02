/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db.property;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The compare-and-set raise primitives: {@link InternalPropertiesDao#raiseTextIfGreater} and
 * {@link InternalPropertiesDao#raiseStampedMax}. Split out of {@code InternalPropertiesDaoIT}, which covers the
 * plain read and write surface and was outgrowing a single file.
 */
class InternalPropertiesDaoRaiseIT {

  private static final String EMPTY_STRING = "";
  private static final String A_KEY = "a_key";

  private final System2 system2 = mock(System2.class);

  @RegisterExtension
  private final DbTester dbTester = DbTester.create(system2);

  private final DbSession dbSession = dbTester.getSession();
  private final AuditPersister auditPersister = mock(AuditPersister.class);
  private final InternalPropertiesDao underTest = new InternalPropertiesDao(system2, auditPersister);

  @Test
  void raiseTextIfGreater_throws_IAE_if_key_is_null() {
    expectKeyNullOrEmptyIAE(() -> underTest.raiseTextIfGreater(dbSession, null, 8L));
  }

  @Test
  void raiseTextIfGreater_throws_IAE_if_key_is_empty() {
    expectKeyNullOrEmptyIAE(() -> underTest.raiseTextIfGreater(dbSession, EMPTY_STRING, 8L));
  }

  @Test
  void raiseTextIfGreater_inserts_when_absent() {
    assertThat(underTest.raiseTextIfGreater(dbSession, A_KEY, 8_000_000L)).isEqualTo(8_000_000L);
    dbSession.commit();

    assertThat(underTest.selectByKey(dbSession, A_KEY)).contains("8000000");
  }

  @Test
  void raiseTextIfGreater_raises_when_greater() {
    underTest.save(dbSession, A_KEY, "5");
    dbSession.commit();

    assertThat(underTest.raiseTextIfGreater(dbSession, A_KEY, 8L)).isEqualTo(8L);
    dbSession.commit();

    assertThat(underTest.selectByKey(dbSession, A_KEY)).contains("8");
  }

  @Test
  void raiseTextIfGreater_is_noop_when_not_greater() {
    underTest.save(dbSession, A_KEY, "8");
    dbSession.commit();

    assertThat(underTest.raiseTextIfGreater(dbSession, A_KEY, 6L)).isEqualTo(8L);
    assertThat(underTest.selectByKey(dbSession, A_KEY)).contains("8");
  }

  @Test
  void raiseTextIfGreater_overwrites_when_unparseable() {
    underTest.save(dbSession, A_KEY, "1e6");
    dbSession.commit();

    assertThat(underTest.raiseTextIfGreater(dbSession, A_KEY, 8L)).isEqualTo(8L);
    dbSession.commit();

    assertThat(underTest.selectByKey(dbSession, A_KEY)).contains("8");
  }

  @Test
  void raiseTextIfGreater_overwrites_when_empty() {
    underTest.saveAsEmpty(dbSession, A_KEY);
    dbSession.commit();

    assertThat(underTest.raiseTextIfGreater(dbSession, A_KEY, 8L)).isEqualTo(8L);
    dbSession.commit();

    assertThat(underTest.selectByKey(dbSession, A_KEY)).contains("8");
  }

  @Test
  void raiseTextIfGreater_when_insert_races_rolls_back_and_returns_winner() {
    InternalPropertiesMapper mapperMock = mock(InternalPropertiesMapper.class);
    DbSession dbSessionMock = mock(DbSession.class);
    when(dbSessionMock.getMapper(InternalPropertiesMapper.class)).thenReturn(mapperMock);

    InternalPropertyDto winner = new InternalPropertyDto();
    winner.setKey(A_KEY);
    winner.setValue("8000000");
    when(mapperMock.selectAsText(List.of(A_KEY)))
      .thenReturn(List.of())
      .thenReturn(List.of(winner));
    doThrow(RuntimeException.class).when(mapperMock).insertAsText(eq(A_KEY), anyString(), anyLong());

    assertThat(underTest.raiseTextIfGreater(dbSessionMock, A_KEY, 1_000_000L)).isEqualTo(8_000_000L);
    verify(dbSessionMock).rollback();
  }

  @Test
  void raiseTextIfGreater_when_insert_fails_and_row_still_absent_throws_with_cause() {
    InternalPropertiesMapper mapperMock = mock(InternalPropertiesMapper.class);
    DbSession dbSessionMock = mock(DbSession.class);
    when(dbSessionMock.getMapper(InternalPropertiesMapper.class)).thenReturn(mapperMock);
    when(mapperMock.selectAsText(List.of(A_KEY))).thenReturn(List.of());
    RuntimeException insertFailure = new RuntimeException("constraint");
    doThrow(insertFailure).when(mapperMock).insertAsText(eq(A_KEY), anyString(), anyLong());

    assertThatThrownBy(() -> underTest.raiseTextIfGreater(dbSessionMock, A_KEY, 1_000_000L))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(A_KEY)
      .hasCause(insertFailure);
    verify(dbSessionMock).rollback();
  }

  @Test
  void raiseStampedMax_throws_IAE_if_key_is_empty() {
    expectKeyNullOrEmptyIAE(() -> underTest.raiseStampedMax(dbSession, EMPTY_STRING, "2026-08", 8L));
  }

  @Test
  void raiseStampedMax_throws_IAE_if_stamp_is_empty() {
    assertThatThrownBy(() -> underTest.raiseStampedMax(dbSession, A_KEY, EMPTY_STRING, 8L))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("stamp");
  }

  @Test
  void raiseStampedMax_throws_IAE_if_stamp_contains_the_separator() {
    assertThatThrownBy(() -> underTest.raiseStampedMax(dbSession, A_KEY, "2026-08-31T13:00", 8L))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("stamp");
  }

  @Test
  void raiseStampedMax_inserts_when_absent() {
    assertThat(underTest.raiseStampedMax(dbSession, A_KEY, "2026-08", 8L)).isEqualTo(8L);
    dbSession.commit();

    assertThat(underTest.selectByKey(dbSession, A_KEY)).contains("2026-08:8");
  }

  @Test
  void raiseStampedMax_raises_within_the_same_stamp() {
    underTest.raiseStampedMax(dbSession, A_KEY, "2026-08", 5L);

    assertThat(underTest.raiseStampedMax(dbSession, A_KEY, "2026-08", 8L)).isEqualTo(8L);
    assertThat(underTest.selectByKey(dbSession, A_KEY)).contains("2026-08:8");
  }

  @Test
  void raiseStampedMax_is_noop_when_not_greater_within_the_same_stamp() {
    underTest.raiseStampedMax(dbSession, A_KEY, "2026-08", 8L);

    assertThat(underTest.raiseStampedMax(dbSession, A_KEY, "2026-08", 5L)).isEqualTo(8L);
    assertThat(underTest.selectByKey(dbSession, A_KEY)).contains("2026-08:8");
  }

  @Test
  void raiseStampedMax_replaces_a_value_from_an_earlier_stamp_even_when_lower() {
    underTest.raiseStampedMax(dbSession, A_KEY, "2026-08", 8L);

    assertThat(underTest.raiseStampedMax(dbSession, A_KEY, "2026-09", 5L)).isEqualTo(5L);
    assertThat(underTest.selectByKey(dbSession, A_KEY)).contains("2026-09:5");
  }

  @Test
  void raiseStampedMax_overwrites_when_unparseable() {
    underTest.save(dbSession, A_KEY, "not stamped at all");

    assertThat(underTest.raiseStampedMax(dbSession, A_KEY, "2026-08", 8L)).isEqualTo(8L);
    assertThat(underTest.selectByKey(dbSession, A_KEY)).contains("2026-08:8");
  }

  @Test
  void raiseStampedMax_is_idempotent_across_repeated_raises() {
    long first = underTest.raiseStampedMax(dbSession, A_KEY, "2026-08", 5L);
    long second = underTest.raiseStampedMax(dbSession, A_KEY, "2026-08", 9L);
    long third = underTest.raiseStampedMax(dbSession, A_KEY, "2026-08", 7L);

    assertThat(first).isEqualTo(5L);
    assertThat(second).isEqualTo(9L);
    assertThat(third).isEqualTo(9L);
    assertThat(underTest.selectByKey(dbSession, A_KEY)).contains("2026-08:9");
  }

  @Test
  void raiseStampedMax_when_contended_throws_after_exhausting_attempts() {
    InternalPropertiesMapper mapperMock = mock(InternalPropertiesMapper.class);
    DbSession dbSessionMock = mock(DbSession.class);
    when(dbSessionMock.getMapper(InternalPropertiesMapper.class)).thenReturn(mapperMock);
    when(mapperMock.selectAsText(List.of(A_KEY))).thenReturn(List.of(textDto("2026-08:5")));
    when(mapperMock.replaceValue(A_KEY, "2026-08:5", "2026-08:8")).thenReturn(0);

    assertThatThrownBy(() -> underTest.raiseStampedMax(dbSessionMock, A_KEY, "2026-08", 8L))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(A_KEY);
    verify(mapperMock, times(8)).replaceValue(A_KEY, "2026-08:5", "2026-08:8");
  }

  @Test
  void raiseStampedMax_retries_until_a_contended_write_lands() {
    InternalPropertiesMapper mapperMock = mock(InternalPropertiesMapper.class);
    DbSession dbSessionMock = mock(DbSession.class);
    when(dbSessionMock.getMapper(InternalPropertiesMapper.class)).thenReturn(mapperMock);
    when(mapperMock.selectAsText(List.of(A_KEY))).thenReturn(List.of(textDto("2026-08:5")));
    when(mapperMock.replaceValue(A_KEY, "2026-08:5", "2026-08:8")).thenReturn(0, 0, 1);

    assertThat(underTest.raiseStampedMax(dbSessionMock, A_KEY, "2026-08", 8L)).isEqualTo(8L);
    verify(mapperMock, times(3)).replaceValue(A_KEY, "2026-08:5", "2026-08:8");
  }

  @Test
  void raiseStampedMax_when_a_concurrent_writer_raised_higher_returns_the_stored_value() {
    InternalPropertiesMapper mapperMock = mock(InternalPropertiesMapper.class);
    DbSession dbSessionMock = mock(DbSession.class);
    when(dbSessionMock.getMapper(InternalPropertiesMapper.class)).thenReturn(mapperMock);
    when(mapperMock.selectAsText(List.of(A_KEY)))
      .thenReturn(List.of(textDto("2026-08:5")))
      .thenReturn(List.of(textDto("2026-08:9")));
    when(mapperMock.replaceValue(A_KEY, "2026-08:5", "2026-08:8")).thenReturn(0);

    assertThat(underTest.raiseStampedMax(dbSessionMock, A_KEY, "2026-08", 8L)).isEqualTo(9L);
  }

  @Test
  void selectStampedMax_is_empty_when_absent() {
    assertThat(underTest.selectStampedMax(dbSession, A_KEY, "2026-08")).isEmpty();
  }

  @Test
  void selectStampedMax_is_empty_when_the_stamp_belongs_to_an_earlier_window() {
    underTest.raiseStampedMax(dbSession, A_KEY, "2026-08", 8L);

    assertThat(underTest.selectStampedMax(dbSession, A_KEY, "2026-09")).isEmpty();
  }

  @Test
  void selectStampedMax_is_empty_when_the_row_is_unparseable() {
    underTest.save(dbSession, A_KEY, "not stamped at all");

    assertThat(underTest.selectStampedMax(dbSession, A_KEY, "2026-08")).isEmpty();
  }

  @Test
  void selectStampedMax_returns_the_stored_maximum_for_the_current_stamp() {
    underTest.raiseStampedMax(dbSession, A_KEY, "2026-08", 8L);

    assertThat(underTest.selectStampedMax(dbSession, A_KEY, "2026-08")).hasValue(8L);
  }

  @Test
  void raiseTextIfGreater_when_attempts_exhausted_and_stored_is_at_least_requested_returns_stored() {
    InternalPropertiesMapper mapperMock = mock(InternalPropertiesMapper.class);
    DbSession dbSessionMock = mock(DbSession.class);
    when(dbSessionMock.getMapper(InternalPropertiesMapper.class)).thenReturn(mapperMock);
    AtomicInteger selects = new AtomicInteger();
    when(mapperMock.selectAsText(List.of(A_KEY))).thenAnswer(invocation -> {
      if (selects.incrementAndGet() <= 8) {
        return List.of(textDto("5"));
      }
      return List.of(textDto("10"));
    });
    when(mapperMock.replaceValue(A_KEY, "5", "8")).thenReturn(0);

    assertThat(underTest.raiseTextIfGreater(dbSessionMock, A_KEY, 8L)).isEqualTo(10L);
    verify(mapperMock, times(8)).replaceValue(A_KEY, "5", "8");
  }

  @Test
  void raiseTextIfGreater_when_attempts_exhausted_and_stored_is_below_throws() {
    InternalPropertiesMapper mapperMock = mock(InternalPropertiesMapper.class);
    DbSession dbSessionMock = mock(DbSession.class);
    when(dbSessionMock.getMapper(InternalPropertiesMapper.class)).thenReturn(mapperMock);
    when(mapperMock.selectAsText(List.of(A_KEY))).thenReturn(List.of(textDto("5")));
    when(mapperMock.replaceValue(A_KEY, "5", "8")).thenReturn(0);

    assertThatThrownBy(() -> underTest.raiseTextIfGreater(dbSessionMock, A_KEY, 8L))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(A_KEY)
      .hasMessageContaining("8")
      .hasMessageContaining("stored=5");
  }

  @Test
  void raiseTextIfGreater_when_concurrent_raises_keeps_the_max() throws Exception {
    long[] values = {1_000_000L, 8_000_000L, 3_000_000L, 6_000_000L, 8_000_000L, 2_000_000L, 7_000_000L, 4_000_000L};
    int threads = values.length;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CyclicBarrier start = new CyclicBarrier(threads);
    List<Future<Long>> results = new ArrayList<>();
    try {
      for (long value : values) {
        Callable<Long> task = () -> {
          start.await(5, TimeUnit.SECONDS);
          try (DbSession session = dbTester.getDbClient().openSession(false)) {
            long result = underTest.raiseTextIfGreater(session, A_KEY, value);
            session.commit();
            return result;
          }
        };
        results.add(pool.submit(task));
      }
      for (int i = 0; i < threads; i++) {
        assertThat(results.get(i).get(5, TimeUnit.SECONDS)).isBetween(values[i], 8_000_000L);
      }
    } finally {
      pool.shutdownNow();
    }

    try (DbSession session = dbTester.getDbClient().openSession(false)) {
      assertThat(underTest.selectByKey(session, A_KEY)).contains("8000000");
    }
  }

  private static InternalPropertyDto textDto(String value) {
    InternalPropertyDto dto = new InternalPropertyDto();
    dto.setKey(A_KEY);
    dto.setValue(value);
    return dto;
  }

  private void expectKeyNullOrEmptyIAE(ThrowingCallable callback) {
    assertThatThrownBy(callback)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("key can't be null nor empty");
  }
}
