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
package org.sonar.server.component.index;

import java.util.HashMap;
import org.sonar.server.es.BaseDoc;

public class ComponentDoc extends BaseDoc {

  public ComponentDoc() {
    super(new HashMap<>(5));
  }

  @Override
  public String getId() {
    return getField("_id");
  }

  @Override
  public String getRouting() {
    return getProjectUuid();
  }

  @Override
  public String getParent() {
    return getProjectUuid();
  }

  public ComponentDoc setId(String s) {
    setField("_id", s);
    return this;
  }

  public String getProjectUuid() {
    return getField(ComponentIndexDefinition.FIELD_PROJECT_UUID);
  }

  public ComponentDoc setProjectUuid(String s) {
    setField(ComponentIndexDefinition.FIELD_PROJECT_UUID, s);
    return this;
  }

  public String getKey() {
    return getField(ComponentIndexDefinition.FIELD_KEY);
  }

  public ComponentDoc setKey(String s) {
    setField(ComponentIndexDefinition.FIELD_KEY, s);
    return this;
  }

  public String getName() {
    return getField(ComponentIndexDefinition.FIELD_NAME);
  }

  public ComponentDoc setName(String s) {
    setField(ComponentIndexDefinition.FIELD_NAME, s);
    return this;
  }

  public String getQualifier() {
    return getField(ComponentIndexDefinition.FIELD_QUALIFIER);
  }

  public ComponentDoc setQualifier(String s) {
    setField(ComponentIndexDefinition.FIELD_QUALIFIER, s);
    return this;
  }
}
