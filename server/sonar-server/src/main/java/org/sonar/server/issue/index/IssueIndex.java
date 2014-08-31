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
package org.sonar.server.issue.index;

import org.elasticsearch.common.settings.Settings;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.SearchClient;

import java.io.IOException;
import java.util.Map;

public class IssueIndex extends BaseIndex<IssueDoc, IssueDto, String> {

  protected IssueIndex(IssueNormalizer normalizer, SearchClient client) {
    super(IndexDefinition.ISSUES, normalizer, client);
  }

  @Override
  protected String getKeyValue(String s) {
    return s;
  }

  @Override
  protected Settings getIndexSettings() throws IOException {
    return null;
  }

  @Override
  protected Map mapProperties() {
    return null;
  }

  @Override
  protected Map mapKey() {
    return null;
  }

  @Override
  protected IssueDoc toDoc(Map<String, Object> fields) {
    return null;
  }
}
