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
package org.sonar.server.es;

import javax.annotation.concurrent.Immutable;

@Immutable
class DocId {

  private final String index;
  private final String indexType;
  private final String id;

  DocId(IndexType indexType, String id) {
    this(indexType.getIndex(), indexType.getType(), id);
  }

  DocId(String index, String indexType, String id) {
    this.index = index;
    this.indexType = indexType;
    this.id = id;
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

    if (!index.equals(docId.index)) {
      return false;
    }
    if (!indexType.equals(docId.indexType)) {
      return false;
    }
    return id.equals(docId.id);
  }

  @Override
  public int hashCode() {
    int result = index.hashCode();
    result = 31 * result + indexType.hashCode();
    result = 31 * result + id.hashCode();
    return result;
  }
}
