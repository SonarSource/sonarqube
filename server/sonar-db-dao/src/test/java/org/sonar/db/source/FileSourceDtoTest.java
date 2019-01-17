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
package org.sonar.db.source;

import com.google.common.base.Joiner;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.protobuf.DbFileSources;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSourceDtoTest {
  private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam ac magna libero. " +
    "Integer eu quam vulputate, interdum ante quis, sodales mauris. Nam mollis ornare dolor at maximus. Cras pharetra aliquam fringilla. " +
    "Nunc hendrerit, elit eu mattis fermentum, ligula metus malesuada nunc, non fermentum augue tellus eu odio. Praesent ut vestibulum nibh. " +
    "Curabitur sit amet dignissim magna, at efficitur dolor. Ut non felis aliquam justo euismod gravida. Morbi eleifend vitae ante eu pulvinar. " +
    "Aliquam rhoncus magna quis lorem posuere semper.";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void getSourceData_throws_ISE_with_id_fileUuid_and_projectUuid_in_message_when_data_cant_be_read() {
    long id = 12L;
    String fileUuid = "file uuid";
    String projectUuid = "project uuid";
    FileSourceDto underTest = new FileSourceDto()
      .setBinaryData(new byte[] {1, 2, 3, 4, 5})
      .setId(id)
      .setFileUuid(fileUuid)
      .setProjectUuid(projectUuid);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to decompress and deserialize source data [id=" + id + ",fileUuid=" + fileUuid + ",projectUuid=" + projectUuid + "]");

    underTest.getSourceData();
  }

  @Test
  public void getSourceData_reads_Data_object_bigger_than_default_size_limit() {
    DbFileSources.Data build = createOver64MBDataStructure();
    byte[] bytes = FileSourceDto.encodeSourceData(build);

    DbFileSources.Data data = new FileSourceDto().decodeSourceData(bytes);
    assertThat(data.getLinesCount()).isEqualTo(build.getLinesCount());
  }

  private static DbFileSources.Data createOver64MBDataStructure() {
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    DbFileSources.Line.Builder lineBuilder = DbFileSources.Line.newBuilder();
    for (int i = 0; i < 199999; i++) {
      dataBuilder.addLines(
        lineBuilder.setSource(LOREM_IPSUM)
          .setLine(i)
          .build());
    }
    return dataBuilder.build();
  }

  @Test
  public void new_FileSourceDto_as_lineCount_0_and_rawLineHashes_to_null() {
    FileSourceDto underTest = new FileSourceDto();

    assertThat(underTest.getLineCount()).isZero();
    assertThat(underTest.getLineHashes()).isEmpty();
    assertThat(underTest.getRawLineHashes()).isNull();
  }

  @Test
  public void setLineHashes_null_sets_lineCount_to_0_and_rawLineHashes_to_null() {
    FileSourceDto underTest = new FileSourceDto();
    underTest.setLineHashes(null);

    assertThat(underTest.getLineCount()).isZero();
    assertThat(underTest.getLineHashes()).isEmpty();
    assertThat(underTest.getRawLineHashes()).isNull();
  }

  @Test
  public void setLineHashes_empty_sets_lineCount_to_1_and_rawLineHashes_to_null() {
    FileSourceDto underTest = new FileSourceDto();
    underTest.setLineHashes(Collections.emptyList());

    assertThat(underTest.getLineCount()).isEqualTo(1);
    assertThat(underTest.getLineHashes()).isEmpty();
    assertThat(underTest.getRawLineHashes()).isNull();
  }

  @Test
  public void setLineHashes_sets_lineCount_to_size_of_list_and_rawLineHashes_to_join_by_line_return() {
    FileSourceDto underTest = new FileSourceDto();
    int expected = 1 + new Random().nextInt(96);
    List<String> lineHashes = IntStream.range(0, expected).mapToObj(String::valueOf).collect(Collectors.toList());
    underTest.setLineHashes(lineHashes);

    assertThat(underTest.getLineCount()).isEqualTo(expected);
    assertThat(underTest.getRawLineHashes()).isEqualTo(Joiner.on('\n').join(lineHashes));
  }
}
