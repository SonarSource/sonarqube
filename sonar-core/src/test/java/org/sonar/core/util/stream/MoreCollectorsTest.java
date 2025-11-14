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
package org.sonar.core.util.stream;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Test;

import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.core.util.stream.MoreCollectors.unorderedFlattenIndex;
import static org.sonar.core.util.stream.MoreCollectors.unorderedIndex;

public class MoreCollectorsTest {

  private static final List<String> HUGE_LIST = IntStream.range(0, 2_000).mapToObj(String::valueOf).collect(java.util.stream.Collectors.toList());
  private static final Set<String> HUGE_SET = new HashSet<>(HUGE_LIST);
  private static final MyObj MY_OBJ_1_A = new MyObj(1, "A");
  private static final MyObj MY_OBJ_1_C = new MyObj(1, "C");
  private static final MyObj MY_OBJ_2_B = new MyObj(2, "B");
  private static final MyObj MY_OBJ_3_C = new MyObj(3, "C");
  private static final MyObj2 MY_OBJ2_1_A_X = new MyObj2(1, "A", "X");
  private static final MyObj2 MY_OBJ2_1_C = new MyObj2(1, "C");
  private static final MyObj2 MY_OBJ2_2_B = new MyObj2(2, "B");
  private static final MyObj2 MY_OBJ2_3_C = new MyObj2(3, "C");
  private static final List<MyObj> SINGLE_ELEMENT_LIST = Arrays.asList(MY_OBJ_1_A);
  private static final List<MyObj2> SINGLE_ELEMENT2_LIST = Arrays.asList(MY_OBJ2_1_A_X);
  private static final List<MyObj> LIST_WITH_DUPLICATE_ID = Arrays.asList(MY_OBJ_1_A, MY_OBJ_2_B, MY_OBJ_1_C);
  private static final List<MyObj2> LIST2_WITH_DUPLICATE_ID = Arrays.asList(MY_OBJ2_1_A_X, MY_OBJ2_2_B, MY_OBJ2_1_C);
  private static final List<MyObj> LIST = Arrays.asList(MY_OBJ_1_A, MY_OBJ_2_B, MY_OBJ_3_C);
  private static final List<MyObj2> LIST2 = Arrays.asList(MY_OBJ2_1_A_X, MY_OBJ2_2_B, MY_OBJ2_3_C);


  @Test
  public void index_empty_stream_returns_empty_map() {
    assertThat(Stream.<MyObj>empty().collect(index(MyObj::getId)).size()).isZero();
    assertThat(Stream.<MyObj>empty().collect(index(MyObj::getId, MyObj::getText)).size()).isZero();
  }

