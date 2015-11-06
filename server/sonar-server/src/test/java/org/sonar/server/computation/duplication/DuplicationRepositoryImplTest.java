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
package org.sonar.server.computation.duplication;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Set;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.util.WrapInSingleElementArray;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class DuplicationRepositoryImplTest {

  private static final Component COMPONENT_1 = ReportComponent.builder(Component.Type.FILE, 1).build();
  private static final Component COMPONENT_2 = ReportComponent.builder(Component.Type.FILE, 2).build();
  private static final Component COMPONENT_3 = ReportComponent.builder(Component.Type.FILE, 3).build();
  private static final TextBlock ORIGINAL_TEXTBLOCK = new TextBlock(1, 2);
  private static final TextBlock COPY_OF_ORIGINAL_TEXTBLOCK = new TextBlock(1, 2);
  private static final TextBlock DUPLICATE_TEXTBLOCK_1 = new TextBlock(15, 15);
  private static final TextBlock DUPLICATE_TEXTBLOCK_2 = new TextBlock(15, 16);
  private static final String FILE_KEY_1 = "1";
  private static final String FILE_KEY_2 = "2";

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
    assertNoDuplication(COMPONENT_1);
  }

  @Test
  public void addDuplication_inner_throws_NPE_if_file_argument_is_null() {
    expectFileArgumentNPE();

    underTest.addDuplication(null, ORIGINAL_TEXTBLOCK, DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  public void addDuplication_inner_throws_NPE_if_original_argument_is_null() {
    expectOriginalArgumentNPE();

    underTest.addDuplication(COMPONENT_1, null, DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  public void addDuplication_inner_throws_NPE_if_duplicate_argument_is_null() {
    expectDuplicateArgumentNPE();

    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, null);
  }

  @Test
  @UseDataProvider("allComponentTypesButFile")
  public void addDuplication_inner_throws_IAE_if_file_type_is_not_FILE(Component.Type type) {
    expectFileTypeIAE();

    Component component = mockComponentGetType(type);

    underTest.addDuplication(component, ORIGINAL_TEXTBLOCK, DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  public void addDuplication_inner_throws_IAE_if_original_and_duplicate_are_equal() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("original and duplicate TextBlocks can not be the same");

    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, COPY_OF_ORIGINAL_TEXTBLOCK);
  }

  @Test
  public void addDuplication_inner_throws_IAE_if_duplication_already_exists() {
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, DUPLICATE_TEXTBLOCK_1);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format(
        "Inner duplicate %s is already associated to original %s in file %s",
        DUPLICATE_TEXTBLOCK_1, ORIGINAL_TEXTBLOCK, COMPONENT_1.getKey()));

    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  public void addDuplication_inner_throws_IAE_if_reverse_duplication_already_exists() {
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, DUPLICATE_TEXTBLOCK_1);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format(
        "Inner duplicate %s is already associated to original %s in file %s",
        ORIGINAL_TEXTBLOCK, DUPLICATE_TEXTBLOCK_1, COMPONENT_1.getKey()));

    underTest.addDuplication(COMPONENT_1, DUPLICATE_TEXTBLOCK_1, ORIGINAL_TEXTBLOCK);
  }

  @Test
  public void addDuplication_inner_throws_IAE_if_reverse_duplication_already_exists_and_duplicate_has_duplicates_of_its_own() {
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, DUPLICATE_TEXTBLOCK_1);
    underTest.addDuplication(COMPONENT_1, DUPLICATE_TEXTBLOCK_1, DUPLICATE_TEXTBLOCK_2);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format(
        "Inner duplicate %s is already associated to original %s in file %s",
        ORIGINAL_TEXTBLOCK, DUPLICATE_TEXTBLOCK_1, COMPONENT_1.getKey()));

    underTest.addDuplication(COMPONENT_1, DUPLICATE_TEXTBLOCK_1, ORIGINAL_TEXTBLOCK);
  }

  @Test
  public void addDuplication_inner_is_returned_by_getDuplications() {
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, DUPLICATE_TEXTBLOCK_1);

    Set<Duplication> duplications = underTest.getDuplications(COMPONENT_1);
    assertThat(duplications).hasSize(1);
    assertDuplication(
        duplications.iterator().next(),
        ORIGINAL_TEXTBLOCK,
        new InnerDuplicate(DUPLICATE_TEXTBLOCK_1));

    assertNoDuplication(COMPONENT_2);
  }

  @Test
  public void addDuplication_inner_called_multiple_times_populate_a_single_Duplication() {
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, DUPLICATE_TEXTBLOCK_2);
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, DUPLICATE_TEXTBLOCK_1);

    Set<Duplication> duplications = underTest.getDuplications(COMPONENT_1);
    assertThat(duplications).hasSize(1);
    assertDuplication(
        duplications.iterator().next(),
        ORIGINAL_TEXTBLOCK,
        new InnerDuplicate(DUPLICATE_TEXTBLOCK_1), new InnerDuplicate(DUPLICATE_TEXTBLOCK_2));

    assertNoDuplication(COMPONENT_2);
  }

  @Test
  public void addDuplication_inProject_throws_NPE_if_file_argument_is_null() {
    expectFileArgumentNPE();

    underTest.addDuplication(null, ORIGINAL_TEXTBLOCK, COMPONENT_2, DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  public void addDuplication_inProject_throws_NPE_if_original_argument_is_null() {
    expectOriginalArgumentNPE();

    underTest.addDuplication(COMPONENT_1, null, COMPONENT_2, DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  public void addDuplication_inProject_throws_NPE_if_otherFile_argument_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("otherFile can not be null");

    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, (Component) null, DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  @UseDataProvider("allComponentTypesButFile")
  public void addDuplication_inProject_throws_NPE_if_otherFile_type_is_not_FILE(Component.Type type) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("type of otherFile argument must be FILE");

    Component component = mockComponentGetType(type);

    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, component, DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  public void addDuplication_inProject_throws_NPE_if_duplicate_argument_is_null() {
    expectDuplicateArgumentNPE();

    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, COMPONENT_2, null);
  }

  @Test
  @UseDataProvider("allComponentTypesButFile")
  public void addDuplication_inProject_throws_NPE_if_file_type_is_not_FILE(Component.Type type) {
    expectFileTypeIAE();

    Component component = mockComponentGetType(type);

    underTest.addDuplication(component, ORIGINAL_TEXTBLOCK, COMPONENT_2, DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  public void addDuplication_inProject_throws_NPE_if_file_and_otherFile_are_the_same() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("file and otherFile Components can not be the same");

    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, COMPONENT_1, DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  public void addDuplication_inProject_throws_IAE_if_duplication_already_exists() {
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, COMPONENT_2, DUPLICATE_TEXTBLOCK_1);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format(
        "In-project duplicate %s in file %s is already associated to original %s in file %s",
        DUPLICATE_TEXTBLOCK_1, COMPONENT_2.getKey(), ORIGINAL_TEXTBLOCK, COMPONENT_1.getKey()));

    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, COMPONENT_2, DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  public void addDuplication_inProject_is_returned_by_getDuplications() {
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, COMPONENT_2, DUPLICATE_TEXTBLOCK_1);

    Set<Duplication> duplications = underTest.getDuplications(COMPONENT_1);
    assertThat(duplications).hasSize(1);
    assertDuplication(
        duplications.iterator().next(),
        ORIGINAL_TEXTBLOCK,
        new InProjectDuplicate(COMPONENT_2, DUPLICATE_TEXTBLOCK_1));

    assertNoDuplication(COMPONENT_2);
  }

  @Test
  public void addDuplication_inProject_called_multiple_times_populate_a_single_Duplication() {
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, COMPONENT_2, DUPLICATE_TEXTBLOCK_2);
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, COMPONENT_2, DUPLICATE_TEXTBLOCK_1);

    Set<Duplication> duplications = underTest.getDuplications(COMPONENT_1);
    assertThat(duplications).hasSize(1);
    assertDuplication(
        duplications.iterator().next(),
        ORIGINAL_TEXTBLOCK,
        new InProjectDuplicate(COMPONENT_2, DUPLICATE_TEXTBLOCK_1), new InProjectDuplicate(COMPONENT_2, DUPLICATE_TEXTBLOCK_2));

    assertNoDuplication(COMPONENT_2);
  }

  @Test
  public void addDuplication_inProject_called_multiple_times_with_different_components_populate_a_single_Duplication() {
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, COMPONENT_2, DUPLICATE_TEXTBLOCK_2);
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, COMPONENT_3, DUPLICATE_TEXTBLOCK_2);
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, COMPONENT_3, DUPLICATE_TEXTBLOCK_1);

    Set<Duplication> duplications = underTest.getDuplications(COMPONENT_1);
    assertThat(duplications).hasSize(1);
    assertDuplication(
        duplications.iterator().next(),
        ORIGINAL_TEXTBLOCK,
        new InProjectDuplicate(COMPONENT_2, DUPLICATE_TEXTBLOCK_2), new InProjectDuplicate(COMPONENT_3, DUPLICATE_TEXTBLOCK_1), new InProjectDuplicate(COMPONENT_3, DUPLICATE_TEXTBLOCK_2));

    assertNoDuplication(COMPONENT_2);
    assertNoDuplication(COMPONENT_3);
  }

  @Test
  public void addDuplication_crossProject_throws_NPE_if_file_argument_is_null() {
    expectFileArgumentNPE();

    underTest.addDuplication(null, ORIGINAL_TEXTBLOCK, FILE_KEY_1, DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  public void addDuplication_crossProject_throws_NPE_if_original_argument_is_null() {
    expectOriginalArgumentNPE();

    underTest.addDuplication(COMPONENT_1, null, FILE_KEY_1, DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  public void addDuplication_crossProject_throws_NPE_if_otherFileKey_argument_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("otherFileKey can not be null");

    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, (String) null, DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  public void addDuplication_crossProject_throws_NPE_if_duplicate_argument_is_null() {
    expectDuplicateArgumentNPE();

    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, FILE_KEY_1, null);
  }

  @Test
  @Ignore
  public void addDuplication_crossProject_throws_IAE_if_otherFileKey_is_key_of_Component_in_the_project() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("type of file argument must be FILE");

    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, COMPONENT_2.getKey(), DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  @UseDataProvider("allComponentTypesButFile")
  public void addDuplication_crossProject_throws_NPE_if_file_type_is_not_FILE(Component.Type type) {
    expectFileTypeIAE();

    Component component = mockComponentGetType(type);

    underTest.addDuplication(component, ORIGINAL_TEXTBLOCK, FILE_KEY_1, DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  public void addDuplication_crossProject_throws_IAE_if_duplication_already_exists() {
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, FILE_KEY_1, DUPLICATE_TEXTBLOCK_1);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format(
        "Cross-project duplicate %s in file %s is already associated to original %s in file %s",
        DUPLICATE_TEXTBLOCK_1, FILE_KEY_1, ORIGINAL_TEXTBLOCK, COMPONENT_1.getKey()));

    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, FILE_KEY_1, DUPLICATE_TEXTBLOCK_1);
  }

  @Test
  public void addDuplication_crossProject_is_returned_by_getDuplications() {
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, FILE_KEY_1, DUPLICATE_TEXTBLOCK_1);

    Set<Duplication> duplications = underTest.getDuplications(COMPONENT_1);
    assertThat(duplications).hasSize(1);
    assertDuplication(
        duplications.iterator().next(),
        ORIGINAL_TEXTBLOCK,
        new CrossProjectDuplicate(FILE_KEY_1, DUPLICATE_TEXTBLOCK_1));

    assertNoDuplication(COMPONENT_2);
  }

  @Test
  public void addDuplication_crossProject_called_multiple_times_populate_a_single_Duplication() {
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, FILE_KEY_1, DUPLICATE_TEXTBLOCK_2);
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, FILE_KEY_1, DUPLICATE_TEXTBLOCK_1);

    Set<Duplication> duplications = underTest.getDuplications(COMPONENT_1);
    assertThat(duplications).hasSize(1);
    assertDuplication(
        duplications.iterator().next(),
        ORIGINAL_TEXTBLOCK,
        new CrossProjectDuplicate(FILE_KEY_1, DUPLICATE_TEXTBLOCK_1), new CrossProjectDuplicate(FILE_KEY_1, DUPLICATE_TEXTBLOCK_2));

    assertNoDuplication(COMPONENT_2);
  }

  @Test
  public void addDuplication_crossProject_called_multiple_times_with_different_fileKeys_populate_a_single_Duplication() {
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, FILE_KEY_1, DUPLICATE_TEXTBLOCK_2);
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, FILE_KEY_2, DUPLICATE_TEXTBLOCK_2);
    underTest.addDuplication(COMPONENT_1, ORIGINAL_TEXTBLOCK, FILE_KEY_2, DUPLICATE_TEXTBLOCK_1);

    Set<Duplication> duplications = underTest.getDuplications(COMPONENT_1);
    assertThat(duplications).hasSize(1);
    assertDuplication(
        duplications.iterator().next(),
        ORIGINAL_TEXTBLOCK,
        new CrossProjectDuplicate(FILE_KEY_1, DUPLICATE_TEXTBLOCK_2), new CrossProjectDuplicate(FILE_KEY_2, DUPLICATE_TEXTBLOCK_1), new CrossProjectDuplicate(FILE_KEY_2, DUPLICATE_TEXTBLOCK_2));

    assertNoDuplication(COMPONENT_2);
  }

  @DataProvider
  public static Object[][] allComponentTypesButFile() {
    return from(Arrays.asList(Component.Type.values()))
        .filter(not(equalTo(Component.Type.FILE)))
        .transform(WrapInSingleElementArray.INSTANCE)
        .toArray(Object[].class);
  }

  private static void assertDuplication(Duplication duplication, TextBlock original, Duplicate... duplicates) {
    assertThat(duplication.getOriginal()).isEqualTo(original);
    assertThat(duplication.getDuplicates()).containsExactly(duplicates);
  }

  private void assertNoDuplication(Component component) {
    assertThat(underTest.getDuplications(component)).isEmpty();
  }

  private void expectFileArgumentNPE() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("file can not be null");
  }

  private void expectOriginalArgumentNPE() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("original can not be null");
  }

  private void expectDuplicateArgumentNPE() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("duplicate can not be null");
  }

  private void expectFileTypeIAE() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("type of file argument must be FILE");
  }

  private Component mockComponentGetType(Component.Type type) {
    Component component = mock(Component.class);
    when(component.getType()).thenReturn(type);
    return component;
  }
}
