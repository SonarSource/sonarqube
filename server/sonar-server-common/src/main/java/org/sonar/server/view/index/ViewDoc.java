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
package org.sonar.server.view.index;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import org.sonar.server.es.BaseDoc;

import static org.sonar.server.view.index.ViewIndexDefinition.TYPE_VIEW;

public class ViewDoc extends BaseDoc {

  public ViewDoc(Map<String, Object> fields) {
    super(TYPE_VIEW, fields);
  }

  public ViewDoc() {
    super(TYPE_VIEW, Maps.newHashMap());
  }

  @Override
  public String getId() {
    return uuid();
  }

  public String uuid() {
    return getField(ViewIndexDefinition.FIELD_UUID);
  }

  public List<String> projects() {
    return getField(ViewIndexDefinition.FIELD_PROJECTS);
  }

  public ViewDoc setUuid(String s) {
    setField(ViewIndexDefinition.FIELD_UUID, s);
    return this;
  }

  public ViewDoc setProjects(List<String> s) {
    setField(ViewIndexDefinition.FIELD_PROJECTS, s);
    return this;
  }

}
