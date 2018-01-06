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
package org.sonar.server.user.index;

import org.sonar.api.config.Configuration;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.NewIndex;

import static org.sonar.server.es.DefaultIndexSettingsElement.SORTABLE_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.USER_SEARCH_GRAMS_ANALYZER;
import static org.sonar.server.es.NewIndex.SettingsConfiguration.newBuilder;

/**
 * Definition of ES index "users", including settings and fields.
 */
public class UserIndexDefinition implements IndexDefinition {

  public static final IndexType INDEX_TYPE_USER = new IndexType("users", "user");
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

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX_TYPE_USER.getIndex(),
      newBuilder(config)
        .setDefaultNbOfShards(1)
        .build());

    // type "user"
    NewIndex.NewIndexType mapping = index.createType(INDEX_TYPE_USER.getType());
    mapping.keywordFieldBuilder(FIELD_LOGIN).addSubFields(USER_SEARCH_GRAMS_ANALYZER).build();
    mapping.keywordFieldBuilder(FIELD_NAME).addSubFields(USER_SEARCH_GRAMS_ANALYZER).build();
    mapping.keywordFieldBuilder(FIELD_EMAIL).addSubFields(USER_SEARCH_GRAMS_ANALYZER, SORTABLE_ANALYZER).build();
    mapping.createBooleanField(FIELD_ACTIVE);
    mapping.keywordFieldBuilder(FIELD_SCM_ACCOUNTS).disableNorms().addSubFields(SORTABLE_ANALYZER).build();
    mapping.keywordFieldBuilder(FIELD_ORGANIZATION_UUIDS).disableNorms().build();
  }
}
