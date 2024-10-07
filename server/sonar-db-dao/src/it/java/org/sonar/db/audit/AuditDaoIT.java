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
package org.sonar.db.audit;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.audit.AuditDao.EXCEEDED_LENGTH;

class AuditDaoIT {

  private static final long NOW = 1000000L;
  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);
  @RegisterExtension
  private final DbTester db = DbTester.create(system2);
  private final DbSession dbSession = db.getSession();

  private final AuditDao testAuditDao = new AuditDao(system2, UuidFactoryImpl.INSTANCE);

  @Test
  void selectByPeriodPaginated_10001EntriesInserted_defaultPageSizeEntriesReturned() {
    prepareRowsWithDeterministicCreatedAt(10001);

    List<AuditDto> auditDtos = testAuditDao.selectByPeriodPaginated(dbSession, 1, 20000, 1);

    assertThat(auditDtos).hasSize(10_000);
  }

  @Test
  void selectByPeriodPaginated_10001EntriesInserted_querySecondPageReturns1Item() {
    prepareRowsWithDeterministicCreatedAt(10001);

    List<AuditDto> auditDtos = testAuditDao.selectByPeriodPaginated(dbSession, 1, 20000, 2);

    assertThat(auditDtos.size()).isOne();
  }

  @Test
  void purge_has_limit() {
    prepareRowsWithDeterministicCreatedAt(100_001);
    long purged = testAuditDao.deleteBefore(dbSession, 200_000);
    assertThat(purged).isEqualTo(100_000);
    assertThat(db.countRowsOfTable(dbSession, "audits")).isOne();
    assertThat(testAuditDao.selectOlderThan(dbSession, 100_002))
      .extracting(AuditDto::getCreatedAt)
      .containsOnly(100_001L);
  }

  @Test
  void purge_with_threshold() {
    prepareRowsWithDeterministicCreatedAt(100_000);
    long purged = testAuditDao.deleteBefore(dbSession, 50_000);
    assertThat(purged).isEqualTo(49_999);
    assertThat(db.countRowsOfTable(dbSession, "audits")).isEqualTo(50_001);
    assertThat(testAuditDao.selectOlderThan(dbSession, 100_000))
      .hasSize(50_000)
      .allMatch(a -> a.getCreatedAt() >= 50_000);
  }

  @Test
  void selectByPeriodPaginated_100EntriesInserted_100EntriesReturned() {
    prepareRowsWithDeterministicCreatedAt(100);

    List<AuditDto> auditDtos = testAuditDao.selectByPeriodPaginated(dbSession, 1, 101, 1);

    assertThat(auditDtos).hasSize(100);
  }

  @Test
  void insert_doNotSetACreatedAtIfAlreadySet() {
    AuditDto auditDto = AuditTesting.newAuditDto();
    auditDto.setCreatedAt(1041375600000L);

    testAuditDao.insert(dbSession, auditDto);

    List<AuditDto> auditDtos = testAuditDao.selectByPeriodPaginated(dbSession, 1041375500000L, 1041375700000L, 1);
    AuditDto storedAuditDto = auditDtos.get(0);
    assertThat(storedAuditDto.getCreatedAt()).isEqualTo(auditDto.getCreatedAt());
  }

  @Test
  void insert_setACreatedAtIfAlreadySet() {
    AuditDto auditDto = AuditTesting.newAuditDto();
    auditDto.setCreatedAt(0);

    testAuditDao.insert(dbSession, auditDto);

    assertThat(auditDto.getCreatedAt()).isNotZero();
  }

  @Test
  void insert_doNotSetAUUIDIfAlreadySet() {
    AuditDto auditDto = AuditTesting.newAuditDto();
    auditDto.setUuid("myuuid");
    auditDto.setCreatedAt(1041375600000L);

    testAuditDao.insert(dbSession, auditDto);

    List<AuditDto> auditDtos = testAuditDao.selectByPeriodPaginated(dbSession, 1041375500000L, 1041375700000L, 1);
    AuditDto storedAuditDto = auditDtos.get(0);
    assertThat(storedAuditDto.getUuid()).isEqualTo(auditDto.getUuid());
  }

  @Test
  void insert_truncateVeryLongNewValue() {
    AuditDto auditDto = AuditTesting.newAuditDto();
    String veryLongString = secure().nextAlphanumeric(5000);
    auditDto.setNewValue(veryLongString);

    testAuditDao.insert(dbSession, auditDto);

    assertThat(auditDto.getNewValue()).isEqualTo(EXCEEDED_LENGTH);
  }

  @Test
  void selectByPeriodPaginated_whenRowsInAnyOrder_returnOrderedByCreatedAt() {
    List<Long> createdAts = LongStream.range(1, 51).boxed().collect(Collectors.toList());
    Collections.shuffle(createdAts);
    createdAts.stream()
      .map(AuditTesting::newAuditDto)
      .forEach(auditDto -> testAuditDao.insert(dbSession, auditDto));

    List<AuditDto> auditDtos = testAuditDao.selectByPeriodPaginated(dbSession, 1, 51, 1);

    assertThat(auditDtos).hasSize(50);
    assertThat(auditDtos).extracting(AuditDto::getCreatedAt).isSorted();
  }

  @Test
  void selectByPeriodPaginated_whenRowsWithIdenticalCreatedAt_returnOrderedByCreatedAtAndUuids() {
    AuditDto auditDto1 = AuditTesting.newAuditDto(100L);
    auditDto1.setUuid("uuid1");
    testAuditDao.insert(dbSession, auditDto1);
    AuditDto auditDto2 = AuditTesting.newAuditDto(100L);
    auditDto2.setUuid("uuid2");
    testAuditDao.insert(dbSession, auditDto2);

    List<AuditDto> auditDtos = testAuditDao.selectByPeriodPaginated(dbSession, 99, 101, 1);

    assertThat(auditDtos).hasSize(2);
    assertThat(auditDtos).extracting(AuditDto::getCreatedAt).isSorted();
    assertThat(auditDtos).extracting(AuditDto::getUuid).isSorted();
  }

  private void prepareRowsWithDeterministicCreatedAt(int size) {
    for (int i = 1; i <= size; i++) {
      AuditDto auditDto = AuditTesting.newAuditDto(i);
      testAuditDao.insert(dbSession, auditDto);
    }
  }
}
