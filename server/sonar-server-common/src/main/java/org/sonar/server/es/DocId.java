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

import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

@Immutable
class DocId {

  private final String index;
  private final String indexType;
  private final String id;

  DocId(String index, String indexType, String id) {
    this.index = requireNonNull(index, "index can't be null");
    this.indexType = requireNonNull(indexType,"type can't be null");
    this.id = requireNonNull(id, "id can't be null");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DocId docId = (DocId) o;

    return index.equals(docId.index) && indexType.equals(docId.indexType) && id.equals(docId.id);
  }

  @Override
  public int hashCode() {
    int result = index.hashCode();
    result = 31 * result + indexType.hashCode();
    result = 31 * result + id.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "DocId{" + index + '/' + indexType + '/' + id + '}';
  }
}
