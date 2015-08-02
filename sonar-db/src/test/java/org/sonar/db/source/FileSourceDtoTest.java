/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.junit.Test;
import org.sonar.db.protobuf.DbFileSources;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSourceDtoTest {

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
}
