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
package org.sonar.ce.task.projectanalysis.component;

import java.util.Map;
import org.junit.Test;
import org.sonar.db.source.FileHashesDto;
import org.sonar.db.source.FileSourceDto;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class PreviousSourceHashRepositoryImplTest {
  private final PreviousSourceHashRepositoryImpl previousFileHashesRepository = new PreviousSourceHashRepositoryImpl();

  @Test
  public void return_file_hashes() {
    Component file1 = ReportComponent.builder(Component.Type.FILE, 1).build();
    Component file2 = ReportComponent.builder(Component.Type.FILE, 2).build();
    Component file3 = ReportComponent.builder(Component.Type.FILE, 3).build();

    FileSourceDto fileSource1 = new FileSourceDto();
    FileSourceDto fileSource2 = new FileSourceDto();

    previousFileHashesRepository.set(Map.of(file1.getUuid(), fileSource1, file2.getUuid(), fileSource2));
    assertThat(previousFileHashesRepository.getDbFile(file1)).contains(fileSource1);
    assertThat(previousFileHashesRepository.getDbFile(file2)).contains(fileSource2);
    assertThat(previousFileHashesRepository.getDbFile(file3)).isEmpty();
  }

  @Test
  public void fail_if_not_set() {
    assertThatThrownBy(() -> previousFileHashesRepository.getDbFile(mock(Component.class))).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void fail_if_set_twice() {
    Map<String, FileHashesDto> empty = emptyMap();
    previousFileHashesRepository.set(empty);
    assertThatThrownBy(() -> previousFileHashesRepository.set(empty)).isInstanceOf(IllegalStateException.class);

  }
}
