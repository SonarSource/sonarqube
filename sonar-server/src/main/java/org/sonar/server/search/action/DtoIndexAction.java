/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.search.action;

import org.sonar.core.persistence.Dto;

public class DtoIndexAction<E extends Dto> extends IndexAction {

  private final E item;

  public DtoIndexAction(String indexType, Method method, E item) {
    super(indexType, method);
    this.item = item;
  }

  @Override
  public void doExecute() {
    try {
      if (this.getMethod().equals(Method.DELETE)) {
        index.deleteByDto(this.item);
      } else if (this.getMethod().equals(Method.INSERT)) {
        index.insertByDto(this.item);
      } else if (this.getMethod().equals(Method.UPDATE)) {
        index.updateByDto(this.item);
      }
    } catch (Exception e) {
      throw new IllegalStateException(this.getClass().getSimpleName() +
        " cannot execute " + this.getMethod() + " for " + this.item.getClass().getSimpleName() +
        " as " + this.getIndexType() +
        " on key: "+ this.item.getKey(), e);
    }
  }

  @Override
  public String toString() {
    return "{DtoIndexItem {key: " + item.getKey() + "}";
  }
}

