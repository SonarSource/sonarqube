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

import com.google.common.annotations.VisibleForTesting;

public class IndexDefinition {

  private final String indexName;
  private final String indexType;

  private static final String MANAGEMENT_INDEX = "sonarindex";
  private static final String MANAGEMENT_TYPE = "index";

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

  public String getManagementIndex() {
    return MANAGEMENT_INDEX;
  }

  public String getManagementType() {
    return MANAGEMENT_TYPE;
  }

  public static final IndexDefinition RULE = new IndexDefinition("rules", "rules");
  public static final IndexDefinition ACTIVE_RULE = new IndexDefinition("rules", "activeRules");
  public static final IndexDefinition ISSUES = new IndexDefinition("issues", "issues");
  public static final IndexDefinition LOG = new IndexDefinition("logs", "sonarLogs");


  @VisibleForTesting
  protected static IndexDefinition TEST = new IndexDefinition("test", "test");
}
