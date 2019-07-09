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
import java.util.Locale;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.newindex.SettingsConfiguration;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.es.newindex.SettingsConfiguration.newBuilder;

@RunWith(DataProviderRunner.class)
public class IndexDefinitionContextTest {
  private SettingsConfiguration emptySettingsConfiguration = newBuilder(new MapSettings().asConfig()).build();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_indices() {
    IndexDefinition.IndexDefinitionContext context = new IndexDefinition.IndexDefinitionContext();

    context.create(Index.withRelations("issues"), emptySettingsConfiguration);
    context.create(Index.simple("users"), emptySettingsConfiguration);
    assertThat(context.getIndices().keySet())
      .containsOnly("issues", "users");
  }

  @Test
  @UseDataProvider("paarOfIndicesWithSameName")
  public void fail_to_create_twice_index_with_given_name(Index index1, Index index2) {
    IndexDefinition.IndexDefinitionContext context = new IndexDefinition.IndexDefinitionContext();

    context.create(index1, emptySettingsConfiguration);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Index already exists: " + index1.getName());

    context.create(index2, emptySettingsConfiguration);
  }

  @DataProvider
  public static Object[][] paarOfIndicesWithSameName() {
    String indexName = randomAlphabetic(10).toLowerCase(Locale.ENGLISH);
    return new Object[][] {
      {Index.simple(indexName), Index.simple(indexName)},
      {Index.withRelations(indexName), Index.withRelations(indexName)},
      {Index.simple(indexName), Index.withRelations(indexName)},
      {Index.withRelations(indexName), Index.simple(indexName)},
    };
  }
}