  @Test
  public void index_fails_if_key_function_is_null() {
    assertThatThrownBy(() -> index(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Key function can't be null");
  }

  @Test
  public void index_with_valueFunction_fails_if_key_function_is_null() {
    assertThatThrownBy(() -> index(null, MyObj::getText))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Key function can't be null");
  }

  @Test
  public void index_with_valueFunction_fails_if_value_function_is_null() {
    assertThatThrownBy(() ->  index(MyObj::getId, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Value function can't be null");
  }

  @Test
  public void index_fails_if_key_function_returns_null() {
    assertThatThrownBy(() -> SINGLE_ELEMENT_LIST.stream().collect(index(s -> null)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Key function can't return null");
  }

  @Test
  public void index_with_valueFunction_fails_if_key_function_returns_null() {
    assertThatThrownBy(() -> SINGLE_ELEMENT_LIST.stream().collect(index(s -> null, MyObj::getText)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Key function can't return null");
  }

  @Test
  public void index_with_valueFunction_fails_if_value_function_returns_null() {
    assertThatThrownBy(() -> SINGLE_ELEMENT_LIST.stream().collect(index(MyObj::getId, s -> null)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Value function can't return null");
  }

  @Test
  public void index_supports_duplicate_keys() {
    ListMultimap<Integer, MyObj> multimap = LIST_WITH_DUPLICATE_ID.stream().collect(index(MyObj::getId));

    assertThat(multimap.keySet()).containsOnly(1, 2);
    assertThat(multimap.get(1)).containsOnly(MY_OBJ_1_A, MY_OBJ_1_C);
    assertThat(multimap.get(2)).containsOnly(MY_OBJ_2_B);
  }

  @Test
  public void index_returns_ListMultimap() {
    ListMultimap<Integer, MyObj> multimap = LIST.stream().collect(index(MyObj::getId));

    assertThat(multimap.size()).isEqualTo(3);
    Map<Integer, Collection<MyObj>> map = multimap.asMap();
    assertThat(map.get(1)).containsOnly(MY_OBJ_1_A);
    assertThat(map.get(2)).containsOnly(MY_OBJ_2_B);
    assertThat(map.get(3)).containsOnly(MY_OBJ_3_C);
  }

  @Test
  public void index_with_valueFunction_returns_ListMultimap() {
    ListMultimap<Integer, String> multimap = LIST.stream().collect(index(MyObj::getId, MyObj::getText));

    assertThat(multimap.size()).isEqualTo(3);
    Map<Integer, Collection<String>> map = multimap.asMap();
    assertThat(map.get(1)).containsOnly("A");
    assertThat(map.get(2)).containsOnly("B");
    assertThat(map.get(3)).containsOnly("C");
  }

  @Test
  public void index_parallel_stream() {
    ListMultimap<String, String> multimap = HUGE_LIST.parallelStream().collect(index(identity()));

    assertThat(multimap.keySet()).isEqualTo(HUGE_SET);
  }

  @Test
  public void index_with_valueFunction_parallel_stream() {
    ListMultimap<String, String> multimap = HUGE_LIST.parallelStream().collect(index(identity(), identity()));

    assertThat(multimap.keySet()).isEqualTo(HUGE_SET);
  }

  @Test
  public void unorderedIndex_empty_stream_returns_empty_map() {
    assertThat(Stream.<MyObj>empty().collect(unorderedIndex(MyObj::getId)).size()).isZero();
    assertThat(Stream.<MyObj>empty().collect(unorderedIndex(MyObj::getId, MyObj::getText)).size()).isZero();
  }

  @Test
  public void unorderedIndex_fails_if_key_function_is_null() {
    assertThatThrownBy(() -> unorderedIndex(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Key function can't be null");
  }

  @Test
  public void unorderedIndex_with_valueFunction_fails_if_key_function_is_null() {
    assertThatThrownBy(() -> unorderedIndex(null, MyObj::getText))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Key function can't be null");
  }

  @Test
  public void unorderedIndex_with_valueFunction_fails_if_value_function_is_null() {
    assertThatThrownBy(() ->  unorderedIndex(MyObj::getId, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Value function can't be null");
  }

  @Test
  public void unorderedIndex_fails_if_key_function_returns_null() {
    assertThatThrownBy(() -> SINGLE_ELEMENT_LIST.stream().collect(unorderedIndex(s -> null)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Key function can't return null");
  }

  @Test
  public void unorderedIndex_with_valueFunction_fails_if_key_function_returns_null() {
    assertThatThrownBy(() -> SINGLE_ELEMENT_LIST.stream().collect(unorderedIndex(s -> null, MyObj::getText)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Key function can't return null");
  }

  @Test
  public void unorderedIndex_with_valueFunction_fails_if_value_function_returns_null() {
    assertThatThrownBy(() -> SINGLE_ELEMENT_LIST.stream().collect(unorderedIndex(MyObj::getId, s -> null)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Value function can't return null");
  }

  @Test
  public void unorderedIndex_supports_duplicate_keys() {
    SetMultimap<Integer, MyObj> multimap = LIST_WITH_DUPLICATE_ID.stream().collect(unorderedIndex(MyObj::getId));

    assertThat(multimap.keySet()).containsOnly(1, 2);
    assertThat(multimap.get(1)).containsOnly(MY_OBJ_1_A, MY_OBJ_1_C);
    assertThat(multimap.get(2)).containsOnly(MY_OBJ_2_B);
  }

  @Test
  public void unorderedIndex_returns_SetMultimap() {
    SetMultimap<Integer, MyObj> multimap = LIST.stream().collect(unorderedIndex(MyObj::getId));

    assertThat(multimap.size()).isEqualTo(3);
    Map<Integer, Collection<MyObj>> map = multimap.asMap();
    assertThat(map.get(1)).containsOnly(MY_OBJ_1_A);
    assertThat(map.get(2)).containsOnly(MY_OBJ_2_B);
    assertThat(map.get(3)).containsOnly(MY_OBJ_3_C);
  }

  @Test
  public void unorderedIndex_with_valueFunction_returns_SetMultimap() {
    SetMultimap<Integer, String> multimap = LIST.stream().collect(unorderedIndex(MyObj::getId, MyObj::getText));

    assertThat(multimap.size()).isEqualTo(3);
    Map<Integer, Collection<String>> map = multimap.asMap();
    assertThat(map.get(1)).containsOnly("A");
    assertThat(map.get(2)).containsOnly("B");
    assertThat(map.get(3)).containsOnly("C");
  }

  @Test
  public void unorderedIndex_parallel_stream() {
    SetMultimap<String, String> multimap = HUGE_LIST.parallelStream().collect(unorderedIndex(identity()));

    assertThat(multimap.keySet()).isEqualTo(HUGE_SET);
  }

  @Test
  public void unorderedIndex_with_valueFunction_parallel_stream() {
    SetMultimap<String, String> multimap = HUGE_LIST.parallelStream().collect(unorderedIndex(identity(), identity()));

    assertThat(multimap.keySet()).isEqualTo(HUGE_SET);
  }

  @Test
  public void unorderedFlattenIndex_empty_stream_returns_empty_map() {
    assertThat(Stream.<MyObj2>empty()
      .collect(unorderedFlattenIndex(MyObj2::getId, MyObj2::getTexts))
      .size()).isZero();
  }

  @Test
  public void unorderedFlattenIndex_with_valueFunction_fails_if_key_function_is_null() {
    assertThatThrownBy(() -> unorderedFlattenIndex(null, MyObj2::getTexts))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Key function can't be null");
  }

  @Test
  public void unorderedFlattenIndex_with_valueFunction_fails_if_value_function_is_null() {
    assertThatThrownBy(() -> unorderedFlattenIndex(MyObj2::getId, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Value function can't be null");
  }

  @Test
  public void unorderedFlattenIndex_with_valueFunction_fails_if_key_function_returns_null() {
    assertThatThrownBy(() -> SINGLE_ELEMENT2_LIST.stream().collect(unorderedFlattenIndex(s -> null, MyObj2::getTexts)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Key function can't return null");
  }

  @Test
  public void unorderedFlattenIndex_with_valueFunction_fails_if_value_function_returns_null() {
    assertThatThrownBy(() -> SINGLE_ELEMENT2_LIST.stream().collect(unorderedFlattenIndex(MyObj2::getId, s -> null)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Value function can't return null");
  }

  @Test
  public void unorderedFlattenIndex_supports_duplicate_keys() {
    SetMultimap<Integer, String> multimap = LIST2_WITH_DUPLICATE_ID.stream()
      .collect(unorderedFlattenIndex(MyObj2::getId, MyObj2::getTexts));

    assertThat(multimap.keySet()).containsOnly(1, 2);
    assertThat(multimap.get(1)).containsOnly("A", "X", "C");
    assertThat(multimap.get(2)).containsOnly("B");
  }

  @Test
  public void unorderedFlattenIndex_with_valueFunction_returns_SetMultimap() {
    SetMultimap<Integer, String> multimap = LIST2.stream()
      .collect(unorderedFlattenIndex(MyObj2::getId, MyObj2::getTexts));

    assertThat(multimap.size()).isEqualTo(4);
    Map<Integer, Collection<String>> map = multimap.asMap();
    assertThat(map.get(1)).containsOnly("A", "X");
    assertThat(map.get(2)).containsOnly("B");
    assertThat(map.get(3)).containsOnly("C");
  }

  @Test
  public void unorderedFlattenIndex_with_valueFunction_parallel_stream() {
    SetMultimap<String, String> multimap = HUGE_LIST.parallelStream().collect(unorderedFlattenIndex(identity(), Stream::of));

    assertThat(multimap.keySet()).isEqualTo(HUGE_SET);
  }

  private static final class MyObj {
    private final int id;
    private final String text;

    public MyObj(int id, String text) {
      this.id = id;
      this.text = text;
    }

    public int getId() {
      return id;
    }

    public String getText() {
      return text;
    }
  }

  private static final class MyObj2 {
    private final int id;
    private final List<String> texts;

    public MyObj2(int id, String... texts) {
      this.id = id;
      this.texts = Arrays.stream(texts).collect(Collectors.toList());
    }

    public int getId() {
      return id;
    }

    public Stream<String> getTexts() {
      return texts.stream();
    }
  }

  private enum MyEnum {
    ONE, TWO, THREE
  }
}
