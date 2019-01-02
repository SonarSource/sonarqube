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
package org.sonar.core.platform;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.core.util.UuidFactoryImpl;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.platform.ServerId.DATABASE_ID_LENGTH;
import static org.sonar.core.platform.ServerId.DEPRECATED_SERVER_ID_LENGTH;
import static org.sonar.core.platform.ServerId.NOT_UUID_DATASET_ID_LENGTH;
import static org.sonar.core.platform.ServerId.SPLIT_CHARACTER;
import static org.sonar.core.platform.ServerId.UUID_DATASET_ID_LENGTH;
import static org.sonar.core.platform.ServerId.Format.DEPRECATED;
import static org.sonar.core.platform.ServerId.Format.NO_DATABASE_ID;
import static org.sonar.core.platform.ServerId.Format.WITH_DATABASE_ID;

@RunWith(DataProviderRunner.class)
public class ServerIdTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void parse_throws_NPE_if_argument_is_null() {
    expectedException.expect(NullPointerException.class);

    ServerId.parse(null);
  }

  @Test
  @UseDataProvider("emptyAfterTrim")
  public void parse_throws_IAE_if_parameter_is_empty_after_trim(String serverId) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("serverId can't be empty");

    ServerId.parse(serverId);
  }

  @DataProvider
  public static Object[][] emptyAfterTrim() {
    return new Object[][] {
      {""},
      {" "},
      {"    "}
    };
  }

  @Test
  @UseDataProvider("wrongFormatWithDatabaseId")
  public void parse_throws_IAE_if_split_char_is_at_wrong_position(String emptyDatabaseId) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unrecognized serverId format. Parts have wrong length");

    ServerId.parse(emptyDatabaseId);
  }

  @DataProvider
  public static Object[][] wrongFormatWithDatabaseId() {
    String onlySplitChar = repeat(SPLIT_CHARACTER + "", DATABASE_ID_LENGTH);
    String startWithSplitChar = SPLIT_CHARACTER + randomAlphabetic(DATABASE_ID_LENGTH - 1);

    Stream<String> databaseIds = Stream.of(
      UuidFactoryImpl.INSTANCE.create(),
      randomAlphabetic(NOT_UUID_DATASET_ID_LENGTH),
      randomAlphabetic(UUID_DATASET_ID_LENGTH),
      repeat(SPLIT_CHARACTER + "", NOT_UUID_DATASET_ID_LENGTH),
      repeat(SPLIT_CHARACTER + "", UUID_DATASET_ID_LENGTH));

    return databaseIds
      .flatMap(datasetId -> Stream.of(
        startWithSplitChar + SPLIT_CHARACTER + datasetId,
        onlySplitChar + SPLIT_CHARACTER + datasetId,
        startWithSplitChar + randomAlphabetic(1) + datasetId,
        onlySplitChar + randomAlphabetic(1) + datasetId))
      .flatMap(serverId -> Stream.of(
        serverId,
        " " + serverId,
        "    " + serverId))
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @Test
  public void parse_parses_deprecated_format_serverId() {
    String deprecated = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

    ServerId serverId = ServerId.parse(deprecated);

    assertThat(serverId.getFormat()).isEqualTo(DEPRECATED);
    assertThat(serverId.getDatasetId()).isEqualTo(deprecated);
    assertThat(serverId.getDatabaseId()).isEmpty();
    assertThat(serverId.toString()).isEqualTo(deprecated);
  }

  @Test
  @UseDataProvider("validOldFormatServerIds")
  public void parse_parses_no_databaseId_format_serverId(String noDatabaseId) {
    ServerId serverId = ServerId.parse(noDatabaseId);

    assertThat(serverId.getFormat()).isEqualTo(NO_DATABASE_ID);
    assertThat(serverId.getDatasetId()).isEqualTo(noDatabaseId);
    assertThat(serverId.getDatabaseId()).isEmpty();
    assertThat(serverId.toString()).isEqualTo(noDatabaseId);
  }

  @DataProvider
  public static Object[][] validOldFormatServerIds() {
    return new Object[][] {
      {UuidFactoryImpl.INSTANCE.create()},
      {randomAlphabetic(NOT_UUID_DATASET_ID_LENGTH)},
      {repeat(SPLIT_CHARACTER + "", NOT_UUID_DATASET_ID_LENGTH)},
      {randomAlphabetic(UUID_DATASET_ID_LENGTH)},
      {repeat(SPLIT_CHARACTER + "", UUID_DATASET_ID_LENGTH)}
    };
  }

  @Test
  @UseDataProvider("validServerIdWithDatabaseId")
  public void parse_parses_serverId_with_database_id(String databaseId, String datasetId) {
    String rawServerId = databaseId + SPLIT_CHARACTER + datasetId;

    ServerId serverId = ServerId.parse(rawServerId);

    assertThat(serverId.getFormat()).isEqualTo(WITH_DATABASE_ID);
    assertThat(serverId.getDatasetId()).isEqualTo(datasetId);
    assertThat(serverId.getDatabaseId()).contains(databaseId);
    assertThat(serverId.toString()).isEqualTo(rawServerId);
  }

  @DataProvider
  public static Object[][] validServerIdWithDatabaseId() {
    return new Object[][] {
      {randomAlphabetic(DATABASE_ID_LENGTH), randomAlphabetic(NOT_UUID_DATASET_ID_LENGTH)},
      {randomAlphabetic(DATABASE_ID_LENGTH), randomAlphabetic(UUID_DATASET_ID_LENGTH)},
      {randomAlphabetic(DATABASE_ID_LENGTH), repeat(SPLIT_CHARACTER + "", NOT_UUID_DATASET_ID_LENGTH)},
      {randomAlphabetic(DATABASE_ID_LENGTH), repeat(SPLIT_CHARACTER + "", UUID_DATASET_ID_LENGTH)},
      {randomAlphabetic(DATABASE_ID_LENGTH), UuidFactoryImpl.INSTANCE.create()},
    };
  }

  @Test
  public void parse_does_not_support_deprecated_server_id_with_database_id() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("serverId does not have a supported length");

    ServerId.parse(randomAlphabetic(DATABASE_ID_LENGTH) + SPLIT_CHARACTER + randomAlphabetic(DEPRECATED_SERVER_ID_LENGTH));
  }

  @Test
  public void of_throws_NPE_if_datasetId_is_null() {
    expectedException.expect(NullPointerException.class);

    ServerId.of(randomAlphabetic(DATABASE_ID_LENGTH), null);
  }

  @Test
  public void of_throws_IAE_if_datasetId_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Illegal datasetId length (0)");

    ServerId.of(randomAlphabetic(DATABASE_ID_LENGTH), "");
  }

  @Test
  public void of_throws_IAE_if_databaseId_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Illegal databaseId length (0)");

    ServerId.of("", randomAlphabetic(UUID_DATASET_ID_LENGTH));
  }

  @Test
  @UseDataProvider("datasetIdSupportedLengths")
  public void of_accepts_null_databaseId(int datasetIdLength) {
    String datasetId = randomAlphabetic(datasetIdLength);
    ServerId serverId = ServerId.of(null, datasetId);

    assertThat(serverId.getDatabaseId()).isEmpty();
    assertThat(serverId.getDatasetId()).isEqualTo(datasetId);
  }

  @Test
  @UseDataProvider("illegalDatabaseIdLengths")
  public void of_throws_IAE_if_databaseId_length_is_not_8(int illegalDatabaseIdLengths) {
    String databaseId = randomAlphabetic(illegalDatabaseIdLengths);
    String datasetId = randomAlphabetic(UUID_DATASET_ID_LENGTH);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Illegal databaseId length (" + illegalDatabaseIdLengths + ")");

    ServerId.of(databaseId, datasetId);
  }

  @DataProvider
  public static Object[][] illegalDatabaseIdLengths() {
    return IntStream.range(1, 8 + new Random().nextInt(5))
      .filter(i -> i != DATABASE_ID_LENGTH)
      .mapToObj(i -> new Object[] {i})
      .toArray(Object[][]::new);
  }

  @Test
  @UseDataProvider("illegalDatasetIdLengths")
  public void of_throws_IAE_if_datasetId_length_is_not_8(int illegalDatasetIdLengths) {
    String datasetId = randomAlphabetic(illegalDatasetIdLengths);
    String databaseId = randomAlphabetic(DATABASE_ID_LENGTH);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Illegal datasetId length (" + illegalDatasetIdLengths + ")");

    ServerId.of(databaseId, datasetId);
  }

  @DataProvider
  public static Object[][] illegalDatasetIdLengths() {
    return IntStream.range(1, UUID_DATASET_ID_LENGTH + new Random().nextInt(5))
      .filter(i -> i != UUID_DATASET_ID_LENGTH)
      .filter(i -> i != NOT_UUID_DATASET_ID_LENGTH)
      .filter(i -> i != DEPRECATED_SERVER_ID_LENGTH)
      .mapToObj(i -> new Object[] {i})
      .toArray(Object[][]::new);
  }

  @Test
  @UseDataProvider("datasetIdSupportedLengths")
  public void equals_is_based_on_databaseId_and_datasetId(int datasetIdLength) {
    String databaseId = randomAlphabetic(DATABASE_ID_LENGTH - 1) + 'a';
    String otherDatabaseId = randomAlphabetic(DATABASE_ID_LENGTH - 1) + 'b';
    String datasetId = randomAlphabetic(datasetIdLength - 1) + 'a';
    String otherDatasetId = randomAlphabetic(datasetIdLength - 1) + 'b';

    ServerId newServerId = ServerId.of(databaseId, datasetId);
    assertThat(newServerId).isEqualTo(newServerId);
    assertThat(newServerId).isEqualTo(ServerId.of(databaseId, datasetId));
    assertThat(newServerId).isNotEqualTo(new Object());
    assertThat(newServerId).isNotEqualTo(null);
    assertThat(newServerId).isNotEqualTo(ServerId.of(otherDatabaseId, datasetId));
    assertThat(newServerId).isNotEqualTo(ServerId.of(databaseId, otherDatasetId));
    assertThat(newServerId).isNotEqualTo(ServerId.of(otherDatabaseId, otherDatasetId));

    ServerId oldServerId = ServerId.parse(datasetId);
    assertThat(oldServerId).isEqualTo(oldServerId);
    assertThat(oldServerId).isEqualTo(ServerId.parse(datasetId));
    assertThat(oldServerId).isNotEqualTo(ServerId.parse(otherDatasetId));
    assertThat(oldServerId).isNotEqualTo(ServerId.of(databaseId, datasetId));
  }

  @Test
  @UseDataProvider("datasetIdSupportedLengths")
  public void hashcode_is_based_on_databaseId_and_datasetId(int datasetIdLength) {
    String databaseId = randomAlphabetic(DATABASE_ID_LENGTH - 1) + 'a';
    String otherDatabaseId = randomAlphabetic(DATABASE_ID_LENGTH - 1) + 'b';
    String datasetId = randomAlphabetic(datasetIdLength - 1) + 'a';
    String otherDatasetId = randomAlphabetic(datasetIdLength - 1) + 'b';

    ServerId newServerId = ServerId.of(databaseId, datasetId);
    assertThat(newServerId.hashCode()).isEqualTo(newServerId.hashCode());
    assertThat(newServerId.hashCode()).isEqualTo(ServerId.of(databaseId, datasetId).hashCode());
    assertThat(newServerId.hashCode()).isNotEqualTo(new Object().hashCode());
    assertThat(newServerId.hashCode()).isNotEqualTo(null);
    assertThat(newServerId.hashCode()).isNotEqualTo(ServerId.of(otherDatabaseId, datasetId).hashCode());
    assertThat(newServerId.hashCode()).isNotEqualTo(ServerId.of(databaseId, otherDatasetId).hashCode());
    assertThat(newServerId.hashCode()).isNotEqualTo(ServerId.of(otherDatabaseId, otherDatasetId).hashCode());

    ServerId oldServerId = ServerId.parse(datasetId);
    assertThat(oldServerId.hashCode()).isEqualTo(oldServerId.hashCode());
    assertThat(oldServerId.hashCode()).isEqualTo(ServerId.parse(datasetId).hashCode());
    assertThat(oldServerId.hashCode()).isNotEqualTo(ServerId.parse(otherDatasetId).hashCode());
    assertThat(oldServerId.hashCode()).isNotEqualTo(ServerId.of(databaseId, datasetId).hashCode());
  }

  @DataProvider
  public static Object[][] datasetIdSupportedLengths() {
    return new Object[][] {
      {ServerId.NOT_UUID_DATASET_ID_LENGTH},
      {UUID_DATASET_ID_LENGTH},
    };
  }
}
