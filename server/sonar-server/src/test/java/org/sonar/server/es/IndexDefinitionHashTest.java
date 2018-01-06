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
package org.sonar.server.es;

import org.junit.Test;

import java.util.Arrays;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.es.NewIndex.SettingsConfiguration.newBuilder;

public class IndexDefinitionHashTest {

  @Test
  public void of() {
    IndexDefinitions.Index indexV1 = new IndexDefinitions.Index(createIndex());
    String hashV1 = IndexDefinitionHash.of(indexV1);
    assertThat(hashV1).isNotEmpty();
    // always the same
    assertThat(hashV1).isEqualTo(IndexDefinitionHash.of(indexV1));

    NewIndex newIndexV2 = createIndex();
    newIndexV2.getTypes().get("fake").createIntegerField("max");
    String hashV2 = IndexDefinitionHash.of(new IndexDefinitions.Index(newIndexV2));
    assertThat(hashV2).isNotEmpty().isNotEqualTo(hashV1);
  }

  private NewIndex createIndex() {
    NewIndex newIndex = new NewIndex("fakes", newBuilder(new MapSettings().asConfig()).build());
    NewIndex.NewIndexType mapping = newIndex.createType("fake");
    mapping.setAttribute("list_attr", Arrays.asList("foo", "bar"));
    mapping.keywordFieldBuilder("key").build();
    mapping.createDateTimeField("updatedAt");
    return newIndex;
  }

}
