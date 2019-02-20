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

import org.sonar.server.es.Index;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexType.IndexRelationType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.FIELD_ALLOW_ANYONE;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.FIELD_GROUP_IDS;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.FIELD_USER_IDS;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.TYPE_AUTHORIZATION;

public class NewAuthorizedIndex extends NewIndex<NewAuthorizedIndex> {
  private final IndexType.IndexMainType mainType;

  public NewAuthorizedIndex(Index index, SettingsConfiguration settingsConfiguration) {
    super(index, settingsConfiguration);
    checkArgument(index.acceptsRelations(), "Index must accept relations");

    this.mainType = IndexType.main(index, TYPE_AUTHORIZATION);
    super.createTypeMapping(mainType)
      .createLongField(FIELD_GROUP_IDS)
      .createLongField(FIELD_USER_IDS)
      .createBooleanField(FIELD_ALLOW_ANYONE);
  }

  @Override
  public IndexType.IndexMainType getMainType() {
    return mainType;
  }

  @Override
  public TypeMapping createTypeMapping(IndexRelationType relationType) {
    checkArgument(relationType.getMainType().equals(mainType), "mainType of relation must be %s", mainType);
    return super.createTypeMapping(relationType);
  }

  @Override
  public BuiltIndex<NewAuthorizedIndex> build() {
    checkState(!getRelations().isEmpty(), "At least one relation mapping must be defined");
    return new BuiltIndex<>(this);
  }

}
