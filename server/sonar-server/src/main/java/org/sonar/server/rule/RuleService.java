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
package org.sonar.server.rule;

import org.sonar.api.ServerSide;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @since 4.4
 */
@ServerSide
public class RuleService {

  private final RuleIndex index;
  private final RuleUpdater ruleUpdater;
  private final RuleCreator ruleCreator;
  private final RuleDeleter ruleDeleter;

  public RuleService(RuleIndex index, RuleUpdater ruleUpdater, RuleCreator ruleCreator, RuleDeleter ruleDeleter) {
    this.index = index;
    this.ruleUpdater = ruleUpdater;
    this.ruleCreator = ruleCreator;
    this.ruleDeleter = ruleDeleter;
  }

  @CheckForNull
  public Rule getByKey(RuleKey key) {
    return index.getNullableByKey(key);
  }

  public List<Rule> getByKeys(Collection<RuleKey> keys) {
    return index.getByKeys(keys);
  }

  public Rule getNonNullByKey(RuleKey key) {
    Rule rule = index.getNullableByKey(key);
    if (rule == null) {
      throw new NotFoundException("Rule not found: " + key);
    }
    return rule;
  }

  public RuleQuery newRuleQuery() {
    return new RuleQuery();
  }

  public Result<Rule> search(RuleQuery query, QueryContext options) {
    return index.search(query, options);
  }

  /**
   * List all tags, including system tags, defined on rules
   */
  public Set<String> listTags() {
    /** using combined ALL_TAGS field of ES until ES update that has multiTerms aggregation */
    return index.terms(RuleNormalizer.RuleField.ALL_TAGS.field());
  }

  /**
   * List tags matching a given criterion
   */
  public Set<String> listTags(@Nullable String query, int size) {
    /** using combined ALL_TAGS field of ES until ES update that has multiTerms aggregation */
    return index.terms(RuleNormalizer.RuleField.ALL_TAGS.field(), query, size);
  }

  public void update(RuleUpdate update) {
    checkPermission();
    ruleUpdater.update(update, UserSession.get());
  }

  public RuleKey create(NewRule newRule) {
    checkPermission();
    return ruleCreator.create(newRule);
  }

  public void delete(RuleKey ruleKey) {
    checkPermission();
    ruleDeleter.delete(ruleKey);
  }

  private void checkPermission() {
    UserSession userSession = UserSession.get();
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }
}
