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

import com.google.common.collect.Maps;

import static org.sonar.server.es.newindex.FakeIndexDefinition.INT_FIELD;
import static org.sonar.server.es.newindex.FakeIndexDefinition.TYPE_FAKE;

public class FakeDoc extends BaseDoc {
  public FakeDoc() {
    super(TYPE_FAKE, Maps.newHashMap());
  }

  @Override
  public String getId() {
    return null;
  }

  public int getInt() {
    return getField(INT_FIELD);
  }

  public FakeDoc setInt(int i) {
    setField(INT_FIELD, i);
    return this;
  }
}
