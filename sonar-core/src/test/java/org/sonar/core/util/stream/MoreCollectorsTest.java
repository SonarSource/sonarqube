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
package org.sonar.core.util.stream;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.core.util.stream.MoreCollectors.join;
import static org.sonar.core.util.stream.MoreCollectors.toArrayList;
import static org.sonar.core.util.stream.MoreCollectors.toHashSet;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

public class MoreCollectorsTest {

  private static final List<String> HUGE_LIST = IntStream.range(0, 2_000).mapToObj(String::valueOf).collect(java.util.stream.Collectors.toList());
  private static final Set<String> HUGE_SET = new HashSet<>(HUGE_LIST);
  private static final MyObj MY_OBJ_1_A = new MyObj(1, "A");
  private static final MyObj MY_OBJ_1_C = new MyObj(1, "C");
  private static final MyObj MY_OBJ_2_B = new MyObj(2, "B");
  private static final MyObj MY_OBJ_3_C = new MyObj(3, "C");
  private static final List<MyObj> SINGLE_ELEMENT_LIST = Arrays.asList(MY_OBJ_1_A);
  private static final List<MyObj> LIST_WITH_DUPLICATE_ID = Arrays.asList(MY_OBJ_1_A, MY_OBJ_2_B, MY_OBJ_1_C);
  private static final List<MyObj> LIST = Arrays.asList(MY_OBJ_1_A, MY_OBJ_2_B, MY_OBJ_3_C);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void toList_builds_an_ImmutableList() {
    List<Integer> res = Arrays.asList(1, 2, 3, 4, 5).stream().collect(toList());
    assertThat(res).isInstanceOf(ImmutableList.class)
      .containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  public void toList_parallel_stream() {
    assertThat(HUGE_LIST.parallelStream().collect(toList())).isEqualTo(HUGE_LIST);
  }

  @Test
  public void toList_with_size_builds_an_ImmutableList() {
    List<Integer> res = Arrays.asList(1, 2, 3, 4, 5).stream().collect(toList(30));
    assertThat(res).isInstanceOf(ImmutableList.class)
      .containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  public void toList_with_size_parallel_stream() {
    assertThat(HUGE_LIST.parallelStream().collect(toList(HUGE_LIST.size()))).isEqualTo(HUGE_LIST);
  }

  @Test
  public void toSet_builds_an_ImmutableSet() {
    Set<Integer> res = Arrays.asList(1, 2, 3, 4, 5).stream().collect(toSet());
    assertThat(res).isInstanceOf(ImmutableSet.class)
      .containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  public void toSet_parallel_stream() {
    assertThat(HUGE_SET.parallelStream().collect(toSet())).isEqualTo(HUGE_SET);
  }

  @Test
  public void toSet_with_size_builds_an_ImmutableSet() {
    Set<Integer> res = Stream.of(1, 2, 3, 4, 5).collect(toSet(30));
    assertThat(res).isInstanceOf(ImmutableSet.class)
      .containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  public void toSet_with_size_parallel_stream() {
    assertThat(HUGE_SET.parallelStream().collect(toSet(HUGE_SET.size()))).isEqualTo(HUGE_SET);
  }

  @Test
  public void toEnumSet() {
    Set<MyEnum> res = Stream.of(MyEnum.ONE, MyEnum.ONE, MyEnum.TWO).collect(MoreCollectors.toEnumSet(MyEnum.class));
    assertThat(res).isInstanceOf(EnumSet.class)
      .containsExactly(MyEnum.ONE, MyEnum.TWO);
  }

  @Test
  public void toEnumSet_with_empty_stream() {
    Set<MyEnum> res = Stream.<MyEnum>empty().collect(MoreCollectors.toEnumSet(MyEnum.class));
    assertThat(res).isInstanceOf(EnumSet.class)
      .isEmpty();
  }

  @Test
  public void toArrayList_builds_an_ArrayList() {
    List<Integer> res = Arrays.asList(1, 2, 3, 4, 5).stream().collect(toArrayList());
    assertThat(res).isInstanceOf(ArrayList.class)
      .containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  public void toArrayList_parallel_stream() {
    assertThat(HUGE_LIST.parallelStream().collect(toArrayList())).isEqualTo(HUGE_LIST);
  }

  @Test
  public void toArrayList_with_size_builds_an_ArrayList() {
    List<Integer> res = Arrays.asList(1, 2, 3, 4, 5).stream().collect(toArrayList(30));
    assertThat(res).isInstanceOf(ArrayList.class)
      .containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  public void toArrayList_with_size_parallel_stream() {
    assertThat(HUGE_LIST.parallelStream().collect(toArrayList(HUGE_LIST.size()))).isEqualTo(HUGE_LIST);
  }

  @Test
  public void toHashSet_builds_an_HashSet() {
    Set<Integer> res = Arrays.asList(1, 2, 3, 4, 5).stream().collect(toHashSet());
    assertThat(res).isInstanceOf(HashSet.class)
      .containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  public void toHashSet_parallel_stream() {
    assertThat(HUGE_SET.parallelStream().collect(toHashSet())).isEqualTo(HUGE_SET);
  }

  @Test
  public void toHashSet_with_size_builds_an_ArrayList() {
    Set<Integer> res = Arrays.asList(1, 2, 3, 4, 5).stream().collect(toHashSet(30));
    assertThat(res).isInstanceOf(HashSet.class)
      .containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  public void toHashSet_with_size_parallel_stream() {
    assertThat(HUGE_SET.parallelStream().collect(toHashSet(HUGE_SET.size()))).isEqualTo(HUGE_SET);
  }

  @Test
  public void uniqueIndex_empty_stream_returns_empty_map() {
    assertThat(Collections.<MyObj>emptyList().stream().collect(uniqueIndex(MyObj::getId))).isEmpty();
    assertThat(Collections.<MyObj>emptyList().stream().collect(uniqueIndex(MyObj::getId, 6))).isEmpty();
    assertThat(Collections.<MyObj>emptyList().stream().collect(uniqueIndex(MyObj::getId, MyObj::getText))).isEmpty();
    assertThat(Collections.<MyObj>emptyList().stream().collect(uniqueIndex(MyObj::getId, MyObj::getText, 10))).isEmpty();
  }

  @Test
  public void uniqueIndex_fails_when_there_is_duplicate_keys() {
    Stream<MyObj> stream = LIST_WITH_DUPLICATE_ID.stream();

    expectedDuplicateKey1IAE();

    stream.collect(uniqueIndex(MyObj::getId));
  }

  @Test
  public void uniqueIndex_with_expected_size_fails_when_there_is_duplicate_keys() {
    Stream<MyObj> stream = LIST_WITH_DUPLICATE_ID.stream();

    expectedDuplicateKey1IAE();

    stream.collect(uniqueIndex(MyObj::getId, 1));
  }

  @Test
  public void uniqueIndex_with_valueFunction_fails_when_there_is_duplicate_keys() {
    Stream<MyObj> stream = LIST_WITH_DUPLICATE_ID.stream();

    expectedDuplicateKey1IAE();

    stream.collect(uniqueIndex(MyObj::getId, MyObj::getText));
  }

  @Test
  public void uniqueIndex_with_valueFunction_and_expected_size_fails_when_there_is_duplicate_keys() {
    Stream<MyObj> stream = LIST_WITH_DUPLICATE_ID.stream();

    expectedDuplicateKey1IAE();

    stream.collect(uniqueIndex(MyObj::getId, MyObj::getText, 10));
  }

  @Test
  public void uniqueIndex_fails_if_key_function_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Key function can't be null");

    uniqueIndex(null);
  }

  @Test
  public void uniqueIndex_with_expected_size_fails_if_key_function_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Key function can't be null");

    uniqueIndex(null, 2);
  }

  @Test
  public void uniqueIndex_with_valueFunction_fails_if_key_function_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Key function can't be null");

    uniqueIndex(null, MyObj::getText);
  }

  @Test
  public void uniqueIndex_with_valueFunction_and_expected_size_fails_if_key_function_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Key function can't be null");

    uniqueIndex(null, MyObj::getText, 9);
  }

  @Test
  public void uniqueIndex_with_valueFunction_fails_if_value_function_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Value function can't be null");

    uniqueIndex(MyObj::getId, null);
  }

  @Test
  public void uniqueIndex_with_valueFunction_and_expected_size_fails_if_value_function_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Value function can't be null");

    uniqueIndex(MyObj::getId, null, 9);
  }

  @Test
  public void uniqueIndex_fails_if_key_function_returns_null() {
    expectKeyFunctionCantReturnNullNPE();

    SINGLE_ELEMENT_LIST.stream().collect(uniqueIndex(s -> null));
  }

  @Test
  public void uniqueIndex_with_expected_size_fails_if_key_function_returns_null() {
    expectKeyFunctionCantReturnNullNPE();

    SINGLE_ELEMENT_LIST.stream().collect(uniqueIndex(s -> null, 90));
  }

  @Test
  public void uniqueIndex_with_valueFunction_fails_if_key_function_returns_null() {
    expectKeyFunctionCantReturnNullNPE();

    SINGLE_ELEMENT_LIST.stream().collect(uniqueIndex(s -> null, MyObj::getText));
  }

  @Test
  public void uniqueIndex_with_valueFunction_and_expected_size_fails_if_key_function_returns_null() {
    expectKeyFunctionCantReturnNullNPE();

    SINGLE_ELEMENT_LIST.stream().collect(uniqueIndex(s -> null, MyObj::getText, 9));
  }

  @Test
  public void uniqueIndex_with_valueFunction_fails_if_value_function_returns_null() {
    expectValueFunctionCantReturnNullNPE();

    SINGLE_ELEMENT_LIST.stream().collect(uniqueIndex(MyObj::getId, s -> null));
  }

  @Test
  public void uniqueIndex_with_valueFunction_and_expected_size_fails_if_value_function_returns_null() {
    expectValueFunctionCantReturnNullNPE();

    SINGLE_ELEMENT_LIST.stream().collect(uniqueIndex(MyObj::getId, s -> null, 9));
  }

  @Test
  public void uniqueIndex_returns_map() {
    assertThat(LIST.stream().collect(uniqueIndex(MyObj::getId))).containsOnly(entry(1, MY_OBJ_1_A), entry(2, MY_OBJ_2_B), entry(3, MY_OBJ_3_C));
  }

  @Test
  public void uniqueIndex_with_expected_size_returns_map() {
    assertThat(LIST.stream().collect(uniqueIndex(MyObj::getId, 3))).containsOnly(entry(1, MY_OBJ_1_A), entry(2, MY_OBJ_2_B), entry(3, MY_OBJ_3_C));
  }

  @Test
  public void uniqueIndex_with_valueFunction_returns_map() {
    assertThat(LIST.stream().collect(uniqueIndex(MyObj::getId, MyObj::getText))).containsOnly(entry(1, "A"), entry(2, "B"), entry(3, "C"));
  }

  @Test
  public void uniqueIndex_with_valueFunction_and_expected_size_returns_map() {
    assertThat(LIST.stream().collect(uniqueIndex(MyObj::getId, MyObj::getText, 9))).containsOnly(entry(1, "A"), entry(2, "B"), entry(3, "C"));
  }

  @Test
  public void uniqueIndex_parallel_stream() {
    Map<String, String> map = HUGE_LIST.parallelStream().collect(uniqueIndex(identity()));
    assertThat(map.keySet()).isEqualTo(HUGE_SET);
    assertThat(map.values()).containsExactlyElementsOf(HUGE_SET);
  }

  @Test
  public void uniqueIndex_with_expected_size_parallel_stream() {
    Map<String, String> map = HUGE_LIST.parallelStream().collect(uniqueIndex(identity(), HUGE_LIST.size()));
    assertThat(map.keySet()).isEqualTo(HUGE_SET);
    assertThat(map.values()).containsExactlyElementsOf(HUGE_SET);
  }

  @Test
  public void uniqueIndex_with_valueFunction_parallel_stream() {
    Map<String, String> map = HUGE_LIST.parallelStream().collect(uniqueIndex(identity(), identity()));
    assertThat(map.keySet()).isEqualTo(HUGE_SET);
    assertThat(map.values()).containsExactlyElementsOf(HUGE_SET);
  }

  @Test
  public void uniqueIndex_with_valueFunction_and_expected_size_parallel_stream() {
    Map<String, String> map = HUGE_LIST.parallelStream().collect(uniqueIndex(identity(), identity(), HUGE_LIST.size()));
    assertThat(map.keySet()).isEqualTo(HUGE_SET);
    assertThat(map.values()).containsExactlyElementsOf(HUGE_SET);
  }

  @Test
  public void index_empty_stream_returns_empty_map() {
    assertThat(Collections.<MyObj>emptyList().stream().collect(index(MyObj::getId)).size()).isEqualTo(0);
    assertThat(Collections.<MyObj>emptyList().stream().collect(index(MyObj::getId, MyObj::getText)).size()).isEqualTo(0);
  }

  @Test
  public void index_fails_if_key_function_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Key function can't be null");

    index(null);
  }

  @Test
  public void index_with_valueFunction_fails_if_key_function_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Key function can't be null");

    index(null, MyObj::getText);
  }

