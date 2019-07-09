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
import java.util.Locale;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexType;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.server.es.newindex.SettingsConfiguration.newBuilder;

public class NewAuthorizedIndexTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private String someIndexName = randomAlphabetic(10).toLowerCase(Locale.ENGLISH);
  private Index someIndex = Index.withRelations(someIndexName);
  private MapSettings settings = new MapSettings();
  private SettingsConfiguration defaultSettingsConfiguration = newBuilder(settings.asConfig()).build();

  @Test
  public void constructor_fails_with_IAE_if_index_does_not_support_relations() {
    Index simpleIndex = Index.simple(someIndexName);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Index must accept relations");

    new NewAuthorizedIndex(simpleIndex, defaultSettingsConfiguration);
  }

  @Test
  public void getMainType_returns_main_type_of_authorization_for_index_of_constructor() {
    NewAuthorizedIndex underTest = new NewAuthorizedIndex(someIndex, defaultSettingsConfiguration);

    assertThat(underTest.getMainType()).isEqualTo(IndexType.main(someIndex, "auth"));
  }

  @Test
  public void build_fails_if_no_relation_mapping_has_been_created() {
    NewAuthorizedIndex underTest = new NewAuthorizedIndex(someIndex, defaultSettingsConfiguration);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("At least one relation mapping must be defined");

    underTest.build();
  }

  @Test
  public void build_enforces_routing() {
    NewAuthorizedIndex underTest = new NewAuthorizedIndex(someIndex, defaultSettingsConfiguration);
    underTest.createTypeMapping(IndexType.relation(underTest.getMainType(), "donut"));

    BuiltIndex<NewAuthorizedIndex> builtIndex = underTest.build();

    assertThat(getAttributeAsMap(builtIndex, "_routing"))
      .containsOnly(entry("required", true));
  }

  @Test
  public void build_defines_type_field() {
    NewAuthorizedIndex underTest = new NewAuthorizedIndex(someIndex, defaultSettingsConfiguration);
    underTest.createTypeMapping(IndexType.relation(underTest.getMainType(), "donut"));

    BuiltIndex<NewAuthorizedIndex> builtIndex = underTest.build();

    Map<String, Object> properties = getProperties(builtIndex);
    assertThat(getFieldAsMap(properties, "indexType"))
      .isEqualTo(ImmutableMap.of(
        "type", "keyword",
        "norms", false,
        "store", false,
        "doc_values", false));
  }

  @Test
  public void constructor_creates_mapping_for_authorization_type() {
    NewAuthorizedIndex underTest = new NewAuthorizedIndex(someIndex, defaultSettingsConfiguration);
    underTest.createTypeMapping(IndexType.relation(underTest.getMainType(), "donut"));

    BuiltIndex<NewAuthorizedIndex> builtIndex = underTest.build();

    Map<String, Object> properties = getProperties(builtIndex);
    assertThat(getFieldAsMap(properties, "auth_groupIds"))
      .containsOnly(entry("type", "long"));
    assertThat(getFieldAsMap(properties, "auth_userIds"))
      .containsOnly(entry("type", "long"));
    assertThat(getFieldAsMap(properties, "auth_allowAnyone"))
      .containsOnly(entry("type", "boolean"));
  }

  private static Map<String, Object> getProperties(BuiltIndex<?> index) {
    return getAttributeAsMap(index, "properties");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> getAttributeAsMap(BuiltIndex<?> index, String attributeKey) {
    return (Map<String, Object>) index.getAttributes().get(attributeKey);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getFieldAsMap(Map<String, Object> properties, String fieldName) {
    return (Map<String, Object>) properties.get(fieldName);
  }
}
