/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.PropertyNewValue;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.property.InternalPropertiesDao.LOCK_NAME_MAX_LENGTH;

class InternalPropertiesDaoIT {

  private static final String EMPTY_STRING = "";
  private static final String A_KEY = "a_key";
  private static final String ANOTHER_KEY = "another_key";
  private static final long DATE_1 = 1_500_000_000_000L;
  private static final long DATE_2 = 1_600_000_000_000L;
  private static final String VALUE_SMALL = "some small value";
  private static final String OTHER_VALUE_SMALL = "other small value";
  private static final String VALUE_SIZE_4000 = String.format("%1$4000.4000s", "*");
  private static final String VALUE_SIZE_4001 = VALUE_SIZE_4000 + "P";
  private static final String OTHER_VALUE_SIZE_4001 = VALUE_SIZE_4000 + "D";

  private final System2 system2 = mock(System2.class);
  private final String DEFAULT_PROJECT_TEMPLATE = "defaultTemplate.prj";

  @RegisterExtension
  private final DbTester dbTester = DbTester.create(system2);

  private final DbSession dbSession = dbTester.getSession();
  private final AuditPersister auditPersister = mock(AuditPersister.class);
  private final InternalPropertiesDao underTest = new InternalPropertiesDao(system2, auditPersister);
  private final ArgumentCaptor<PropertyNewValue> newValueCaptor = ArgumentCaptor.forClass(PropertyNewValue.class);

  @BeforeEach
  void setUp() {
    when(auditPersister.isTrackedProperty(DEFAULT_PROJECT_TEMPLATE)).thenReturn(true);
  }

  @Test
  void save_throws_IAE_if_key_is_null() {
    expectKeyNullOrEmptyIAE(() -> underTest.save(dbSession, null, VALUE_SMALL));
  }

  @Test
  void save_throws_IAE_if_key_is_empty() {
    expectKeyNullOrEmptyIAE(() -> underTest.save(dbSession, EMPTY_STRING, VALUE_SMALL));
  }

  @Test
  void save_throws_IAE_if_value_is_null() {
    expectValueNullOrEmptyIAE(() -> underTest.save(dbSession, A_KEY, null));
  }

  @Test
  void save_throws_IAE_if_value_is_empty() {
    expectValueNullOrEmptyIAE(() -> underTest.save(dbSession, A_KEY, EMPTY_STRING));
  }

  @Test
  void save_persists_value_in_varchar_if_less_than_4000() {
    when(system2.now()).thenReturn(DATE_2);
    underTest.save(dbSession, A_KEY, VALUE_SMALL);

    assertThatInternalProperty(A_KEY)
      .hasTextValue(VALUE_SMALL)
      .hasCreatedAt(DATE_2);
  }

