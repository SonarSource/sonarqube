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
import org.sonar.server.es.IndexType.IndexMainType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public class NewRegularIndex extends NewIndex<NewRegularIndex> {
  private IndexMainType mainType;

  public NewRegularIndex(Index index, SettingsConfiguration settingsConfiguration) {
    super(index, settingsConfiguration);
  }

  @Override
  public IndexMainType getMainType() {
    checkState(mainType != null, "Main type has not been defined");
    return mainType;
  }

  @Override
  public TypeMapping createTypeMapping(IndexMainType mainType) {
    checkState(this.mainType == null, "Main type can only be defined once");
    this.mainType = mainType;
    return super.createTypeMapping(mainType);
  }

  @Override
  public TypeMapping createTypeMapping(IndexType.IndexRelationType relationType) {
    checkState(mainType != null, "Mapping for main type must be created first");
    checkArgument(relationType.getMainType().equals(mainType), "main type of relation must be %s", mainType);
    return super.createTypeMapping(relationType);
  }

  @Override
  public BuiltIndex<NewRegularIndex> build() {
    checkState(mainType != null, "Mapping for main type must be defined");
    checkState(!mainType.getIndex().acceptsRelations() || !getRelations().isEmpty(), "At least one relation must be defined when index accepts relations");
    return new BuiltIndex<>(this);
  }
}
