/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.util.WrapInSingleElementArray;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class DuplicationRepositoryImplTest {
  private static final Component FILE_COMPONENT_1 = ReportComponent.builder(Component.Type.FILE, 1).build();
  private static final Component FILE_COMPONENT_2 = ReportComponent.builder(Component.Type.FILE, 2).build();
  private static final Duplication SOME_DUPLICATION = createDuplication(1, 2);


  private DuplicationRepository underTest = new DuplicationRepositoryImpl();

  @Test
  public void getDuplications_throws_NPE_if_Component_argument_is_null() {
    assertThatThrownBy(() -> underTest.getDuplications(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("file can not be null");
  }

  @Test
  @UseDataProvider("allComponentTypesButFile")
  public void getDuplications_throws_IAE_if_Component_type_is_not_FILE(Component.Type type) {
    assertThatThrownBy(() -> {
      Component component = mockComponentGetType(type);
      underTest.getDuplications(component);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("type of file must be FILE");
  }

  @Test
  public void getDuplications_returns_empty_set_when_repository_is_empty() {
    assertNoDuplication(FILE_COMPONENT_1);
  }

  @Test
  public void add_throws_NPE_if_file_argument_is_null() {
    assertThatThrownBy(() -> underTest.add(null, SOME_DUPLICATION))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("file can not be null");
  }

  @Test
  public void addDuplication_inner_throws_NPE_if_duplication_argument_is_null() {
    assertThatThrownBy(() -> underTest.add(FILE_COMPONENT_1, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("duplication can not be null");
  }

  @Test
  @UseDataProvider("allComponentTypesButFile")
  public void addDuplication_inner_throws_IAE_if_file_type_is_not_FILE(Component.Type type) {
    assertThatThrownBy(() -> {
      Component component = mockComponentGetType(type);
      underTest.add(component, SOME_DUPLICATION);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("type of file must be FILE");
  }

  @Test
  public void added_duplication_is_returned_as_is_by_getDuplications() {
    underTest.add(FILE_COMPONENT_1, SOME_DUPLICATION);

    Iterable<Duplication> duplications = underTest.getDuplications(FILE_COMPONENT_1);
    Assertions.assertThat(duplications).hasSize(1);
    assertThat(duplications.iterator().next()).isSameAs(SOME_DUPLICATION);

    assertNoDuplication(FILE_COMPONENT_2);
  }

  @Test
  public void added_duplication_does_not_avoid_same_duplication_inserted_twice_but_only_one_is_returned() {
    underTest.add(FILE_COMPONENT_1, SOME_DUPLICATION);
    underTest.add(FILE_COMPONENT_1, SOME_DUPLICATION);

    Iterable<Duplication> duplications = underTest.getDuplications(FILE_COMPONENT_1);
    Assertions.assertThat(duplications).hasSize(1);
    assertThat(duplications.iterator().next()).isSameAs(SOME_DUPLICATION);

    assertNoDuplication(FILE_COMPONENT_2);
  }

  @Test
  public void added_duplications_are_returned_in_any_order_and_associated_to_the_right_file() {
    underTest.add(FILE_COMPONENT_1, SOME_DUPLICATION);
    underTest.add(FILE_COMPONENT_1, createDuplication(2, 4));
    underTest.add(FILE_COMPONENT_1, createDuplication(2, 3));
    underTest.add(FILE_COMPONENT_2, createDuplication(2, 3));
    underTest.add(FILE_COMPONENT_2, createDuplication(1, 2));

    assertThat(underTest.getDuplications(FILE_COMPONENT_1)).containsOnly(SOME_DUPLICATION, createDuplication(2, 3), createDuplication(2, 4));
    assertThat(underTest.getDuplications(FILE_COMPONENT_2)).containsOnly(createDuplication(1, 2), createDuplication(2, 3));
  }

  private static Duplication createDuplication(int originalLine, int duplicateLine) {
    return new Duplication(new TextBlock(originalLine, originalLine), Arrays.asList(new InnerDuplicate(new TextBlock(duplicateLine, duplicateLine))));
  }

  @DataProvider
  public static Object[][] allComponentTypesButFile() {
    return Arrays.stream(Component.Type.values())
      .filter(t -> t != Component.Type.FILE)
      .map(WrapInSingleElementArray.INSTANCE)
      .toArray(Object[][]::new);
  }

  private void assertNoDuplication(Component component) {
    assertThat(underTest.getDuplications(component)).isEmpty();
  }

  private Component mockComponentGetType(Component.Type type) {
    Component component = mock(Component.class);
    when(component.getType()).thenReturn(type);
    return component;
  }
}