  @Test
  void delete_removes_value() {
    underTest.save(dbSession, A_KEY, VALUE_SMALL);
    dbSession.commit();
    assertThat(dbTester.countRowsOfTable("internal_properties")).isOne();
    clearInvocations(auditPersister);

    underTest.delete(dbSession, A_KEY);
    dbSession.commit();

    assertThat(dbTester.countRowsOfTable("internal_properties")).isZero();
    verify(auditPersister).isTrackedProperty(A_KEY);
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  void delete_audits_if_value_was_deleted_and_it_is_tracked_key() {
    underTest.save(dbSession, DEFAULT_PROJECT_TEMPLATE, VALUE_SMALL);
    clearInvocations(auditPersister);
    underTest.delete(dbSession, DEFAULT_PROJECT_TEMPLATE);
    verify(auditPersister).deleteProperty(any(), newValueCaptor.capture(), eq(false));
    assertAuditValue(DEFAULT_PROJECT_TEMPLATE, null);
  }

  @Test
  void delete_audits_does_not_audit_if_nothing_was_deleted() {
    underTest.delete(dbSession, DEFAULT_PROJECT_TEMPLATE);
    verifyNoInteractions(auditPersister);
  }

  @Test
  void save_audits_if_key_is_tracked() {
    underTest.save(dbSession, DEFAULT_PROJECT_TEMPLATE, VALUE_SMALL);
    verify(auditPersister).addProperty(any(), newValueCaptor.capture(), eq(false));
    assertAuditValue(DEFAULT_PROJECT_TEMPLATE, VALUE_SMALL);
  }

  @Test
  void save_audits_update_if_key_is_tracked_and_updated() {
    underTest.save(dbSession, DEFAULT_PROJECT_TEMPLATE, "first value");

    Mockito.clearInvocations(auditPersister);

    underTest.save(dbSession, DEFAULT_PROJECT_TEMPLATE, VALUE_SMALL);
    verify(auditPersister).updateProperty(any(), newValueCaptor.capture(), eq(false));
    assertAuditValue(DEFAULT_PROJECT_TEMPLATE, VALUE_SMALL);
  }

  @Test
  void save_persists_value_in_varchar_if_4000() {
    when(system2.now()).thenReturn(DATE_1);
    underTest.save(dbSession, A_KEY, VALUE_SIZE_4000);

    assertThatInternalProperty(A_KEY)
      .hasTextValue(VALUE_SIZE_4000)
      .hasCreatedAt(DATE_1);
  }

  @Test
  void save_persists_value_in_varchar_if_more_than_4000() {
    when(system2.now()).thenReturn(DATE_2);

    underTest.save(dbSession, A_KEY, VALUE_SIZE_4001);

    assertThatInternalProperty(A_KEY)
      .hasClobValue(VALUE_SIZE_4001)
      .hasCreatedAt(DATE_2);
  }

  @Test
  void save_persists_new_value_in_varchar_if_4000_when_old_one_was_in_varchar() {
    when(system2.now()).thenReturn(DATE_1, DATE_2);

    underTest.save(dbSession, A_KEY, VALUE_SMALL);
    assertThatInternalProperty(A_KEY)
      .hasTextValue(VALUE_SMALL)
      .hasCreatedAt(DATE_1);

    underTest.save(dbSession, A_KEY, VALUE_SIZE_4000);
    assertThatInternalProperty(A_KEY)
      .hasTextValue(VALUE_SIZE_4000)
      .hasCreatedAt(DATE_2);
  }

  @Test
  void save_persists_new_value_in_clob_if_more_than_4000_when_old_one_was_in_varchar() {
    when(system2.now()).thenReturn(DATE_1, DATE_2);

    underTest.save(dbSession, A_KEY, VALUE_SMALL);
    assertThatInternalProperty(A_KEY)
      .hasTextValue(VALUE_SMALL)
      .hasCreatedAt(DATE_1);

    underTest.save(dbSession, A_KEY, VALUE_SIZE_4001);
    assertThatInternalProperty(A_KEY)
      .hasClobValue(VALUE_SIZE_4001)
      .hasCreatedAt(DATE_2);
  }

  @Test
  void save_persists_new_value_in_varchar_if_less_than_4000_when_old_one_was_in_clob() {
    when(system2.now()).thenReturn(DATE_1, DATE_2);

    underTest.save(dbSession, A_KEY, VALUE_SIZE_4001);
    assertThatInternalProperty(A_KEY)
      .hasClobValue(VALUE_SIZE_4001)
      .hasCreatedAt(DATE_1);

    underTest.save(dbSession, A_KEY, VALUE_SMALL);
    assertThatInternalProperty(A_KEY)
      .hasTextValue(VALUE_SMALL)
      .hasCreatedAt(DATE_2);
  }

  @Test
  void save_persists_new_value_in_clob_if_more_than_4000_when_old_one_was_in_clob() {
    when(system2.now()).thenReturn(DATE_1, DATE_2);

    String oldValue = VALUE_SIZE_4001 + "blabla";
    underTest.save(dbSession, A_KEY, oldValue);
    assertThatInternalProperty(A_KEY)
      .hasClobValue(oldValue)
      .hasCreatedAt(DATE_1);

    underTest.save(dbSession, A_KEY, VALUE_SIZE_4001);
    assertThatInternalProperty(A_KEY)
      .hasClobValue(VALUE_SIZE_4001)
      .hasCreatedAt(DATE_2);
  }

  @Test
  void saveAsEmpty_throws_IAE_if_key_is_null() {
    expectKeyNullOrEmptyIAE(() -> underTest.saveAsEmpty(dbSession, null));
  }

  @Test
  void saveAsEmpty_throws_IAE_if_key_is_empty() {
    expectKeyNullOrEmptyIAE(() -> underTest.saveAsEmpty(dbSession, EMPTY_STRING));
  }

  @Test
  void saveAsEmpty_persist_property_without_textvalue_nor_clob_value() {
    when(system2.now()).thenReturn(DATE_2);

    underTest.saveAsEmpty(dbSession, A_KEY);

    assertThatInternalProperty(A_KEY)
      .isEmpty()
      .hasCreatedAt(DATE_2);
  }

  @Test
  void saveAsEmpty_audits_if_key_is_tracked() {
    underTest.saveAsEmpty(dbSession, DEFAULT_PROJECT_TEMPLATE);
    verify(auditPersister).addProperty(any(), newValueCaptor.capture(), eq(false));
    assertAuditValue(DEFAULT_PROJECT_TEMPLATE, "");
  }

  @Test
  void saveAsEmpty_audits_update_if_key_is_tracked_and_updated() {
    underTest.save(dbSession, DEFAULT_PROJECT_TEMPLATE, "first value");
    Mockito.clearInvocations(auditPersister);
    underTest.saveAsEmpty(dbSession, DEFAULT_PROJECT_TEMPLATE);
    verify(auditPersister).updateProperty(any(), newValueCaptor.capture(), eq(false));
    assertAuditValue(DEFAULT_PROJECT_TEMPLATE, "");
  }

  @Test
  void saveAsEmpty_persist_property_without_textvalue_nor_clob_value_when_old_value_was_in_varchar() {
    when(system2.now()).thenReturn(DATE_1, DATE_2);

    underTest.save(dbSession, A_KEY, VALUE_SMALL);
    assertThatInternalProperty(A_KEY)
      .hasTextValue(VALUE_SMALL)
      .hasCreatedAt(DATE_1);

    underTest.saveAsEmpty(dbSession, A_KEY);
    assertThatInternalProperty(A_KEY)
      .isEmpty()
      .hasCreatedAt(DATE_2);
  }

  @Test
  void saveAsEmpty_persist_property_without_textvalue_nor_clob_value_when_old_value_was_in_clob() {
    when(system2.now()).thenReturn(DATE_2, DATE_1);

    underTest.save(dbSession, A_KEY, VALUE_SIZE_4001);
    assertThatInternalProperty(A_KEY)
      .hasClobValue(VALUE_SIZE_4001)
      .hasCreatedAt(DATE_2);

    underTest.saveAsEmpty(dbSession, A_KEY);
    assertThatInternalProperty(A_KEY)
      .isEmpty()
      .hasCreatedAt(DATE_1);
  }

  @Test
  void selectByKey_throws_IAE_when_key_is_null() {
    expectKeyNullOrEmptyIAE(() -> underTest.selectByKey(dbSession, null));
  }

  @Test
  void selectByKey_throws_IAE_when_key_is_empty() {
    expectKeyNullOrEmptyIAE(() -> underTest.selectByKey(dbSession, EMPTY_STRING));
  }

  @Test
  void selectByKey_returns_empty_optional_when_property_does_not_exist_in_DB() {
    assertThat(underTest.selectByKey(dbSession, A_KEY)).isEmpty();
  }

  @Test
  void selectByKey_returns_empty_string_when_property_is_empty_in_DB() {
    underTest.saveAsEmpty(dbSession, A_KEY);

    assertThat(underTest.selectByKey(dbSession, A_KEY)).contains(EMPTY_STRING);
  }

  @Test
  void selectByKey_returns_value_when_property_has_value_stored_in_varchar() {
    underTest.save(dbSession, A_KEY, VALUE_SMALL);

    assertThat(underTest.selectByKey(dbSession, A_KEY)).contains(VALUE_SMALL);
  }

  @Test
  void selectByKey_returns_value_when_property_has_value_stored_in_clob() {
    underTest.save(dbSession, A_KEY, VALUE_SIZE_4001);

    assertThat(underTest.selectByKey(dbSession, A_KEY)).contains(VALUE_SIZE_4001);
  }

  @Test
  void selectByKeys_returns_empty_map_if_keys_is_null() {
    assertThat(underTest.selectByKeys(dbSession, null)).isEmpty();
  }

  @Test
  void selectByKeys_returns_empty_map_if_keys_is_empty() {
    assertThat(underTest.selectByKeys(dbSession, Collections.emptySet())).isEmpty();
  }

  @Test
  void selectByKeys_throws_IAE_when_keys_contains_null() {
    Set<String> keysIncludingANull = Stream.of(
        IntStream.range(0, 10).mapToObj(i -> "b_" + i),
        Stream.of((String) null),
        IntStream.range(0, 10).mapToObj(i -> "a_" + i))
      .flatMap(s -> s)
      .collect(Collectors.toSet());

    expectKeyNullOrEmptyIAE(() -> underTest.selectByKeys(dbSession, keysIncludingANull));
  }

  @Test
  void selectByKeys_throws_IAE_when_keys_contains_empty_string() {
    Random random = new Random();
    Set<String> keysIncludingAnEmptyString = Stream.of(
        IntStream.range(0, random.nextInt(10)).mapToObj(i -> "b_" + i),
        Stream.of(""),
        IntStream.range(0, random.nextInt(10)).mapToObj(i -> "a_" + i))
      .flatMap(s -> s)
      .collect(Collectors.toSet());

    expectKeyNullOrEmptyIAE(() -> underTest.selectByKeys(dbSession, keysIncludingAnEmptyString));
  }

  @Test
  void selectByKeys_returns_empty_optional_when_property_does_not_exist_in_DB() {
    assertThat(underTest.selectByKeys(dbSession, ImmutableSet.of(A_KEY, ANOTHER_KEY)))
      .containsOnly(
        entry(A_KEY, Optional.empty()),
        entry(ANOTHER_KEY, Optional.empty()));
  }

  @Test
  void selectByKeys_returns_empty_string_when_property_is_empty_in_DB() {
    underTest.saveAsEmpty(dbSession, A_KEY);
    underTest.saveAsEmpty(dbSession, ANOTHER_KEY);

    assertThat(underTest.selectByKeys(dbSession, ImmutableSet.of(A_KEY, ANOTHER_KEY)))
      .containsOnly(
        entry(A_KEY, Optional.of("")),
        entry(ANOTHER_KEY, Optional.of("")));
  }

  @Test
  void selectByKeys_returns_value_when_property_has_value_stored_in_varchar() {
    underTest.save(dbSession, A_KEY, VALUE_SMALL);
    underTest.save(dbSession, ANOTHER_KEY, OTHER_VALUE_SMALL);

    assertThat(underTest.selectByKeys(dbSession, ImmutableSet.of(A_KEY, ANOTHER_KEY)))
      .containsOnly(
        entry(A_KEY, Optional.of(VALUE_SMALL)),
        entry(ANOTHER_KEY, Optional.of(OTHER_VALUE_SMALL)));
  }

  @Test
  void selectByKeys_returns_values_when_properties_has_value_stored_in_clob() {
    underTest.save(dbSession, A_KEY, VALUE_SIZE_4001);
    underTest.save(dbSession, ANOTHER_KEY, OTHER_VALUE_SIZE_4001);

    assertThat(underTest.selectByKeys(dbSession, ImmutableSet.of(A_KEY, ANOTHER_KEY)))
      .containsOnly(
        entry(A_KEY, Optional.of(VALUE_SIZE_4001)),
        entry(ANOTHER_KEY, Optional.of(OTHER_VALUE_SIZE_4001)));
  }

  @Test
  void selectByKeys_queries_only_clob_properties_with_clob_SQL_query() {
    underTest.saveAsEmpty(dbSession, A_KEY);
    underTest.save(dbSession, "key2", VALUE_SMALL);
    underTest.save(dbSession, "key3", VALUE_SIZE_4001);
    Set<String> keys = ImmutableSet.of(A_KEY, "key2", "key3", "non_existent_key");
    List<InternalPropertyDto> allInternalPropertyDtos =
      dbSession.getMapper(InternalPropertiesMapper.class).selectAsText(ImmutableList.copyOf(keys));
    List<InternalPropertyDto> clobPropertyDtos = dbSession.getMapper(InternalPropertiesMapper.class).selectAsClob(ImmutableList.of("key3"));

    InternalPropertiesMapper mapperMock = mock(InternalPropertiesMapper.class);
    DbSession dbSessionMock = mock(DbSession.class);
    when(dbSessionMock.getMapper(InternalPropertiesMapper.class)).thenReturn(mapperMock);
    when(mapperMock.selectAsText(ImmutableList.copyOf(keys)))
      .thenReturn(allInternalPropertyDtos);
    when(mapperMock.selectAsClob(ImmutableList.of("key3")))
      .thenReturn(clobPropertyDtos);

    underTest.selectByKeys(dbSessionMock, keys);

    verify(mapperMock).selectAsText(ImmutableList.copyOf(keys));
    verify(mapperMock).selectAsClob(ImmutableList.of("key3"));
    verifyNoMoreInteractions(mapperMock);
  }

  @Test
  void tryLock_succeeds_if_lock_did_not_exist() {
    long now = new Random().nextInt();
    when(system2.now()).thenReturn(now);
    assertThat(underTest.tryLock(dbSession, A_KEY, 60)).isTrue();

    assertThat(underTest.selectByKey(dbSession, propertyKeyOf(A_KEY))).contains(String.valueOf(now));
  }

  @Test
  void tryLock_succeeds_if_lock_acquired_before_lease_duration() {
    int lockDurationSeconds = 60;

    long before = new Random().nextInt();
    when(system2.now()).thenReturn(before);
    assertThat(underTest.tryLock(dbSession, A_KEY, lockDurationSeconds)).isTrue();

    long now = before + lockDurationSeconds * 1000;
    when(system2.now()).thenReturn(now);
    assertThat(underTest.tryLock(dbSession, A_KEY, lockDurationSeconds)).isTrue();

    assertThat(underTest.selectByKey(dbSession, propertyKeyOf(A_KEY))).contains(String.valueOf(now));
  }

  @Test
  void tryLock_fails_if_lock_acquired_within_lease_duration() {
    long now = new Random().nextInt();
    when(system2.now()).thenReturn(now);
    assertThat(underTest.tryLock(dbSession, A_KEY, 60)).isTrue();
    assertThat(underTest.tryLock(dbSession, A_KEY, 60)).isFalse();

    assertThat(underTest.selectByKey(dbSession, propertyKeyOf(A_KEY))).contains(String.valueOf(now));
  }

  @Test
  void tryLock_fails_if_it_would_insert_concurrently() {
    String name = secure().nextAlphabetic(5);
    String propertyKey = propertyKeyOf(name);

    long now = new Random().nextInt();
    when(system2.now()).thenReturn(now);
    assertThat(underTest.tryLock(dbSession, name, 60)).isTrue();

    InternalPropertiesMapper mapperMock = mock(InternalPropertiesMapper.class);
    DbSession dbSessionMock = mock(DbSession.class);
    when(dbSessionMock.getMapper(InternalPropertiesMapper.class)).thenReturn(mapperMock);
    when(mapperMock.selectAsText(ImmutableList.of(propertyKey)))
      .thenReturn(ImmutableList.of());
    doThrow(RuntimeException.class).when(mapperMock).insertAsText(eq(propertyKey), anyString(), anyLong());

    assertThat(underTest.tryLock(dbSessionMock, name, 60)).isFalse();

    assertThat(underTest.selectByKey(dbSession, propertyKey)).contains(String.valueOf(now));
  }

  @Test
  void tryLock_fails_if_concurrent_caller_succeeded_first() {
    int lockDurationSeconds = 60;
    String name = secure().nextAlphabetic(5);
    String propertyKey = propertyKeyOf(name);

    long now = new Random().nextInt(4_889_989);
    long oldTimestamp = now - lockDurationSeconds * 1000;
    when(system2.now()).thenReturn(oldTimestamp);
    assertThat(underTest.tryLock(dbSession, name, lockDurationSeconds)).isTrue();
    when(system2.now()).thenReturn(now);

    InternalPropertiesMapper mapperMock = mock(InternalPropertiesMapper.class);
    DbSession dbSessionMock = mock(DbSession.class);
    when(dbSessionMock.getMapper(InternalPropertiesMapper.class)).thenReturn(mapperMock);
    InternalPropertyDto dto = new InternalPropertyDto();
    dto.setKey(propertyKey);
    dto.setValue(String.valueOf(oldTimestamp - 1));
    when(mapperMock.selectAsText(ImmutableList.of(propertyKey)))
      .thenReturn(ImmutableList.of(dto));

    assertThat(underTest.tryLock(dbSessionMock, name, lockDurationSeconds)).isFalse();

    assertThat(underTest.selectByKey(dbSession, propertyKey)).contains(String.valueOf(oldTimestamp));
  }

  @Test
  void tryLock_throws_IAE_if_lock_name_is_empty() {
    assertThatThrownBy(() -> underTest.tryLock(dbSession, "", 60))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("lock name can't be empty");
  }

  @Test
  void tryLock_throws_IAE_if_lock_name_length_is_too_long() {
    String tooLongName = secure().nextAlphabetic(LOCK_NAME_MAX_LENGTH + 1);

    assertThatThrownBy(() -> underTest.tryLock(dbSession, tooLongName, 60))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("lock name is too long");
  }

  private void assertAuditValue(String key, @Nullable String value) {
    verify(auditPersister).isTrackedProperty(key);
    PropertyNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue, PropertyNewValue::getUserUuid,
        PropertyNewValue::getUserLogin)
      .containsExactly(key, value, null, null);
  }

