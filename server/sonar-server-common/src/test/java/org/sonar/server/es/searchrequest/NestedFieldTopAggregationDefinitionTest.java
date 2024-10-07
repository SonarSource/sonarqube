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
package org.sonar.server.es.searchrequest;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(DataProviderRunner.class)
public class NestedFieldTopAggregationDefinitionTest {

  public static final Random RANDOM = new Random();

  @Test
  @UseDataProvider("notOneLevelDeepPaths")
  public void constructor_supports_nestedFieldPath_only_one_level_deep(String unsupportedPath) {
    String value = secure().nextAlphabetic(7);
    boolean sticky = RANDOM.nextBoolean();

    assertThatThrownBy(() -> new NestedFieldTopAggregationDefinition<>(unsupportedPath, value, sticky))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Field path should have only one dot: " + unsupportedPath);
  }

  @DataProvider
  public static Object[][] notOneLevelDeepPaths() {
    return new Object[][] {
      {""},
      {" "},
      {".."},
      {"a.b."},
      {"a.b.c"},
      {".b.c"},
      {"..."}
    };
  }

  @Test
  @UseDataProvider("emptyFieldNames")
  public void constructor_fails_with_IAE_if_empty_field_name(String unsupportedPath, List<String> expectedParsedFieldNames) {
    String value = secure().nextAlphabetic(7);

    assertThatThrownBy(() -> new NestedFieldTopAggregationDefinition<>(unsupportedPath, value, true))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("field path \"" + unsupportedPath + "\" should have exactly 2 non empty field names, got: " + expectedParsedFieldNames);
  }

  @DataProvider
  public static Object[][] emptyFieldNames() {
    String str1 = secure().nextAlphabetic(6);
    return new Object[][] {
      {".", emptyList()},
      {" . ", emptyList()},
      {str1 + ".", singletonList(str1)},
      {str1 + ". ", singletonList(str1)},
      {"." + str1, singletonList(str1)},
      {" . " + str1, singletonList(str1)}
    };
  }

  @Test
  public void constructor_parses_nested_field_path() {
    String fieldName = secure().nextAlphabetic(5);
    String nestedFieldName = secure().nextAlphabetic(6);
    String value = secure().nextAlphabetic(7);
    boolean sticky = RANDOM.nextBoolean();
    NestedFieldTopAggregationDefinition<String> underTest = new NestedFieldTopAggregationDefinition<>(fieldName + "." + nestedFieldName, value, sticky);

    assertThat(underTest.getFilterScope().getFieldName()).isEqualTo(fieldName);
    assertThat(underTest.getFilterScope().getNestedFieldName()).isEqualTo(nestedFieldName);
    assertThat(underTest.getFilterScope().getNestedFieldValue()).isEqualTo(value);
    assertThat(underTest.isSticky()).isEqualTo(sticky);
  }

  @Test
  public void constructor_fails_with_NPE_if_nestedFieldPath_is_null() {
    String value = secure().nextAlphabetic(7);
    boolean sticky = RANDOM.nextBoolean();

    assertThatThrownBy(() -> new NestedFieldTopAggregationDefinition<>(null, value, sticky))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("nestedFieldPath can't be null");
  }

  @Test
  public void constructor_fails_with_NPE_if_value_is_null() {
    String value = secure().nextAlphabetic(7);
    boolean sticky = RANDOM.nextBoolean();

    assertThatThrownBy(() -> new NestedFieldTopAggregationDefinition<>(value, null, sticky))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("value can't be null");
  }

  @Test
  public void getFilterScope_always_returns_the_same_instance() {
    String fieldName = secure().nextAlphabetic(5);
    String nestedFieldName = secure().nextAlphabetic(6);
    String value = secure().nextAlphabetic(7);
    boolean sticky = RANDOM.nextBoolean();
    NestedFieldTopAggregationDefinition<String> underTest = new NestedFieldTopAggregationDefinition<>(fieldName + "." + nestedFieldName, value, sticky);

    Set<TopAggregationDefinition.FilterScope> filterScopes = IntStream.range(0, 2 + RANDOM.nextInt(200))
      .mapToObj(i -> underTest.getFilterScope())
      .collect(Collectors.toSet());

    assertThat(filterScopes).hasSize(1);
  }

}
