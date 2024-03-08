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
package org.sonar.db.component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

class AnalysisPropertiesDaoIT {
  private static final long NOW = 1_000L;

  @RegisterExtension
  private final DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final System2 system2 = new TestSystem2().setNow(NOW);
  private final DbSession dbSession = dbTester.getSession();
  private final AnalysisPropertiesDao underTest = new AnalysisPropertiesDao(system2);
  private final Random random = new Random();

  @Test
  void insert_with_null_uuid_throws_NPE() {
    AnalysisPropertyDto analysisPropertyDto = new AnalysisPropertyDto()
      .setAnalysisUuid(randomAlphanumeric(10))
      .setKey(randomAlphanumeric(10))
      .setValue(randomAlphanumeric(10));

    assertThatThrownBy(() -> underTest.insert(dbSession, analysisPropertyDto))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("uuid cannot be null");
  }

  @Test
  void insert_with_null_key_throws_NPE() {
    AnalysisPropertyDto analysisPropertyDto = new AnalysisPropertyDto()
      .setAnalysisUuid(randomAlphanumeric(10))
      .setUuid(randomAlphanumeric(10))
      .setValue(randomAlphanumeric(10));

    assertThatThrownBy(() -> underTest.insert(dbSession, analysisPropertyDto))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("key cannot be null");
  }

  @Test
  void insert_with_null_analysis_uuid_throws_NPE() {
    AnalysisPropertyDto analysisPropertyDto = new AnalysisPropertyDto()
      .setUuid(randomAlphanumeric(10))
      .setKey(randomAlphanumeric(10))
      .setValue(randomAlphanumeric(10));

    assertThatThrownBy(() -> underTest.insert(dbSession, analysisPropertyDto))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("analysis uuid cannot be null");
  }

  @Test
  void insert_with_null_value_throws_NPE() {
    AnalysisPropertyDto analysisPropertyDto = new AnalysisPropertyDto()
      .setAnalysisUuid(randomAlphanumeric(10))
      .setUuid(randomAlphanumeric(10))
      .setKey(randomAlphanumeric(10));

    assertThatThrownBy(() -> underTest.insert(dbSession, analysisPropertyDto))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("value cannot be null");
  }

  @Test
  void insert_as_empty() {
    AnalysisPropertyDto analysisPropertyDto = insertAnalysisPropertyDto(0);

    assertThat(dbTester.countRowsOfTable(dbSession, "ANALYSIS_PROPERTIES")).isOne();
    compareFirstValueWith(analysisPropertyDto);
  }

  @Test
  void insert_as_text() {
    AnalysisPropertyDto analysisPropertyDto = insertAnalysisPropertyDto(1 + random.nextInt(3999));

    assertThat(dbTester.countRowsOfTable(dbSession, "ANALYSIS_PROPERTIES")).isOne();
    compareFirstValueWith(analysisPropertyDto);
  }

  @Test
  void insert_as_clob() {
    AnalysisPropertyDto analysisPropertyDto = insertAnalysisPropertyDto(4000 + random.nextInt(100));

    assertThat(dbTester.countRowsOfTable(dbSession, "ANALYSIS_PROPERTIES")).isOne();
    compareFirstValueWith(analysisPropertyDto);
  }

