/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.protobuf.DbFileSources;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSourceDtoTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void encode_and_decode_test_data() {
    List<DbFileSources.Test> tests = Arrays.asList(
      DbFileSources.Test.newBuilder()
        .setName("name#1")
        .build(),
      DbFileSources.Test.newBuilder()
        .setName("name#2")
        .build());

    FileSourceDto underTest = new FileSourceDto().setTestData(tests);

    assertThat(underTest.getTestData()).hasSize(2);
    assertThat(underTest.getTestData().get(0).getName()).isEqualTo("name#1");
  }

  @Test
  public void getSourceData_throws_ISE_with_id_fileUuid_and_projectUuid_in_message_when_data_cant_be_read() {
    long id = 12L;
    String fileUuid = "file uuid";
    String projectUuid = "project uuid";
    FileSourceDto underTest = new FileSourceDto()
        .setBinaryData(new byte[]{1, 2, 3, 4, 5})
        .setId(id)
        .setFileUuid(fileUuid)
        .setProjectUuid(projectUuid);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to decompress and deserialize source data [id=" + id + ",fileUuid=" + fileUuid + ",projectUuid=" + projectUuid + "]");

    underTest.getSourceData();
  }
}
