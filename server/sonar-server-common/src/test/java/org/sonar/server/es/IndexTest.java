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
package org.sonar.server.es;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(DataProviderRunner.class)
public class IndexTest {
  @Test
  @UseDataProvider("nullOrEmpty")
  public void simple_index_constructor_fails_with_IAE_if_index_name_is_null_or_empty(String nullOrEmpty) {
    assertThatThrownBy(() -> Index.simple(nullOrEmpty))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Index name can't be null nor empty");
  }

  @Test
  @UseDataProvider("nullOrEmpty")
  public void withRelations_index_constructor_fails_with_IAE_if_index_name_is_null_or_empty(String nullOrEmpty) {
    assertThatThrownBy(() -> Index.withRelations(nullOrEmpty))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Index name can't be null nor empty");
  }

  @DataProvider
  public static Object[][] nullOrEmpty() {
    return new Object[][] {
      {null},
      {""}
    };
  }

  @Test
  public void simple_index_name_must_not_contain_upper_case_char() {
    assertThatThrownBy(() -> Index.simple("Issues"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Index name must be lower-case letters or '_all': Issues");
  }

  @Test
  public void withRelations_index_name_must_not_contain_upper_case_char() {
    assertThatThrownBy(() -> Index.withRelations("Issues"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Index name must be lower-case letters or '_all': Issues");
  }

  @Test
  public void simple_index_name_can_not_contain_underscore_except__all_keyword() {
    // doesn't fail
    Index.simple("_all");

    assertThatThrownBy(() -> Index.simple("_"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Index name must be lower-case letters or '_all': _");
  }

  @Test
  public void withRelations_index_name_can_not_contain_underscore_except__all_keyword() {
    // doesn't fail
    Index.withRelations("_all");

    assertThatThrownBy(() -> Index.withRelations("_"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Index name must be lower-case letters or '_all': _");
  }

  @Test
  public void simple_index_does_not_accept_relations() {
    Index underTest = Index.simple("foo");

    assertThat(underTest.acceptsRelations()).isFalse();
  }

  @Test
  public void withRelations_index_does_not_accept_relations() {
    Index underTest = Index.withRelations("foo");

    assertThat(underTest.acceptsRelations()).isTrue();
  }

  @Test
  public void getName_returns_constructor_parameter() {
    String indexName = secure().nextAlphabetic(10).toLowerCase(Locale.ENGLISH);

    assertThat(Index.simple(indexName).getName()).isEqualTo(indexName);
    assertThat(Index.withRelations(indexName).getName()).isEqualTo(indexName);
  }

  @Test
  public void getJoinField_throws_ISE_on_simple_index() {
    Index underTest = Index.simple("foo");

    assertThatThrownBy(underTest::getJoinField)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Only index accepting relations has a join field");
  }

  @Test
  public void getJoinField_returns_name_based_on_index_name() {
    String indexName = secure().nextAlphabetic(10).toLowerCase(Locale.ENGLISH);
    Index underTest = Index.withRelations(indexName);

    assertThat(underTest.getJoinField()).isEqualTo("join_" + indexName);
  }

  @Test
  public void equals_is_based_on_name_and_acceptRelations_flag() {
    assertThat(Index.simple("foo"))
      .isEqualTo(Index.simple("foo"))
      .isNotEqualTo(Index.simple("bar"))
      .isNotEqualTo(Index.withRelations("foo"));
  }

  @Test
  public void hashcode_is_based_on_name_and_acceptRelations_flag() {
    assertThat(Index.simple("foo").hashCode())
      .isEqualTo(Index.simple("foo").hashCode())
      .isNotEqualTo(Index.simple("bar").hashCode())
      .isNotEqualTo(Index.withRelations("foo").hashCode());
  }

}
