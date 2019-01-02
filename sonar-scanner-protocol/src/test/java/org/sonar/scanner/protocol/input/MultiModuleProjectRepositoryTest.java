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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiModuleProjectRepositoryTest {

  private MultiModuleProjectRepository repository;

  @Before
  public void setUp() {
    repository = new MultiModuleProjectRepository();
  }

  @Test
  public void add_file_data_to_nodule() {
    FileData fileData1 = new FileData("123", "456");
    FileData fileData2 = new FileData("153", "6432");
    FileData fileData3 = new FileData("987", "6343");

    repository.addFileDataToModule("Module1", "/Abc.java", fileData1);
    repository.addFileDataToModule("Module1", "/Xyz.java", fileData2);
    repository.addFileDataToModule("Module2", "/Def.java", fileData3);

    assertThat(repository.repositoriesByModule()).hasSize(2);
    assertThat(repository.fileData("Module1", "/Xyz.java")).isEqualTo(fileData2);
    assertThat(repository.fileData("Module2", "/Def.java")).isEqualTo(fileData3);
  }

  @Test
  public void add_file_does_not_add_the_file_without_path() {
    FileData fileData = new FileData("123", "456");

    repository.addFileDataToModule("module1", null, fileData);

    assertThat(repository.repositoriesByModule()).hasSize(0);
  }

  @Test
  public void add_file_does_not_add_the_file_without_revision_and_hash() {
    FileData fileData = new FileData(null, null);

    repository.addFileDataToModule("module2", "/Abc.java", fileData);

    assertThat(repository.repositoriesByModule()).hasSize(0);
  }
}
