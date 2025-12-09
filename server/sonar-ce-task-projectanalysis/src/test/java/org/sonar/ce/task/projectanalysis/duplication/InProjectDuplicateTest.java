/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.duplication;

import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InProjectDuplicateTest {
  private static final Component FILE_1 = ReportComponent.builder(Component.Type.FILE, 1).build();
  private static final Component FILE_2 = ReportComponent.builder(Component.Type.FILE, 2).build();


  @Test
  public void constructors_throws_NPE_if_file_is_null() {
    assertThatThrownBy(() -> new InProjectDuplicate(null, new TextBlock(1, 1)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("file can not be null");
  }

  @Test
  public void constructors_throws_NPE_if_textBlock_is_null() {
    assertThatThrownBy(() -> new InProjectDuplicate(FILE_1, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("textBlock of duplicate can not be null");
  }

  @Test
  public void constructors_throws_IAE_if_type_of_file_argument_is_not_FILE() {
    assertThatThrownBy(() -> new InProjectDuplicate(ReportComponent.builder(Component.Type.PROJECT, 1).build(), new TextBlock(1, 1)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("file must be of type FILE");
  }

  @Test
  public void getTextBlock_returns_TextBlock_constructor_argument() {
    TextBlock textBlock = new TextBlock(2, 3);

    assertThat(new InProjectDuplicate(FILE_1, textBlock).getTextBlock()).isSameAs(textBlock);
  }

  @Test
  public void getFile_returns_Component_constructor_argument() {
    assertThat(new InProjectDuplicate(FILE_1, new TextBlock(2, 3)).getFile()).isSameAs(FILE_1);
  }

  @Test
  public void equals_compares_on_file_and_TextBlock() {
    TextBlock textBlock1 = new TextBlock(1, 2);

    assertThat(new InProjectDuplicate(FILE_1, textBlock1)).isEqualTo(new InProjectDuplicate(FILE_1, new TextBlock(1, 2)));

    assertThat(new InProjectDuplicate(FILE_1, textBlock1)).isNotEqualTo(new InProjectDuplicate(FILE_1, new TextBlock(1, 1)));
    assertThat(new InProjectDuplicate(FILE_1, textBlock1)).isNotEqualTo(new InProjectDuplicate(FILE_2, textBlock1));
  }

  @Test
  public void hashcode_depends_on_file_and_TextBlock() {
    TextBlock textBlock = new TextBlock(1, 2);
    assertThat(new InProjectDuplicate(FILE_1, textBlock)).hasSameHashCodeAs(new InProjectDuplicate(FILE_1, textBlock));

    assertThat(new InProjectDuplicate(FILE_1, textBlock).hashCode()).isNotEqualTo(new InProjectDuplicate(FILE_2, textBlock).hashCode());
    assertThat(new InProjectDuplicate(FILE_1, textBlock).hashCode()).isNotEqualTo(new InProjectDuplicate(FILE_2, new TextBlock(1, 1)).hashCode());
  }

  @Test
  public void verify_toString() {
    assertThat(new InProjectDuplicate(FILE_1, new TextBlock(1, 2)))
      .hasToString("InProjectDuplicate{file=ReportComponent{ref=1, key='key_1', type=FILE}, textBlock=TextBlock{start=1, end=2}}");
  }

}
