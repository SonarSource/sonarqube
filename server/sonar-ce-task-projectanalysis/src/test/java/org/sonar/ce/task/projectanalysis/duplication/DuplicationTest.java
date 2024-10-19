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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class DuplicationTest {
  private static final TextBlock SOME_ORIGINAL_TEXTBLOCK = new TextBlock(1, 2);
  private static final TextBlock TEXT_BLOCK_1 = new TextBlock(2, 2);
  private static final TextBlock TEXT_BLOCK_2 = new TextBlock(2, 3);
  private static final ReportComponent FILE_COMPONENT_1 = ReportComponent.builder(Component.Type.FILE, 1).build();
  private static final ReportComponent FILE_COMPONENT_2 = ReportComponent.builder(Component.Type.FILE, 2).build();
  private static final String FILE_KEY_1 = "1";
  private static final String FILE_KEY_2 = "2";


  @Test
  public void constructor_throws_NPE_if_original_is_null() {
    assertThatThrownBy(() -> new Duplication(null, Collections.emptyList()))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("original TextBlock can not be null");
  }

  @Test
  public void constructor_throws_NPE_if_duplicates_is_null() {
    assertThatThrownBy(() -> new Duplication(SOME_ORIGINAL_TEXTBLOCK, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("duplicates can not be null");
  }

  @Test
  public void constructor_throws_IAE_if_duplicates_is_empty() {
    assertThatThrownBy(() -> new Duplication(SOME_ORIGINAL_TEXTBLOCK, Collections.emptyList()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("duplicates can not be empty");
  }

  @Test
  public void constructor_throws_NPE_if_duplicates_contains_null() {
    assertThatThrownBy(() -> new Duplication(SOME_ORIGINAL_TEXTBLOCK, Arrays.asList(mock(Duplicate.class), null, mock(Duplicate.class))))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("duplicates can not contain null");
  }

  @Test
  public void constructor_throws_IAE_if_duplicates_contains_InnerDuplicate_of_original() {
    assertThatThrownBy(() ->  new Duplication(SOME_ORIGINAL_TEXTBLOCK, Arrays.asList(mock(Duplicate.class), new InnerDuplicate(SOME_ORIGINAL_TEXTBLOCK), mock(Duplicate.class))))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("TextBlock of an InnerDuplicate can not be the original TextBlock");
  }

  @Test
  public void constructor_throws_IAE_when_attempting_to_sort_Duplicate_of_unkown_type() {
    assertThatThrownBy(() -> new Duplication(SOME_ORIGINAL_TEXTBLOCK, Arrays.asList(new MyDuplicate(), new MyDuplicate())))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unsupported type of Duplicate " + MyDuplicate.class.getName());
  }

  private static final class MyDuplicate implements Duplicate {

    @Override
    public TextBlock getTextBlock() {
      throw new UnsupportedOperationException("getTextBlock not implemented");
    }
  }

  @Test
  public void getOriginal_returns_original() {
    assertThat(new Duplication(SOME_ORIGINAL_TEXTBLOCK, Arrays.asList(new InnerDuplicate(TEXT_BLOCK_1)))
      .getOriginal()).isSameAs(SOME_ORIGINAL_TEXTBLOCK);
  }

  @Test
  public void getDuplicates_sorts_duplicates_by_Inner_then_InProject_then_CrossProject() {
    CrossProjectDuplicate crossProjectDuplicate = new CrossProjectDuplicate("some key", TEXT_BLOCK_1);
    InProjectDuplicate inProjectDuplicate = new InProjectDuplicate(FILE_COMPONENT_1, TEXT_BLOCK_1);
    InnerDuplicate innerDuplicate = new InnerDuplicate(TEXT_BLOCK_1);

    Duplication duplication = new Duplication(
      SOME_ORIGINAL_TEXTBLOCK,
      shuffledList(crossProjectDuplicate, inProjectDuplicate, innerDuplicate));

    assertThat(duplication.getDuplicates()).containsExactly(innerDuplicate, inProjectDuplicate, crossProjectDuplicate);
  }

  @Test
  public void getDuplicates_sorts_duplicates_of_InnerDuplicate_by_TextBlock() {
    InnerDuplicate innerDuplicate1 = new InnerDuplicate(TEXT_BLOCK_2);
    InnerDuplicate innerDuplicate2 = new InnerDuplicate(new TextBlock(3, 3));
    InnerDuplicate innerDuplicate3 = new InnerDuplicate(new TextBlock(3, 4));
    InnerDuplicate innerDuplicate4 = new InnerDuplicate(new TextBlock(4, 4));

    assertGetDuplicatesSorting(innerDuplicate1, innerDuplicate2, innerDuplicate3, innerDuplicate4);
  }

  @Test
  public void getDuplicates_sorts_duplicates_of_InProjectDuplicate_by_component_then_TextBlock() {
    InProjectDuplicate innerDuplicate1 = new InProjectDuplicate(FILE_COMPONENT_1, TEXT_BLOCK_1);
    InProjectDuplicate innerDuplicate2 = new InProjectDuplicate(FILE_COMPONENT_1, TEXT_BLOCK_2);
    InProjectDuplicate innerDuplicate3 = new InProjectDuplicate(FILE_COMPONENT_2, TEXT_BLOCK_1);
    InProjectDuplicate innerDuplicate4 = new InProjectDuplicate(FILE_COMPONENT_2, TEXT_BLOCK_2);

    assertGetDuplicatesSorting(innerDuplicate1, innerDuplicate2, innerDuplicate3, innerDuplicate4);
  }

  @Test
  public void getDuplicates_sorts_duplicates_of_CrossProjectDuplicate_by_fileKey_then_TextBlock() {
    CrossProjectDuplicate innerDuplicate1 = new CrossProjectDuplicate(FILE_KEY_1, TEXT_BLOCK_1);
    CrossProjectDuplicate innerDuplicate2 = new CrossProjectDuplicate(FILE_KEY_1, TEXT_BLOCK_2);
    CrossProjectDuplicate innerDuplicate3 = new CrossProjectDuplicate(FILE_KEY_2, TEXT_BLOCK_1);
    CrossProjectDuplicate innerDuplicate4 = new CrossProjectDuplicate(FILE_KEY_2, TEXT_BLOCK_2);

    assertGetDuplicatesSorting(innerDuplicate1, innerDuplicate2, innerDuplicate3, innerDuplicate4);
  }

  @Test
  public void equals_compares_on_original_and_duplicates() {
    Duplication duplication = new Duplication(SOME_ORIGINAL_TEXTBLOCK, Arrays.asList(new InnerDuplicate(TEXT_BLOCK_1)));

    assertThat(duplication)
      .isEqualTo(duplication)
      .isEqualTo(new Duplication(SOME_ORIGINAL_TEXTBLOCK, Arrays.asList(new InnerDuplicate(TEXT_BLOCK_1))))
      .isNotEqualTo(new Duplication(SOME_ORIGINAL_TEXTBLOCK, Arrays.asList(new InnerDuplicate(TEXT_BLOCK_2))))
      .isNotEqualTo(new Duplication(TEXT_BLOCK_1, Arrays.asList(new InnerDuplicate(SOME_ORIGINAL_TEXTBLOCK))));
  }

  @Test
  public void hashcode_is_based_on_original_only() {
    Duplication duplication = new Duplication(SOME_ORIGINAL_TEXTBLOCK, Arrays.asList(new InnerDuplicate(TEXT_BLOCK_1)));

    assertThat(duplication).hasSameHashCodeAs(new Duplication(SOME_ORIGINAL_TEXTBLOCK, Arrays.asList(new InnerDuplicate(TEXT_BLOCK_1))));
    assertThat(duplication.hashCode()).isNotEqualTo(new Duplication(SOME_ORIGINAL_TEXTBLOCK, Arrays.asList(new InnerDuplicate(TEXT_BLOCK_2))).hashCode());
    assertThat(duplication.hashCode()).isNotEqualTo(new Duplication(TEXT_BLOCK_1, Arrays.asList(new InnerDuplicate(SOME_ORIGINAL_TEXTBLOCK))).hashCode());
  }

  @Test
  public void verify_toString() {
    Duplication duplication = new Duplication(
      SOME_ORIGINAL_TEXTBLOCK,
      Arrays.asList(new InnerDuplicate(TEXT_BLOCK_1)));

    assertThat(duplication)
      .hasToString("Duplication{original=TextBlock{start=1, end=2}, duplicates=[InnerDuplicate{textBlock=TextBlock{start=2, end=2}}]}");
  }

  @SafeVarargs
  private final <T extends Duplicate> void assertGetDuplicatesSorting(T... expected) {
    Duplication duplication = new Duplication(SOME_ORIGINAL_TEXTBLOCK, shuffledList(expected));

    assertThat(duplication.getDuplicates()).containsExactly(expected);
  }

  private static List<Duplicate> shuffledList(Duplicate... duplicates) {
    List<Duplicate> res = new ArrayList<>(Arrays.asList(duplicates));
    Collections.shuffle(res);
    return res;
  }
}
