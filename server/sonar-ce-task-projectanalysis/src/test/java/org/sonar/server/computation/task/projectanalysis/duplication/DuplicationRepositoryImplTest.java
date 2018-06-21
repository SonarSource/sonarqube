/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.duplication;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.util.WrapInSingleElementArray;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class DuplicationRepositoryImplTest {
  private static final Component FILE_COMPONENT_1 = ReportComponent.builder(Component.Type.FILE, 1).build();
  private static final Component FILE_COMPONENT_2 = ReportComponent.builder(Component.Type.FILE, 2).build();
  private static final Duplication SOME_DUPLICATION = createDuplication(1, 2);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DuplicationRepository underTest = new DuplicationRepositoryImpl();

  @Test
  public void getDuplications_throws_NPE_if_Component_argument_is_null() {
    expectFileArgumentNPE();

    underTest.getDuplications(null);
  }

  @Test
  @UseDataProvider("allComponentTypesButFile")
  public void getDuplications_throws_IAE_if_Component_type_is_not_FILE(Component.Type type) {
    expectFileTypeIAE();

    Component component = mockComponentGetType(type);

    underTest.getDuplications(component);
  }

  @Test
  public void getDuplications_returns_empty_set_when_repository_is_empty() {
    assertNoDuplication(FILE_COMPONENT_1);
  }

  @Test
  public void add_throws_NPE_if_file_argument_is_null() {
    expectFileArgumentNPE();

    underTest.add(null, SOME_DUPLICATION);
  }

  @Test
  public void addDuplication_inner_throws_NPE_if_duplication_argument_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("duplication can not be null");

    underTest.add(FILE_COMPONENT_1, null);
  }

  @Test
  @UseDataProvider("allComponentTypesButFile")
  public void addDuplication_inner_throws_IAE_if_file_type_is_not_FILE(Component.Type type) {
    expectFileTypeIAE();

    Component component = mockComponentGetType(type);

    underTest.add(component, SOME_DUPLICATION);
  }

  @Test
  public void added_duplication_is_returned_as_is_by_getDuplications() {
    underTest.add(FILE_COMPONENT_1, SOME_DUPLICATION);

    Iterable<Duplication> duplications = underTest.getDuplications(FILE_COMPONENT_1);
    assertThat(duplications).hasSize(1);
    assertThat(duplications.iterator().next()).isSameAs(SOME_DUPLICATION);

    assertNoDuplication(FILE_COMPONENT_2);
  }

  @Test
  public void added_duplication_does_not_avoid_same_duplication_inserted_twice_but_only_one_is_returned() {
    underTest.add(FILE_COMPONENT_1, SOME_DUPLICATION);
    underTest.add(FILE_COMPONENT_1, SOME_DUPLICATION);

    Iterable<Duplication> duplications = underTest.getDuplications(FILE_COMPONENT_1);
    assertThat(duplications).hasSize(1);
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
    return from(Arrays.asList(Component.Type.values()))
      .filter(not(equalTo(Component.Type.FILE)))
      .transform(WrapInSingleElementArray.INSTANCE)
      .toArray(Object[].class);
  }

  private void assertNoDuplication(Component component) {
    assertThat(underTest.getDuplications(component)).isEmpty();
  }

  private void expectFileArgumentNPE() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("file can not be null");
  }

  private void expectFileTypeIAE() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("type of file must be FILE");
  }

  private Component mockComponentGetType(Component.Type type) {
    Component component = mock(Component.class);
    when(component.getType()).thenReturn(type);
    return component;
  }
}
