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
package org.sonar.db.es;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

class EsQueueDaoIT {

  private static final int LIMIT = 10;
  private static final TestSystem2 system2 = new TestSystem2().setNow(1_000);

  @RegisterExtension
  private final DbTester dbTester = DbTester.create(system2);

  private final DbSession dbSession = dbTester.getSession();
  private final EsQueueDao underTest = dbTester.getDbClient().esQueueDao();

  @Test
  void insert_data() {
    int nbOfInsert = 10 + new Random().nextInt(20);
    List<EsQueueDto> esQueueDtos = new ArrayList<>();
    IntStream.rangeClosed(1, nbOfInsert).forEach(
      i -> esQueueDtos.add(EsQueueDto.create("foo", UuidFactoryFast.getInstance().create()))
    );
    underTest.insert(dbSession, esQueueDtos);

    assertThat(dbTester.countSql(dbSession, "select count(*) from es_queue")).isEqualTo(nbOfInsert);
  }

  @Test
  void delete_unknown_EsQueueDto_does_not_throw_exception() {
    int nbOfInsert = 10 + new Random().nextInt(20);
    List<EsQueueDto> esQueueDtos = new ArrayList<>();
    IntStream.rangeClosed(1, nbOfInsert).forEach(
      i -> esQueueDtos.add(EsQueueDto.create("foo", UuidFactoryFast.getInstance().create()))
    );
    underTest.insert(dbSession, esQueueDtos);

    underTest.delete(dbSession, EsQueueDto.create("foo", UuidFactoryFast.getInstance().create()));

    assertThat(dbTester.countSql(dbSession, "select count(*) from es_queue")).isEqualTo(nbOfInsert);
  }

  @Test
  void delete_EsQueueDto_does_not_throw_exception() {
    int nbOfInsert = 10 + new Random().nextInt(20);
    List<EsQueueDto> esQueueDtos = new ArrayList<>();
    IntStream.rangeClosed(1, nbOfInsert).forEach(
      i -> esQueueDtos.add(EsQueueDto.create("foo", UuidFactoryFast.getInstance().create()))
    );
    underTest.insert(dbSession, esQueueDtos);
    assertThat(dbTester.countSql(dbSession, "select count(*) from es_queue")).isEqualTo(nbOfInsert);

    underTest.delete(dbSession, esQueueDtos);

    assertThat(dbTester.countSql(dbSession, "select count(*) from es_queue")).isZero();
  }

  @Test
  void selectForRecovery_must_return_limit_when_there_are_more_rows() {
    system2.setNow(1_000L);
    EsQueueDto i1 = underTest.insert(dbSession, EsQueueDto.create("foo", UuidFactoryFast.getInstance().create()));
    system2.setNow(1_001L);
    EsQueueDto i2 = underTest.insert(dbSession, EsQueueDto.create("foo", UuidFactoryFast.getInstance().create()));
    system2.setNow(1_002L);
    EsQueueDto i3 = underTest.insert(dbSession, EsQueueDto.create("foo", UuidFactoryFast.getInstance().create()));

    assertThat(underTest.selectForRecovery(dbSession, 2_000, 1))
      .extracting(EsQueueDto::getUuid)
      .containsExactly(i3.getUuid());

    assertThat(underTest.selectForRecovery(dbSession, 2_000, 2))
      .extracting(EsQueueDto::getUuid)
      .containsExactly(i3.getUuid(), i2.getUuid());

    assertThat(underTest.selectForRecovery(dbSession, 2_000, 10))
      .extracting(EsQueueDto::getUuid)
      .containsExactly(i3.getUuid(), i2.getUuid(), i1.getUuid());
  }

  @Test
  void selectForRecovery_returns_ordered_rows_created_before_date() {
    system2.setNow(1_000L);
    EsQueueDto i1 = underTest.insert(dbSession, EsQueueDto.create("foo", UuidFactoryFast.getInstance().create()));
    system2.setNow(1_001L);
    EsQueueDto i2 = underTest.insert(dbSession, EsQueueDto.create("foo", UuidFactoryFast.getInstance().create()));
    system2.setNow(1_002L);
    EsQueueDto i3 = underTest.insert(dbSession, EsQueueDto.create("foo", UuidFactoryFast.getInstance().create()));

    assertThat(underTest.selectForRecovery(dbSession, 999, LIMIT)).isEmpty();
    assertThat(underTest.selectForRecovery(dbSession, 1_000, LIMIT))
      .extracting(EsQueueDto::getUuid)
      .containsExactly(i1.getUuid());
    assertThat(underTest.selectForRecovery(dbSession, 1_001, LIMIT))
      .extracting(EsQueueDto::getUuid)
      .containsExactly(i2.getUuid(), i1.getUuid());
    assertThat(underTest.selectForRecovery(dbSession, 2_000, LIMIT))
      .extracting(EsQueueDto::getUuid)
      .containsExactly(i3.getUuid(), i2.getUuid(), i1.getUuid());
  }
}
