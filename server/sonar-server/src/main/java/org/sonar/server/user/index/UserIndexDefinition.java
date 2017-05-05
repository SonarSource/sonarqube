/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import org.sonar.api.config.Settings;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.NewIndex;

import static org.sonar.server.es.DefaultIndexSettingsElement.SORTABLE_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.USER_SEARCH_GRAMS_ANALYZER;

/**
 * Definition of ES index "users", including settings and fields.
 */
public class UserIndexDefinition implements IndexDefinition {

  public static final IndexType INDEX_TYPE_USER = new IndexType("users", "user");
  public static final String FIELD_LOGIN = "login";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_EMAIL = "email";
  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_UPDATED_AT = "updatedAt";
  public static final String FIELD_ACTIVE = "active";
  public static final String FIELD_SCM_ACCOUNTS = "scmAccounts";
  public static final String FIELD_ORGANIZATION_UUIDS = "organizationUuids";

  private final Settings settings;

  public UserIndexDefinition(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX_TYPE_USER.getIndex());

    index.configureShards(settings, 1);

    // type "user"
    NewIndex.NewIndexType mapping = index.createType(INDEX_TYPE_USER.getType());
    mapping.stringFieldBuilder(FIELD_LOGIN).addSubFields(USER_SEARCH_GRAMS_ANALYZER).build();
    mapping.stringFieldBuilder(FIELD_NAME).addSubFields(USER_SEARCH_GRAMS_ANALYZER).build();
    mapping.stringFieldBuilder(FIELD_EMAIL).addSubFields(USER_SEARCH_GRAMS_ANALYZER, SORTABLE_ANALYZER).build();
    mapping.createDateTimeField(FIELD_CREATED_AT);
    mapping.createDateTimeField(FIELD_UPDATED_AT);
    mapping.createBooleanField(FIELD_ACTIVE);
    mapping.stringFieldBuilder(FIELD_SCM_ACCOUNTS).disableNorms().addSubFields(SORTABLE_ANALYZER).build();
    mapping.stringFieldBuilder(FIELD_ORGANIZATION_UUIDS).disableNorms().build();
  }
}