  @Test
  public void index_with_valueFunction_fails_if_value_function_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Value function can't be null");

    index(MyObj::getId, null);
  }

  @Test
  public void index_fails_if_key_function_returns_null() {
    expectKeyFunctionCantReturnNullNPE();

    SINGLE_ELEMENT_LIST.stream().collect(index(s -> null));
  }

  @Test
  public void index_with_valueFunction_fails_if_key_function_returns_null() {
    expectKeyFunctionCantReturnNullNPE();

    SINGLE_ELEMENT_LIST.stream().collect(index(s -> null, MyObj::getText));
  }

  @Test
  public void index_with_valueFunction_fails_if_value_function_returns_null() {
    expectValueFunctionCantReturnNullNPE();

    SINGLE_ELEMENT_LIST.stream().collect(index(MyObj::getId, s -> null));
  }

  @Test
  public void index_supports_duplicate_keys() {
    Multimap<Integer, MyObj> multimap = LIST_WITH_DUPLICATE_ID.stream().collect(index(MyObj::getId));

    assertThat(multimap.keySet()).containsOnly(1, 2);
    assertThat(multimap.get(1)).containsOnly(MY_OBJ_1_A, MY_OBJ_1_C);
    assertThat(multimap.get(2)).containsOnly(MY_OBJ_2_B);
  }

  @Test
  public void uniqueIndex_supports_duplicate_keys() {
    Multimap<Integer, String> multimap = LIST_WITH_DUPLICATE_ID.stream().collect(index(MyObj::getId, MyObj::getText));

    assertThat(multimap.keySet()).containsOnly(1, 2);
    assertThat(multimap.get(1)).containsOnly("A", "C");
    assertThat(multimap.get(2)).containsOnly("B");
  }

  @Test
  public void index_returns_multimap() {
    Multimap<Integer, MyObj> multimap = LIST.stream().collect(index(MyObj::getId));

    assertThat(multimap.size()).isEqualTo(3);
    Map<Integer, Collection<MyObj>> map = multimap.asMap();
    assertThat(map.get(1)).containsOnly(MY_OBJ_1_A);
    assertThat(map.get(2)).containsOnly(MY_OBJ_2_B);
    assertThat(map.get(3)).containsOnly(MY_OBJ_3_C);
  }

  @Test
  public void index_with_valueFunction_returns_multimap() {
    Multimap<Integer, String> multimap = LIST.stream().collect(index(MyObj::getId, MyObj::getText));

    assertThat(multimap.size()).isEqualTo(3);
    Map<Integer, Collection<String>> map = multimap.asMap();
    assertThat(map.get(1)).containsOnly("A");
    assertThat(map.get(2)).containsOnly("B");
    assertThat(map.get(3)).containsOnly("C");
  }

  @Test
  public void index_parallel_stream() {
    Multimap<String, String> multimap = HUGE_LIST.parallelStream().collect(index(identity()));

    assertThat(multimap.keySet()).isEqualTo(HUGE_SET);
  }

  @Test
  public void index_with_valueFunction_parallel_stream() {
    Multimap<String, String> multimap = HUGE_LIST.parallelStream().collect(index(identity(), identity()));

    assertThat(multimap.keySet()).isEqualTo(HUGE_SET);
  }

  @Test
  public void join_on_empty_stream_returns_empty_string() {
    assertThat(Collections.emptyList().stream().collect(join(Joiner.on(",")))).isEmpty();
  }

  @Test
  public void join_fails_with_NPE_if_joiner_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Joiner can't be null");

    join(null);
  }

  @Test
  public void join_applies_joiner_to_stream() {
    assertThat(Arrays.asList("1", "2", "3", "4").stream().collect(join(Joiner.on(","))))
      .isEqualTo("1,2,3,4");
  }

  @Test
  public void join_does_not_support_parallel_stream_and_fails_with_ISE() {
    Stream<String> hugeStream = HUGE_LIST.parallelStream();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Parallel processing is not supported");

    hugeStream.collect(join(Joiner.on(" ")));
  }

  @Test
  public void join_supports_null_if_joiner_does() {
    Stream<String> stream = Arrays.asList("1", null).stream();

    expectedException.expect(NullPointerException.class);

    stream.collect(join(Joiner.on(",")));
  }

  private void expectedDuplicateKey1IAE() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Duplicate key 1");
  }

  private void expectKeyFunctionCantReturnNullNPE() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Key function can't return null");
  }

  private void expectValueFunctionCantReturnNullNPE() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Value function can't return null");
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

  private enum MyEnum {
    ONE, TWO, THREE
  }
}
