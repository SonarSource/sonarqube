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
package org.sonar.server.es.newindex;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Locale;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexType;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.server.es.newindex.DefaultIndexSettings.NORMS;
import static org.sonar.server.es.newindex.DefaultIndexSettings.STORE;
import static org.sonar.server.es.newindex.DefaultIndexSettings.TYPE;
import static org.sonar.server.es.newindex.SettingsConfiguration.newBuilder;

@RunWith(DataProviderRunner.class)
public class NewRegularIndexTest {
  private static final String SOME_INDEX_NAME = randomAlphabetic(10).toLowerCase(Locale.ENGLISH);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MapSettings settings = new MapSettings();
  private SettingsConfiguration defaultSettingsConfiguration = newBuilder(settings.asConfig()).build();

  @Test
  @UseDataProvider("indexes")
  public void getMainType_fails_with_ISE_if_createTypeMapping_with_IndexMainType_has_not_been_called(Index index) {
    NewRegularIndex newIndex = new NewRegularIndex(index, defaultSettingsConfiguration);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Main type has not been defined");

    newIndex.getMainType();
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void createTypeMapping_with_IndexMainType_fails_with_ISE_if_called_twice(Index index) {
    NewRegularIndex underTest = new NewRegularIndex(index, defaultSettingsConfiguration);
    underTest.createTypeMapping(IndexType.main(index, "foo"));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Main type can only be defined once");

    underTest.createTypeMapping(IndexType.main(index, "foo"));
  }

  @Test
  public void createTypeMapping_with_IndexRelationType_fails_with_ISE_if_called_before_createType_with_IndexMainType() {
    Index index = Index.withRelations(SOME_INDEX_NAME);
    NewRegularIndex underTest = new NewRegularIndex(index, defaultSettingsConfiguration);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Mapping for main type must be created first");

    underTest.createTypeMapping(IndexType.relation(IndexType.main(index, "foo"), "bar"));
  }

  @Test
  public void createTypeMapping_with_IndexRelationType_fails_with_IAE_if_mainType_does_not_match_defined_one() {
    Index index = Index.withRelations(SOME_INDEX_NAME);
    IndexType.IndexMainType mainType = IndexType.main(index, "foo");
    NewRegularIndex underTest = new NewRegularIndex(index, defaultSettingsConfiguration);
    underTest.createTypeMapping(mainType);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("main type of relation must be "+ mainType);

    underTest.createTypeMapping(IndexType.relation(IndexType.main(index, "donut"), "bar"));
  }

  @Test
  @UseDataProvider("indexWithAndWithoutRelations")
  public void build_fails_with_ISE_if_no_mainType_is_defined(Index index) {
    NewRegularIndex underTest = new NewRegularIndex(index, defaultSettingsConfiguration);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Mapping for main type must be defined");

    underTest.build();
  }

  @DataProvider
  public static Object[][] indexWithAndWithoutRelations() {
    return new Object[][] {
      {Index.simple(SOME_INDEX_NAME)},
      {Index.withRelations(SOME_INDEX_NAME)}
    };
  }

  @Test
  public void build_fails_with_ISE_if_index_accepts_relations_and_none_is_defined() {
    Index index = Index.withRelations(SOME_INDEX_NAME);
    NewRegularIndex underTest = new NewRegularIndex(index, defaultSettingsConfiguration);
    underTest.createTypeMapping(IndexType.main(index, "foo"));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("At least one relation must be defined when index accepts relations");

    underTest.build();
  }

  @Test
  public void build_does_not_enforce_routing_if_mainType_does_not_accepts_relations() {
    Index someIndex = Index.simple(SOME_INDEX_NAME);
    NewRegularIndex underTest = new NewRegularIndex(someIndex, defaultSettingsConfiguration);
    underTest.createTypeMapping(IndexType.main(someIndex, "foo"));

    BuiltIndex<NewRegularIndex> builtIndex = underTest.build();

    assertThat(builtIndex.getAttributes().get("_routing"))
      .isNull();
  }

  @Test
  public void build_enforces_routing_if_mainType_accepts_relations() {
    Index someIndex = Index.withRelations(SOME_INDEX_NAME);
    NewRegularIndex underTest = new NewRegularIndex(someIndex, defaultSettingsConfiguration);
    underTest.createTypeMapping(IndexType.main(someIndex, "foo"));
    underTest.createTypeMapping(IndexType.relation(underTest.getMainType(), "bar"));

    BuiltIndex<NewRegularIndex> builtIndex = underTest.build();

    assertThat((Map<String, Object>) builtIndex.getAttributes().get("_routing"))
      .contains(entry("required", true));
  }

  @Test
  public void build_does_not_define_type_field_if_index_does_not_accept_relations() {
    Index someIndex = Index.simple(SOME_INDEX_NAME);
    NewRegularIndex underTest = new NewRegularIndex(someIndex, defaultSettingsConfiguration);
    underTest.createTypeMapping(IndexType.main(someIndex, "foo"));

    BuiltIndex<NewRegularIndex> builtIndex = underTest.build();

    Map<String, Object> properties = (Map<String, Object>) builtIndex.getAttributes().get("properties");
    assertThat(properties.get("indexType"))
      .isNull();
  }

  @Test
  public void build_defines_type_field_if_index_accepts_relations() {
    Index someIndex = Index.withRelations(SOME_INDEX_NAME);
    NewRegularIndex underTest = new NewRegularIndex(someIndex, defaultSettingsConfiguration);
    underTest.createTypeMapping(IndexType.main(someIndex, "foo"));
    underTest.createTypeMapping(IndexType.relation(underTest.getMainType(), "bar"));

    BuiltIndex<NewRegularIndex> builtIndex = underTest.build();

    Map<String, Object> properties = (Map<String, Object>) builtIndex.getAttributes().get("properties");
    assertThat((Map) properties.get("indexType"))
      .isEqualTo(ImmutableMap.of(
        TYPE, "keyword",
        NORMS, false,
        STORE, false,
        "doc_values", false));
  }

  @DataProvider
  public static Object[][] indexes() {
    String someIndexName = randomAlphabetic(10).toLowerCase(Locale.ENGLISH);
    return new Object[][] {
      {Index.simple(someIndexName)},
      {Index.withRelations(someIndexName)}
    };
  }

}
