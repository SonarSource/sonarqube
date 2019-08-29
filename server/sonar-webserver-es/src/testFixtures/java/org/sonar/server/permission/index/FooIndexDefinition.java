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
package org.sonar.server.permission.index;

import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.newindex.NewAuthorizedIndex;

import static org.sonar.server.es.newindex.SettingsConfiguration.MANUAL_REFRESH_INTERVAL;
import static org.sonar.server.es.newindex.SettingsConfiguration.newBuilder;

public class FooIndexDefinition implements IndexDefinition {

  public static final Index DESCRIPTOR = Index.withRelations("foos");
  public static final String FOO_TYPE = "foo";
  public static final IndexType.IndexMainType TYPE_AUTHORIZATION = IndexType.main(DESCRIPTOR, IndexAuthorizationConstants.TYPE_AUTHORIZATION);
  public static final IndexType.IndexRelationType TYPE_FOO = IndexType.relation(TYPE_AUTHORIZATION, FOO_TYPE);
  public static final String FIELD_NAME = "name";
  public static final String FIELD_PROJECT_UUID = "projectUuid";

  @Override
  public void define(IndexDefinitionContext context) {
    NewAuthorizedIndex newIndex = context.createWithAuthorization(
      DESCRIPTOR,
      newBuilder(new MapSettings().asConfig())
        .setRefreshInterval(MANUAL_REFRESH_INTERVAL)
        .build());

    newIndex.createTypeMapping(TYPE_FOO)
      .keywordFieldBuilder(FIELD_NAME).build()
      .keywordFieldBuilder(FIELD_PROJECT_UUID).build();
  }
}