  private static String propertyKeyOf(String lockName) {
    return "lock." + lockName;
  }

  private void expectKeyNullOrEmptyIAE(ThrowingCallable callback) {
    assertThatThrownBy(callback)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("key can't be null nor empty");
  }

  private void expectValueNullOrEmptyIAE(ThrowingCallable callback) {
    assertThatThrownBy(callback)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("value can't be null nor empty");
  }

  private InternalPropertyAssert assertThatInternalProperty(String key) {
    return new InternalPropertyAssert(dbTester, dbSession, key);
  }

  private static class InternalPropertyAssert extends AbstractAssert<InternalPropertyAssert, InternalProperty> {

    private InternalPropertyAssert(DbTester dbTester, DbSession dbSession, String internalPropertyKey) {
      super(asInternalProperty(dbTester, dbSession, internalPropertyKey), InternalPropertyAssert.class);
    }

    private static InternalProperty asInternalProperty(DbTester dbTester, DbSession dbSession, String internalPropertyKey) {
      Map<String, Object> row = dbTester.selectFirst(
        dbSession,
        "select" +
          " is_empty as \"isEmpty\", text_value as \"textValue\", clob_value as \"clobValue\", created_at as \"createdAt\"" +
          " from internal_properties" +
          " where kee='" + internalPropertyKey + "'");
      return new InternalProperty(
        isEmpty(row),
        (String) row.get("textValue"),
        (String) row.get("clobValue"),
        (Long) row.get("createdAt"));
    }

