/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.audit.AuditDao.EXCEEDED_LENGTH;

public class AuditDaoTest {

  private static final long NOW = 1000000L;
  private static final String A_UUID = "SOME_UUID";
  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);
  @Rule
  public final DbTester db = DbTester.create(system2);
  private final DbSession dbSession = db.getSession();
  private final UuidFactory uuidFactory = mock(UuidFactory.class);

  private final AuditDao testAuditDao = new AuditDao(system2, UuidFactoryImpl.INSTANCE);

  @Test
  public void selectAll_oneEntryInserted_returnThisEntry() {
    AuditDao auditDaoDeterministicUUID = new AuditDao(system2, uuidFactory);
    when(uuidFactory.create()).thenReturn(A_UUID);
    AuditDto auditDto = AuditTesting.newAuditDto();
    auditDaoDeterministicUUID.insert(dbSession, auditDto);

    List<AuditDto> auditDtos = auditDaoDeterministicUUID.selectAll(dbSession);

    assertThat(auditDtos.size()).isEqualTo(1);
    assertThat(auditDtos.get(0))
      .extracting(AuditDto::getUuid, AuditDto::getUserLogin,
        AuditDto::getUserUuid, AuditDto::getCategory,
        AuditDto::getOperation, AuditDto::getNewValue,
        AuditDto::getCreatedAt)
      .containsExactly(A_UUID, auditDto.getUserLogin(),
        auditDto.getUserUuid(), auditDto.getCategory(),
        auditDto.getOperation(), auditDto.getNewValue(),
        auditDto.getCreatedAt());
  }

  @Test
  public void selectAll_100EntriesInserted_100EntriesReturned() {
    AuditDao auditDao = new AuditDao(system2, UuidFactoryImpl.INSTANCE);
    for(int i=0; i<100; i++) {
      AuditDto auditDto = AuditTesting.newAuditDto();
      auditDto.setUuid(randomAlphanumeric(20));
      auditDao.insert(dbSession, auditDto);
    }

    List<AuditDto> auditDtos = auditDao.selectAll(dbSession);

    assertThat(auditDtos.size()).isEqualTo(100);
  }

  @Test
  public void selectByPeriod_selectOneRowFromTheMiddle() {
    prepareThreeRowsWithDeterministicCreatedAt();

    List<AuditDto> auditDtos = testAuditDao.selectByPeriod(dbSession, 1, 3);

    assertThat(auditDtos.size()).isEqualTo(1);
    assertThat(auditDtos.get(0).getCreatedAt()).isEqualTo(2);
  }

  @Test
  public void selectByPeriod_selectOneRowFromTheEnd() {
    prepareThreeRowsWithDeterministicCreatedAt();

    List<AuditDto> auditDtos = testAuditDao.selectByPeriod(dbSession, 2, 4);

    assertThat(auditDtos.size()).isEqualTo(1);
    assertThat(auditDtos.get(0).getCreatedAt()).isEqualTo(3);
  }

  @Test
  public void selectByPeriod_selectAllRows() {
    prepareThreeRowsWithDeterministicCreatedAt();

    List<AuditDto> auditDtos = testAuditDao.selectByPeriod(dbSession, 0, 4);

    assertThat(auditDtos.size()).isEqualTo(3);
  }

  @Test
  public void selectIfBeforeSelectedDate_select1Row() {
    prepareThreeRowsWithDeterministicCreatedAt();

    List<AuditDto> auditDtos = testAuditDao.selectIfBeforeSelectedDate(dbSession, 2);

    assertThat(auditDtos.size()).isEqualTo(1);
  }

  @Test
  public void deleteIfBeforeSelectedDate_deleteTwoRows() {
    prepareThreeRowsWithDeterministicCreatedAt();

    testAuditDao.deleteIfBeforeSelectedDate(dbSession, 2);

    List<AuditDto> auditDtos = testAuditDao.selectAll(dbSession);
    assertThat(auditDtos.size()).isEqualTo(1);
  }

  @Test
  public void insert_truncateVeryLongNewValue() {
    AuditDto auditDto = AuditTesting.newAuditDto();
    String veryLongString = randomAlphanumeric(5000);
    auditDto.setNewValue(veryLongString);

    testAuditDao.insert(dbSession, auditDto);

    assertThat(auditDto.getNewValue()).isEqualTo(EXCEEDED_LENGTH);
  }

  private void prepareThreeRowsWithDeterministicCreatedAt() {
    for(int i=1; i<=3; i++) {
      AuditDto auditDto = AuditTesting.newAuditDto();
      system2.setNow(i);
      testAuditDao.insert(dbSession, auditDto);
    }
  }
}
