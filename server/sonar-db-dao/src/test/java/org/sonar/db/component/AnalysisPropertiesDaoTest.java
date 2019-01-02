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
package org.sonar.db.component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class AnalysisPropertiesDaoTest {
  private static final long NOW = 1_000L;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final System2 system2 = new TestSystem2().setNow(NOW);
  private final DbSession dbSession = dbTester.getSession();
  private final AnalysisPropertiesDao underTest = new AnalysisPropertiesDao(system2);
  private final Random random = new Random();

  @Test
  public void insert_with_null_uuid_throws_NPE() {
    AnalysisPropertyDto analysisPropertyDto = new AnalysisPropertyDto()
      .setSnapshotUuid(randomAlphanumeric(10))
      .setKey(randomAlphanumeric(10))
      .setValue(randomAlphanumeric(10));

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid cannot be null");

    underTest.insert(dbSession, analysisPropertyDto);
  }

  @Test
  public void insert_with_null_key_throws_NPE() {
    AnalysisPropertyDto analysisPropertyDto = new AnalysisPropertyDto()
      .setSnapshotUuid(randomAlphanumeric(10))
      .setUuid(randomAlphanumeric(10))
      .setValue(randomAlphanumeric(10));

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("key cannot be null");

    underTest.insert(dbSession, analysisPropertyDto);
  }

  @Test
  public void insert_with_null_snapshot_uuid_throws_NPE() {
    AnalysisPropertyDto analysisPropertyDto = new AnalysisPropertyDto()
      .setUuid(randomAlphanumeric(10))
      .setKey(randomAlphanumeric(10))
      .setValue(randomAlphanumeric(10));

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("snapshot uuid cannot be null");

    underTest.insert(dbSession, analysisPropertyDto);
  }

  @Test
  public void insert_with_null_value_throws_NPE() {
    AnalysisPropertyDto analysisPropertyDto = new AnalysisPropertyDto()
      .setSnapshotUuid(randomAlphanumeric(10))
      .setUuid(randomAlphanumeric(10))
      .setKey(randomAlphanumeric(10));

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("value cannot be null");

    underTest.insert(dbSession, analysisPropertyDto);
  }

  @Test
  public void insert_as_empty() {
    AnalysisPropertyDto analysisPropertyDto = insertAnalysisPropertyDto(0);

    assertThat(dbTester.countRowsOfTable(dbSession, "ANALYSIS_PROPERTIES")).isEqualTo(1);
    compareFirstValueWith(analysisPropertyDto);
  }

  @Test
  public void insert_as_text() {
    AnalysisPropertyDto analysisPropertyDto = insertAnalysisPropertyDto(1 + random.nextInt(3999));

    assertThat(dbTester.countRowsOfTable(dbSession, "ANALYSIS_PROPERTIES")).isEqualTo(1);
    compareFirstValueWith(analysisPropertyDto);
  }

  @Test
  public void insert_as_clob() {
    AnalysisPropertyDto analysisPropertyDto = insertAnalysisPropertyDto(4000 + random.nextInt(100));

    assertThat(dbTester.countRowsOfTable(dbSession, "ANALYSIS_PROPERTIES")).isEqualTo(1);
    compareFirstValueWith(analysisPropertyDto);
  }

  @Test
  public void insert_a_list() {
    List<AnalysisPropertyDto> propertyDtos = Arrays.asList(
      newAnalysisPropertyDto(random.nextInt(8000), randomAlphanumeric(40)),
      newAnalysisPropertyDto(random.nextInt(8000), randomAlphanumeric(40)),
      newAnalysisPropertyDto(random.nextInt(8000), randomAlphanumeric(40)),
      newAnalysisPropertyDto(random.nextInt(8000), randomAlphanumeric(40)),
      newAnalysisPropertyDto(random.nextInt(8000), randomAlphanumeric(40)),
      newAnalysisPropertyDto(random.nextInt(8000), randomAlphanumeric(40)),
      newAnalysisPropertyDto(random.nextInt(8000), randomAlphanumeric(40)),
      newAnalysisPropertyDto(random.nextInt(8000), randomAlphanumeric(40)));

    underTest.insert(dbSession, propertyDtos);
    assertThat(dbTester.countRowsOfTable(dbSession, "ANALYSIS_PROPERTIES")).isEqualTo(propertyDtos.size());
  }

  @Test
  public void selectByAnalysisUuid_should_return_correct_values() {
    String snapshotUuid = randomAlphanumeric(40);

    List<AnalysisPropertyDto> propertyDtos = Arrays.asList(
      newAnalysisPropertyDto(random.nextInt(8000), snapshotUuid),
      newAnalysisPropertyDto(random.nextInt(8000), snapshotUuid),
      newAnalysisPropertyDto(random.nextInt(8000), snapshotUuid),
      newAnalysisPropertyDto(random.nextInt(8000), snapshotUuid),
      newAnalysisPropertyDto(random.nextInt(8000), snapshotUuid),
      newAnalysisPropertyDto(random.nextInt(8000), snapshotUuid),
      newAnalysisPropertyDto(random.nextInt(8000), snapshotUuid),
      newAnalysisPropertyDto(random.nextInt(8000), snapshotUuid));

    underTest.insert(dbSession, propertyDtos);
    assertThat(dbTester.countRowsOfTable(dbSession, "ANALYSIS_PROPERTIES")).isEqualTo(propertyDtos.size());

    List<AnalysisPropertyDto> result = underTest.selectBySnapshotUuid(dbSession, snapshotUuid);
    assertThat(result).containsExactlyInAnyOrder(propertyDtos.toArray(new AnalysisPropertyDto[0]));
  }

  private AnalysisPropertyDto insertAnalysisPropertyDto(int valueLength) {
    AnalysisPropertyDto analysisPropertyDto = newAnalysisPropertyDto(valueLength, randomAlphanumeric(40));
    underTest.insert(dbSession, analysisPropertyDto);
    return analysisPropertyDto;
  }

  private AnalysisPropertyDto newAnalysisPropertyDto(int valueLength, String snapshotUuid) {
    return new AnalysisPropertyDto()
      .setSnapshotUuid(snapshotUuid)
      .setKey(randomAlphanumeric(512))
      .setUuid(randomAlphanumeric(40))
      .setValue(randomAlphanumeric(valueLength))
      .setCreatedAt( 1_000L);
  }

  private void compareFirstValueWith(AnalysisPropertyDto analysisPropertyDto) {
    AnalysisPropertyDto dtoFromDatabase = underTest.selectBySnapshotUuid(dbSession, analysisPropertyDto.getSnapshotUuid()).get(0);
    assertThat(dtoFromDatabase).isEqualTo(analysisPropertyDto);
  }
}
