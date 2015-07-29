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
package org.sonar.batch.rule;

import org.sonar.batch.protocol.input.RulesSearchResult;
import org.sonar.batch.bootstrap.WSLoader;

public class DefaultRulesLoader implements RulesLoader {
  private static final String RULES_SEARCH_URL = "/api/rules/search?ps=500&f=repo,name,internalKey";

  private final WSLoader wsLoader;

  public DefaultRulesLoader(WSLoader wsLoader) {
    this.wsLoader = wsLoader;
  }

  @Override
  public RulesSearchResult load() {

    RulesSearchResult rules = RulesSearchResult.fromJson(wsLoader.loadString(getUrl(1)));

    for (int i = 2; i < 100; i++) {
      RulesSearchResult moreRules = RulesSearchResult.fromJson(wsLoader.loadString(getUrl(i)));
      if (moreRules.getRules().isEmpty()) {
        break;
      }
      rules.getRules().addAll(moreRules.getRules());
    }
    return rules;
  }

  private static String getUrl(int page) {
    return RULES_SEARCH_URL + "&p=" + page;
  }
}
