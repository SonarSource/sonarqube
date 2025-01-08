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
package org.sonar.ce.task.projectanalysis.filemove;

import java.util.Random;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ViewsComponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class MutableMovedFilesRepositoryImplTest {
  private static final Component SOME_FILE = builder(Component.Type.FILE, 1).build();
  private static final Component[] COMPONENTS_EXCEPT_FILE = {
    builder(Component.Type.PROJECT, 1).build(),
    builder(Component.Type.DIRECTORY, 1).build(),
    ViewsComponent.builder(Component.Type.VIEW, 1).build(),
    ViewsComponent.builder(Component.Type.SUBVIEW, 1).build(),
    ViewsComponent.builder(Component.Type.PROJECT_VIEW, 1).build()
  };
  private static final MovedFilesRepository.OriginalFile SOME_ORIGINAL_FILE = new MovedFilesRepository.OriginalFile("uuid for 100", "key for 100");

  private MutableMovedFilesRepositoryImpl underTest = new MutableMovedFilesRepositoryImpl();

  @Test
  public void setOriginalFile_throws_NPE_when_file_is_null() {
    assertThatThrownBy(() -> underTest.setOriginalFile(null, SOME_ORIGINAL_FILE))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("file can't be null");
  }

  @Test
  public void setOriginalFile_throws_NPE_when_originalFile_is_null() {
    assertThatThrownBy(() -> underTest.setOriginalFile(SOME_FILE, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("originalFile can't be null");
  }

  @Test
  public void setOriginalFile_throws_IAE_when_type_is_no_FILE() {
    for (Component component : COMPONENTS_EXCEPT_FILE) {
      try {
        underTest.setOriginalFile(component, SOME_ORIGINAL_FILE);
        fail("should have raised a NPE");
      } catch (IllegalArgumentException e) {
        assertThat(e)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("file must be of type FILE");
      }
    }
  }

  @Test
  public void setOriginalFile_throws_ISE_if_settings_another_originalFile() {
    underTest.setOriginalFile(SOME_FILE, SOME_ORIGINAL_FILE);

    assertThatThrownBy(() -> underTest.setOriginalFile(SOME_FILE, new MovedFilesRepository.OriginalFile("uudi", "key")))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Original file OriginalFile{uuid='uuid for 100', key='key for 100'} " +
        "already registered for file ReportComponent{ref=1, key='key_1', type=FILE}");
  }

  @Test
  public void setOriginalFile_does_not_fail_if_same_original_file_is_added_multiple_times_for_the_same_component() {
    underTest.setOriginalFile(SOME_FILE, SOME_ORIGINAL_FILE);

    for (int i = 0; i < 1 + Math.abs(new Random().nextInt(10)); i++) {
      underTest.setOriginalFile(SOME_FILE, SOME_ORIGINAL_FILE);
    }
  }

  @Test
  public void setOriginalFile_does_not_fail_when_originalFile_is_added_twice_for_different_files() {
    underTest.setOriginalFile(SOME_FILE, SOME_ORIGINAL_FILE);
    underTest.setOriginalFile(builder(Component.Type.FILE, 2).build(), SOME_ORIGINAL_FILE);
  }

  @Test
  public void getOriginalFile_throws_NPE_when_file_is_null() {
    assertThatThrownBy(() -> underTest.getOriginalFile(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("file can't be null");
  }

  @Test
  public void getOriginalFile_returns_absent_for_any_component_type_when_empty() {
    assertThat(underTest.getOriginalFile(SOME_FILE)).isEmpty();
    for (Component component : COMPONENTS_EXCEPT_FILE) {
      assertThat(underTest.getOriginalFile(component)).isEmpty();
    }
  }

  @Test
  public void getOriginalFile_returns_absent_for_any_type_of_Component_but_file_when_non_empty() {
    underTest.setOriginalFile(SOME_FILE, SOME_ORIGINAL_FILE);

    for (Component component : COMPONENTS_EXCEPT_FILE) {
      assertThat(underTest.getOriginalFile(component)).isEmpty();
    }
    assertThat(underTest.getOriginalFile(SOME_FILE)).contains(SOME_ORIGINAL_FILE);
  }

  @Test
  public void getOriginalFile_returns_originalFile_base_on_file_key() {
    underTest.setOriginalFile(SOME_FILE, SOME_ORIGINAL_FILE);

    assertThat(underTest.getOriginalFile(SOME_FILE)).contains(SOME_ORIGINAL_FILE);
    assertThat(underTest.getOriginalFile(builder(Component.Type.FILE, 1).setUuid("toto").build())).contains(SOME_ORIGINAL_FILE);
  }
}