    private static Boolean isEmpty(Map<String, Object> row) {
      Object flag = row.get("isEmpty");
      if (flag instanceof Boolean) {
        return (Boolean) flag;
      }
      if (flag instanceof Long) {
        Long longBoolean = (Long) flag;
        return longBoolean.equals(1L);
      }
      throw new IllegalArgumentException("Unsupported object type returned for column \"isEmpty\": " + flag.getClass());
    }

    InternalPropertyAssert isEmpty() {
      isNotNull();

      if (!Objects.equals(actual.empty(), TRUE)) {
        failWithMessage("Expected Internal property to have column IS_EMPTY to be <%s> but was <%s>", true, actual.empty());
      }
      if (actual.textValue() != null) {
        failWithMessage("Expected Internal property to have column TEXT_VALUE to be null but was <%s>", actual.textValue());
      }
      if (actual.clobValue() != null) {
        failWithMessage("Expected Internal property to have column CLOB_VALUE to be null but was <%s>", actual.clobValue());
      }

      return this;
    }

    InternalPropertyAssert hasTextValue(String expected) {
      isNotNull();

      if (!Objects.equals(actual.textValue(), expected)) {
        failWithMessage("Expected Internal property to have column TEXT_VALUE to be <%s> but was <%s>", true, actual.textValue());
      }
      if (actual.clobValue() != null) {
        failWithMessage("Expected Internal property to have column CLOB_VALUE to be null but was <%s>", actual.clobValue());
      }
      if (!Objects.equals(actual.empty(), FALSE)) {
        failWithMessage("Expected Internal property to have column IS_EMPTY to be <%s> but was <%s>", false, actual.empty());
      }

      return this;
    }

