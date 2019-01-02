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
package org.sonar.scanner.protocol.input;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SingleProjectRepositoryTest {
  private SingleProjectRepository repository;

  @Before
  public void setUp() {
    repository = new SingleProjectRepository();
  }

  @Test
  public void add_file_data() {
    FileData fileData = new FileData("123", "456");

    repository.addFileData("/Abc.java", fileData);

    assertThat(repository.fileData()).hasSize(1);
    assertThat(repository.fileData()).contains(Maps.immutableEntry("/Abc.java", fileData));
    assertThat(repository.fileDataByPath("/Abc.java")).isEqualTo(fileData);
  }

  @Test
  public void add_file_data_doesnt_add_the_file_without_path() {
    FileData fileData = new FileData("123", "456");

    repository.addFileData(null, fileData);

    assertThat(repository.fileData()).hasSize(0);
  }

  @Test
  public void add_file_data_doesnt_add_the_file_without_revision_and_hash() {
    FileData fileData = new FileData(null, null);

    repository.addFileData("/Abc.java", fileData);

    assertThat(repository.fileData()).hasSize(0);
  }
}
