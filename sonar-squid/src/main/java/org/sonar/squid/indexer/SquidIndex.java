/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.squid.indexer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.sonar.squid.api.Query;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceCodeIndexer;
import org.sonar.squid.api.SourceCodeSearchEngine;

public class SquidIndex implements SourceCodeIndexer, SourceCodeSearchEngine {

  private Map<String, SourceCode> index = new TreeMap<String, SourceCode>();

  public Collection<SourceCode> search(Query... query) {
    Set<SourceCode> result = new HashSet<SourceCode>();
    for (SourceCode unit : index.values()) {
      if (isSquidUnitMatchQueries(unit, query)) {
        result.add(unit);
      }
    }
    return result;
  }

  private boolean isSquidUnitMatchQueries(SourceCode unit, Query... queries) {
    boolean match;
    for (Query query : queries) {
      match = query.match(unit);
      if (!match) {
        return false;
      }
    }
    return true;
  }

  public SourceCode search(String key) {
    return index.get(key);
  }

  public void index(SourceCode sourceCode) {
    sourceCode.setSourceCodeIndexer(this);
    index.put(sourceCode.getKey(), sourceCode);
  }
}
