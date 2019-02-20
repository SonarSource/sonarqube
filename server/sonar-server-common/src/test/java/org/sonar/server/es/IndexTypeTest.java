/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.server.es.IndexType.IndexMainType;
import org.sonar.server.es.IndexType.IndexRelationType;
import org.sonar.server.es.IndexType.SimpleIndexMainType;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class IndexTypeTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void parseMainType_from_main_type_without_relations() {
    IndexMainType type1 = IndexType.main(Index.simple("foo"), "bar");
    assertThat(type1.format()).isEqualTo("foo/bar");

    SimpleIndexMainType type2 = IndexType.parseMainType(type1.format());
    assertThat(type2)
      .extracting(SimpleIndexMainType::getIndex, SimpleIndexMainType::getType)
      .containsExactly("foo", "bar");
  }

  @Test
  public void parseMainType_from_maintype_with_relations() {
    IndexMainType type1 = IndexType.main(Index.withRelations("foo"), "bar");
    assertThat(type1.format()).isEqualTo("foo/bar");

    SimpleIndexMainType type2 = IndexType.parseMainType(type1.format());
    assertThat(type2)
      .extracting(SimpleIndexMainType::getIndex, SimpleIndexMainType::getType)
      .containsExactly("foo", "bar");
  }

  @Test
  public void parseMainType_from_relationtype() {
    IndexMainType mainType = IndexType.main(Index.withRelations("foo"), "bar");
    IndexRelationType type1 = IndexType.relation(mainType, "donut");
    assertThat(type1.format()).isEqualTo("foo/bar/donut");

    SimpleIndexMainType type2 = IndexType.parseMainType(type1.format());
    assertThat(type2)
      .extracting(SimpleIndexMainType::getIndex, SimpleIndexMainType::getType)
      .containsExactly("foo", "bar");
  }

  @Test
  public void parse_throws_IAE_if_invalid_format() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unsupported IndexType value: foo");

    IndexType.parseMainType("foo");
  }

  @Test
  @UseDataProvider("nullOrEmpty")
  public void main_fails_with_IAE_if_index_name_is_null_or_empty(String nullOrEmpty) {
    Index index = Index.simple("foo");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("type name can't be null nor empty");

    IndexType.main(index, nullOrEmpty);
  }

  @Test
  @UseDataProvider("nullOrEmpty")
  public void relation_fails_with_IAE_if_index_name_is_null_or_empty(String nullOrEmpty) {
    Index index = Index.withRelations("foo");
    IndexMainType mainType = IndexType.main(index, "foobar");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("type name can't be null nor empty");

    IndexType.relation(mainType, nullOrEmpty);
  }

  @DataProvider
  public static Object[][] nullOrEmpty() {
    return new Object[][] {
      {null},
      {""}
    };
  }

  @Test
  public void IndexMainType_equals_and_hashCode() {
    IndexMainType type1 = IndexType.main(Index.simple("foo"), "bar");
    IndexMainType type1b = IndexType.main(Index.simple("foo"), "bar");
    IndexMainType type1c = IndexType.main(Index.withRelations("foo"), "bar");
    IndexMainType type2 = IndexType.main(Index.simple("foo"), "baz");

    assertThat(type1)
      .isEqualTo(type1)
      .isEqualTo(type1b)
      .isNotEqualTo(type1c)
      .isNotEqualTo(type2);

    assertThat(type1.hashCode()).isEqualTo(type1.hashCode());
    assertThat(type1.hashCode()).isEqualTo(type1b.hashCode());
    assertThat(type1.hashCode()).isNotEqualTo(type1c.hashCode());
    assertThat(type2.hashCode()).isNotEqualTo(type1.hashCode());
  }

  @Test
  public void IndexRelationType_equals_and_hashCode() {
    IndexMainType mainType1 = IndexType.main(Index.withRelations("foo"), "bar");
    IndexMainType mainType2 = IndexType.main(Index.withRelations("foo"), "baz");
    IndexRelationType type1 = IndexType.relation(mainType1, "donut");
    IndexRelationType type1b = IndexType.relation(mainType1, "donut");
    IndexRelationType type2 = IndexType.relation(mainType1, "donuz");
    IndexRelationType type3 = IndexType.relation(mainType2, "donut");

    assertThat(type1)
      .isEqualTo(type1)
      .isEqualTo(type1b)
      .isNotEqualTo(type2)
      .isNotEqualTo(type3);

    assertThat(type1.hashCode()).isEqualTo(type1.hashCode());
    assertThat(type1.hashCode()).isEqualTo(type1b.hashCode());
    assertThat(type2.hashCode()).isNotEqualTo(type1.hashCode());
    assertThat(type3.hashCode())
      .isNotEqualTo(type2.hashCode())
      .isNotEqualTo(type1.hashCode());
  }
}
