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

import org.sonar.server.es.IndexType;

public final class TestNewIndex extends NewIndex<TestNewIndex> {
  private final IndexType.IndexMainType mainType;
  private final TypeMapping mainTypeMapping;

  public TestNewIndex(IndexType.IndexMainType mainType, SettingsConfiguration settingsConfiguration) {
    super(mainType.getIndex(), settingsConfiguration);
    this.mainType = mainType;
    mainTypeMapping = super.createTypeMapping(mainType);
  }

  @Override
  public IndexType.IndexMainType getMainType() {
    return mainType;
  }

  public TypeMapping getMainTypeMapping() {
    return mainTypeMapping;
  }

  @Override
  public BuiltIndex<TestNewIndex> build() {
    return new BuiltIndex<>(this);
  }

  public TestNewIndex addRelation(String name) {
    super.createTypeMapping(IndexType.relation(mainType, name));
    return this;
  }

  public TypeMapping createRelationMapping(String name) {
    return super.createTypeMapping(IndexType.relation(mainType, name));
  }
}
