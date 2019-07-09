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
package org.sonar.server.user.index;

import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexType.IndexMainType;
import org.sonar.server.es.newindex.NewRegularIndex;
import org.sonar.server.es.newindex.TypeMapping;

import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SORTABLE_ANALYZER;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.USER_SEARCH_GRAMS_ANALYZER;
import static org.sonar.server.es.newindex.SettingsConfiguration.newBuilder;

/**
 * Definition of ES index "users", including settings and fields.
 */
public class UserIndexDefinition implements IndexDefinition {

  public static final Index DESCRIPTOR = Index.simple("users");
  public static final IndexMainType TYPE_USER = IndexType.main(DESCRIPTOR, "user");
  public static final String FIELD_UUID = "uuid";
  public static final String FIELD_LOGIN = "login";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_EMAIL = "email";
  public static final String FIELD_ACTIVE = "active";
  public static final String FIELD_SCM_ACCOUNTS = "scmAccounts";
  public static final String FIELD_ORGANIZATION_UUIDS = "organizationUuids";

  private final Configuration config;

  public UserIndexDefinition(Configuration config) {
    this.config = config;
  }

  public static UserIndexDefinition createForTest() {
    return new UserIndexDefinition(new MapSettings().asConfig());
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewRegularIndex index = context.create(
      DESCRIPTOR,
      newBuilder(config)
        .setDefaultNbOfShards(1)
        .build())
      // all information is retrieved from the index, not only IDs
      .setEnableSource(true);

    TypeMapping mapping = index.createTypeMapping(TYPE_USER);
    mapping.keywordFieldBuilder(FIELD_UUID).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_LOGIN).addSubFields(USER_SEARCH_GRAMS_ANALYZER).build();
    mapping.keywordFieldBuilder(FIELD_NAME).addSubFields(USER_SEARCH_GRAMS_ANALYZER).build();
    mapping.keywordFieldBuilder(FIELD_EMAIL).addSubFields(USER_SEARCH_GRAMS_ANALYZER, SORTABLE_ANALYZER).build();
    mapping.createBooleanField(FIELD_ACTIVE);
    mapping.keywordFieldBuilder(FIELD_SCM_ACCOUNTS).disableNorms().addSubFields(SORTABLE_ANALYZER).build();
    mapping.keywordFieldBuilder(FIELD_ORGANIZATION_UUIDS).disableNorms().build();
  }
}
