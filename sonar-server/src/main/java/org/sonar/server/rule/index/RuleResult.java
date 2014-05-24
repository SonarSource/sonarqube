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
package org.sonar.server.rule.index;

import org.elasticsearch.action.search.SearchResponse;
import org.sonar.server.rule.Rule;
import org.sonar.server.search.Result;

import java.util.Collection;
import java.util.Map;

public class RuleResult extends Result<Rule> {

  public RuleResult(SearchResponse response) {
    super(response);
  }

  @Override
  protected Rule getSearchResult(Map<String, Object> fields) {
    return new RuleDoc(fields);
  }

  public Collection<Rule> getRules() {
    return super.getHits();
  }
}
