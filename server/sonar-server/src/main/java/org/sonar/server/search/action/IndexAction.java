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

import org.elasticsearch.action.ActionRequest;
import org.sonar.db.deprecated.ClusterAction;
import org.sonar.server.search.Index;

import java.util.List;

public abstract class IndexAction<K extends ActionRequest> implements ClusterAction<List<K>> {

  public static final String MISSING_INDEX_EXCEPTION = "Cannot execute request on null index";

  protected final String indexType;
  private final boolean requiresRefresh;
  private Index index;

  protected IndexAction(String indexType) {
    this(indexType, true);
  }

  protected IndexAction(String indexType, boolean requiresRefresh) {
    this.indexType = indexType;
    this.requiresRefresh = requiresRefresh;
  }

  public abstract String getKey();

  public String getIndexType() {
    return indexType;
  }

  public IndexAction<K> setIndex(Index index) {
    this.index = index;
    return this;
  }

  @Override
  public final List<K> call() throws IllegalStateException {
    if (index == null) {
      throw new IllegalStateException(MISSING_INDEX_EXCEPTION);
    }
    return doCall(index);
  }

  public abstract List<K> doCall(Index index);

  public boolean needsRefresh() {
    return this.requiresRefresh;
  }
}
