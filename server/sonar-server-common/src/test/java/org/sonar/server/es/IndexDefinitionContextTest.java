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
package org.sonar.server.es;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.newindex.SettingsConfiguration;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.es.newindex.SettingsConfiguration.newBuilder;

@RunWith(DataProviderRunner.class)
public class IndexDefinitionContextTest {
  private SettingsConfiguration emptySettingsConfiguration = newBuilder(new MapSettings().asConfig()).build();


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

    assertThatThrownBy(() -> context.create(index2, emptySettingsConfiguration))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Index already exists: " + index1.getName());
  }

  @DataProvider
  public static Object[][] paarOfIndicesWithSameName() {
    String indexName = secure().nextAlphabetic(10).toLowerCase(Locale.ENGLISH);
    return new Object[][] {
      {Index.simple(indexName), Index.simple(indexName)},
      {Index.withRelations(indexName), Index.withRelations(indexName)},
      {Index.simple(indexName), Index.withRelations(indexName)},
      {Index.withRelations(indexName), Index.simple(indexName)},
    };
  }
}