    InternalPropertyAssert hasClobValue(String expected) {
      isNotNull();

      if (!Objects.equals(actual.clobValue(), expected)) {
        failWithMessage("Expected Internal property to have column CLOB_VALUE to be <%s> but was <%s>", true, actual.clobValue());
      }
      if (actual.textValue() != null) {
        failWithMessage("Expected Internal property to have column TEXT_VALUE to be null but was <%s>", actual.textValue());
      }
      if (!Objects.equals(actual.empty(), FALSE)) {
        failWithMessage("Expected Internal property to have column IS_EMPTY to be <%s> but was <%s>", false, actual.empty());
      }

      return this;
    }

    InternalPropertyAssert hasCreatedAt(long expected) {
      isNotNull();

      if (!Objects.equals(actual.createdAt(), expected)) {
        failWithMessage("Expected Internal property to have column CREATED_AT to be <%s> but was <%s>", expected, actual.createdAt());
      }

      return this;
    }

  }

 private record InternalProperty(Boolean empty, String textValue, String clobValue, Long createdAt) {
  private InternalProperty(@Nullable Boolean empty, @Nullable String textValue, @Nullable String clobValue, @Nullable Long createdAt) {
   this.empty = empty;
   this.textValue = textValue;
   this.clobValue = clobValue;
   this.createdAt = createdAt;
  }

  @Override
  @CheckForNull
  public Boolean empty() {
   return empty;
  }

  @Override
  @CheckForNull
  public String textValue() {
   return textValue;
  }

  @Override
  @CheckForNull
  public String clobValue() {
   return clobValue;
  }

  @Override
  @CheckForNull
  public Long createdAt() {
   return createdAt;
  }
 }
}
