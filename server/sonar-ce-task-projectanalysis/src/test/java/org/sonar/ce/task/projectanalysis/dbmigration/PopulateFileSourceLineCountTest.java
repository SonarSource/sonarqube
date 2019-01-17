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
package org.sonar.ce.task.projectanalysis.dbmigration;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.CeTask;
import org.sonar.db.DbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.source.FileSourceDto.LINE_COUNT_NOT_POPULATED;

public class PopulateFileSourceLineCountTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, PopulateFileSourceLineCountTest.class, "file_sources.sql");

  private Random random = new Random();
  private CeTask ceTask = mock(CeTask.class);
  private PopulateFileSourceLineCount underTest = new PopulateFileSourceLineCount(db.database(), ceTask);

  @Test
  public void execute_has_no_effect_on_empty_table() throws SQLException {
    String projectUuid = randomAlphanumeric(4);
    when(ceTask.getComponent()).thenReturn(newComponent(projectUuid));

    underTest.execute();
  }

  @Test
  public void execute_populates_line_count_of_any_type() throws SQLException {
    String projectUuid = randomAlphanumeric(4);
    String fileUuid = randomAlphanumeric(5);
    when(ceTask.getComponent()).thenReturn(newComponent(projectUuid));
    int lineCount = 1 + random.nextInt(15);
    insertUnpopulatedFileSource(projectUuid, fileUuid, lineCount);
    assertThat(getLineCountByFileUuid(fileUuid)).isEqualTo(LINE_COUNT_NOT_POPULATED);

    underTest.execute();

    assertThat(getLineCountByFileUuid(fileUuid)).isEqualTo(lineCount);
  }

  @Test
  public void execute_changes_only_file_source_with_LINE_COUNT_NOT_POPULATED_value() throws SQLException {
    String projectUuid = randomAlphanumeric(4);
    String fileUuid1 = randomAlphanumeric(5);
    String fileUuid2 = randomAlphanumeric(6);
    String fileUuid3 = randomAlphanumeric(7);
    int lineCountFile1 = 100 + random.nextInt(15);
    int lineCountFile2 = 50 + random.nextInt(15);
    int lineCountFile3 = 150 + random.nextInt(15);

    when(ceTask.getComponent()).thenReturn(newComponent(projectUuid));
    insertPopulatedFileSource(projectUuid, fileUuid1, lineCountFile1);
    int badLineCountFile2 = insertInconsistentPopulatedFileSource(projectUuid, fileUuid2, lineCountFile2);
    insertUnpopulatedFileSource(projectUuid, fileUuid3, lineCountFile3);
    assertThat(getLineCountByFileUuid(fileUuid1)).isEqualTo(lineCountFile1);
    assertThat(getLineCountByFileUuid(fileUuid2)).isEqualTo(badLineCountFile2);
    assertThat(getLineCountByFileUuid(fileUuid3)).isEqualTo(LINE_COUNT_NOT_POPULATED);

    underTest.execute();

    assertThat(getLineCountByFileUuid(fileUuid1)).isEqualTo(lineCountFile1);
    assertThat(getLineCountByFileUuid(fileUuid2)).isEqualTo(badLineCountFile2);
    assertThat(getLineCountByFileUuid(fileUuid3)).isEqualTo(lineCountFile3);
  }

  @Test
  public void execute_changes_only_file_source_of_CeTask_component_uuid() throws SQLException {
    String projectUuid1 = randomAlphanumeric(4);
    String projectUuid2 = randomAlphanumeric(5);
    String fileUuid1 = randomAlphanumeric(6);
    String fileUuid2 = randomAlphanumeric(7);
    int lineCountFile1 = 100 + random.nextInt(15);
    int lineCountFile2 = 30 + random.nextInt(15);

    when(ceTask.getComponent()).thenReturn(newComponent(projectUuid1));
    insertUnpopulatedFileSource(projectUuid1, fileUuid1, lineCountFile1);
    insertUnpopulatedFileSource(projectUuid2, fileUuid2, lineCountFile2);

    underTest.execute();

    assertThat(getLineCountByFileUuid(fileUuid1)).isEqualTo(lineCountFile1);
    assertThat(getLineCountByFileUuid(fileUuid2)).isEqualTo(LINE_COUNT_NOT_POPULATED);
  }

  @Test
  public void execute_set_line_count_to_zero_when_file_source_has_no_line_hashes() throws SQLException {
    String projectUuid = randomAlphanumeric(4);
    String fileUuid1 = randomAlphanumeric(5);

    when(ceTask.getComponent()).thenReturn(newComponent(projectUuid));
    insertFileSource(projectUuid, fileUuid1, null, LINE_COUNT_NOT_POPULATED);

    underTest.execute();

    assertThat(getLineCountByFileUuid(fileUuid1)).isZero();
  }

  @Test
  public void execute_set_line_count_to_1_when_file_source_has_empty_line_hashes() throws SQLException {
    String projectUuid = randomAlphanumeric(4);
    String fileUuid1 = randomAlphanumeric(5);

    when(ceTask.getComponent()).thenReturn(newComponent(projectUuid));
    insertFileSource(projectUuid, fileUuid1, "", LINE_COUNT_NOT_POPULATED);

    underTest.execute();

    assertThat(getLineCountByFileUuid(fileUuid1)).isEqualTo(1);
  }

  private int getLineCountByFileUuid(String fileUuid) {
    Long res = (Long) db.selectFirst("select line_count as \"LINE_COUNT\" from file_sources where file_uuid = '" + fileUuid + "'")
      .get("LINE_COUNT");
    return res.intValue();
  }

  private void insertUnpopulatedFileSource(String projectUuid, String fileUuid, int numberOfHashes) {
    String lineHashes = generateLineHashes(numberOfHashes);

    insertFileSource(projectUuid, fileUuid, lineHashes, LINE_COUNT_NOT_POPULATED);
  }

  private void insertPopulatedFileSource(String projectUuid, String fileUuid, int lineCount) {
    String lineHashes = generateLineHashes(lineCount);

    insertFileSource(projectUuid, fileUuid, lineHashes, lineCount);
  }

  private int insertInconsistentPopulatedFileSource(String projectUuid, String fileUuid, int lineCount) {
    String lineHashes = generateLineHashes(lineCount);
    int badLineCount = lineCount + random.nextInt(6);

    insertFileSource(projectUuid, fileUuid, lineHashes, badLineCount);

    return badLineCount;
  }

  private static String generateLineHashes(int numberOfHashes) {
    return IntStream.range(0, numberOfHashes)
      .mapToObj(String::valueOf)
      .collect(Collectors.joining("\n"));
  }

  private void insertFileSource(String projectUuid, String fileUuid, @Nullable String lineHashes, int lineCount) {
    db.executeInsert(
      "FILE_SOURCES",
      "PROJECT_UUID", projectUuid,
      "FILE_UUID", fileUuid,
      "LINE_HASHES", lineHashes,
      "LINE_COUNT", lineCount,
      "CREATED_AT", 1_222_333L,
      "UPDATED_AT", 1_222_333L);
    db.commit();
  }

  private static Optional<CeTask.Component> newComponent(String projectUuid) {
    return Optional.of(new CeTask.Component(projectUuid, "key_" + projectUuid, "name_" + projectUuid));
  }
}
