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

import com.google.common.base.Preconditions;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.sonar.core.issue.db.IssueAuthorizationDto;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.SearchClient;

import java.util.Map;

public class IssueAuthorizationIndex extends BaseIndex<IssueAuthorizationDoc, IssueAuthorizationDto, String> {

  public IssueAuthorizationIndex(IssueAuthorizationNormalizer normalizer, SearchClient client) {
    super(IndexDefinition.ISSUES_AUTHORIZATION, normalizer, client);
  }

  @Override
  protected String getKeyValue(String s) {
    return s;
  }

  @Override
  protected void initializeIndex() {
    // being refactored
  }

  @Override
  protected Map mapProperties() {
    throw new UnsupportedOperationException("being refactored");
  }

  @Override
  protected Map mapKey() {
    throw new UnsupportedOperationException("being refactored");
  }

  @Override
  public IssueAuthorizationDoc toDoc(Map fields) {
    Preconditions.checkNotNull(fields, "Cannot construct IssueAuthorization with null response");
    return new IssueAuthorizationDoc(fields);
  }

  @Override
  protected FilterBuilder getLastSynchronizationBuilder(Map<String, String> params) {
    String projectUuid = params.get(IssueAuthorizationNormalizer.IssueAuthorizationField.PROJECT.field());
    if (projectUuid != null) {
      return FilterBuilders.boolFilter().must(FilterBuilders.termsFilter(IssueAuthorizationNormalizer.IssueAuthorizationField.PROJECT.field(), projectUuid));
    }
    return super.getLastSynchronizationBuilder(params);
  }
}
