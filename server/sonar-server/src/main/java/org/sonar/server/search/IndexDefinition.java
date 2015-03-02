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
package org.sonar.server.search;

public class IndexDefinition {

  private final String indexName;
  private final String indexType;

  private IndexDefinition(String indexName, String indexType) {
    this.indexName = indexName;
    this.indexType = indexType;
  }

  public String getIndexName() {
    return indexName;
  }

  public String getIndexType() {
    return indexType;
  }

  public static final IndexDefinition RULE = new IndexDefinition("rules", "rule");
  public static final IndexDefinition ACTIVE_RULE = new IndexDefinition("rules", "activeRule");
  public static final IndexDefinition ISSUES = new IndexDefinition("issues", "issue");

  // Only used for test
  static final IndexDefinition TEST = new IndexDefinition("test", "test");

  public static IndexDefinition createFor(String indexName, String indexType) {
    return new IndexDefinition(indexName, indexType);
  }
}
