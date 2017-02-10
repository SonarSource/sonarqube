/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.rule;

import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleQuery;

/**
 * @since 4.4
 */
@ServerSide
public class RuleService {

  private final RuleIndex index;

  public RuleService(RuleIndex index) {
    this.index = index;
  }

  public RuleQuery newRuleQuery() {
    return new RuleQuery();
  }

  /**
   * List all tags, including system tags, defined on rules
   */
  public Set<String> listTags() {
    /** using combined ALL_TAGS field of ES until ES update that has multiTerms aggregation */
    return index.terms(RuleIndexDefinition.FIELD_RULE_ALL_TAGS);
  }

  /**
   * List tags matching a given criterion
   */
  public Set<String> listTags(@Nullable String query, int size) {
    /** using combined ALL_TAGS field of ES until ES update that has multiTerms aggregation */
    return index.terms(RuleIndexDefinition.FIELD_RULE_ALL_TAGS, query, size);
  }

  public SearchIdResult<RuleKey> search(RuleQuery query, SearchOptions options) {
    return index.search(query, options);
  }
}
