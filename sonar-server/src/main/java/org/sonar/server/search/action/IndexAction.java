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

import org.sonar.core.cluster.QueueAction;
import org.sonar.server.search.Index;

public abstract class IndexAction extends QueueAction {


  public abstract Class<?> getPayloadClass();

  public enum Method {
    UPSERT, DELETE
  }

  protected String indexType;
  protected Method method;
  protected Index index;

  public IndexAction(String indexType) {
    this(indexType, null);
  }

  public IndexAction(String indexType, Method method) {
    super();
    this.indexType = indexType;
    this.method = method;
  }

  public abstract String getKey();

  public Method getMethod() {
    return this.method;
  }

  public String getIndexType() {
    return indexType;
  }

  public void setMethod(Method method) {
    this.method = method;
  }

  public void setIndex(Index index) {
    this.index = index;
  }

  public Index getIndex() {
    return index;
  }
}