  @Test
  void insert_a_list() {
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
  void selectByAnalysisUuid_should_return_correct_values() {
    String analysisUuid = randomAlphanumeric(40);

    List<AnalysisPropertyDto> propertyDtos = Arrays.asList(
      newAnalysisPropertyDto(random.nextInt(8000), analysisUuid),
      newAnalysisPropertyDto(random.nextInt(8000), analysisUuid),
      newAnalysisPropertyDto(random.nextInt(8000), analysisUuid),
      newAnalysisPropertyDto(random.nextInt(8000), analysisUuid),
      newAnalysisPropertyDto(random.nextInt(8000), analysisUuid),
      newAnalysisPropertyDto(random.nextInt(8000), analysisUuid),
      newAnalysisPropertyDto(random.nextInt(8000), analysisUuid),
      newAnalysisPropertyDto(random.nextInt(8000), analysisUuid));

    underTest.insert(dbSession, propertyDtos);
    assertThat(dbTester.countRowsOfTable(dbSession, "ANALYSIS_PROPERTIES")).isEqualTo(propertyDtos.size());

    List<AnalysisPropertyDto> result = underTest.selectByAnalysisUuid(dbSession, analysisUuid);
    assertThat(result).containsExactlyInAnyOrder(propertyDtos.toArray(new AnalysisPropertyDto[0]));
  }

  @Test
  void selectByKeyAndAnalysisUuids_should_return_correct_values() {
    String analysisUuid = randomAlphanumeric(40);

    List<AnalysisPropertyDto> propertyDtos = Arrays.asList(
      newAnalysisPropertyDto(random.nextInt(10), "key1", analysisUuid),
      newAnalysisPropertyDto(random.nextInt(10), "key2", analysisUuid),
      newAnalysisPropertyDto(random.nextInt(10), "key3", analysisUuid)
    );

    underTest.insert(dbSession, propertyDtos);
    assertThat(dbTester.countRowsOfTable(dbSession, "ANALYSIS_PROPERTIES")).isEqualTo(propertyDtos.size());

    List<AnalysisPropertyDto> result = underTest.selectByKeyAndAnalysisUuids(dbSession, "key1", Set.of(analysisUuid));
    assertThat(result).contains(propertyDtos.get(0));
    result = underTest.selectByKeyAndAnalysisUuids(dbSession, "key2", Set.of(analysisUuid));
    assertThat(result).contains(propertyDtos.get(1));
    result = underTest.selectByKeyAndAnalysisUuids(dbSession, "key3", Set.of(analysisUuid));
    assertThat(result).contains(propertyDtos.get(2));
  }

  @Test
  void selectProjectCountPerAnalysisPropertyValueInLastAnalysis_should_return_correct_values() {
    final String analysisPropertyKey = "key";
    for (int i = 0; i < 7; i++) {
      String uuid = "uuid" + i;
      ProjectDto project = dbTester.components().insertPrivateProject(c -> {
      }, p -> p.setUuid(uuid)).getProjectDto();
      dbTester.components().insertSnapshot(project, s -> s.setLast(true).setUuid(uuid));
      // branches shouldn't be taken into account
      dbTester.components().insertProjectBranch(project);
    }

    underTest.insert(dbSession, new AnalysisPropertyDto().setKey(analysisPropertyKey).setValue("git").setAnalysisUuid("uuid0").setUuid("0"
    ));
    underTest.insert(dbSession, new AnalysisPropertyDto().setKey(analysisPropertyKey).setValue("svn").setAnalysisUuid("uuid1").setUuid("1"
    ));
    underTest.insert(dbSession,
      new AnalysisPropertyDto().setKey(analysisPropertyKey).setValue("undetected").setAnalysisUuid("uuid2").setUuid("2"));
    underTest.insert(dbSession,
      new AnalysisPropertyDto().setKey(analysisPropertyKey).setValue("undetected").setAnalysisUuid("uuid3").setUuid("3"));
    underTest.insert(dbSession, new AnalysisPropertyDto().setKey(analysisPropertyKey).setValue("git").setAnalysisUuid("uuid4").setUuid("4"
    ));
    underTest.insert(dbSession, new AnalysisPropertyDto().setKey(analysisPropertyKey).setValue("git").setAnalysisUuid("uuid5").setUuid("5"
    ));

    List<AnalysisPropertyValuePerProject> result = underTest.selectAnalysisPropertyValueInLastAnalysisPerProject(dbSession,
      analysisPropertyKey);

    assertThat(result)
      .extracting(AnalysisPropertyValuePerProject::getProjectUuid, AnalysisPropertyValuePerProject::getPropertyValue)
      .containsExactlyInAnyOrder(
        tuple("uuid0", "git"),
        tuple("uuid1", "svn"),
        tuple("uuid2", "undetected"),
        tuple("uuid3", "undetected"),
        tuple("uuid4", "git"),
        tuple("uuid5", "git")
      );
  }

  private AnalysisPropertyDto insertAnalysisPropertyDto(int valueLength) {
    AnalysisPropertyDto analysisPropertyDto = newAnalysisPropertyDto(valueLength, randomAlphanumeric(40));
    underTest.insert(dbSession, analysisPropertyDto);
    return analysisPropertyDto;
  }

  private AnalysisPropertyDto newAnalysisPropertyDto(int valueLength, String key, String analysisUuid) {
    return new AnalysisPropertyDto()
      .setAnalysisUuid(analysisUuid)
      .setKey(key)
      .setUuid(randomAlphanumeric(40))
      .setValue(randomAlphanumeric(valueLength))
      .setCreatedAt(1_000L);
  }

  private AnalysisPropertyDto newAnalysisPropertyDto(int valueLength, String analysisUuid) {
    return newAnalysisPropertyDto(valueLength, randomAlphanumeric(512), analysisUuid);
  }

  private void compareFirstValueWith(AnalysisPropertyDto analysisPropertyDto) {
    AnalysisPropertyDto dtoFromDatabase = underTest.selectByAnalysisUuid(dbSession, analysisPropertyDto.getAnalysisUuid()).get(0);
    assertThat(dtoFromDatabase).isEqualTo(analysisPropertyDto);
  }
}
