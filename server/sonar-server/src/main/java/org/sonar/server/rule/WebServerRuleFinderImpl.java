/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import javax.annotation.CheckForNull;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.db.DbClient;
import org.sonar.server.organization.DefaultOrganizationProvider;

public class WebServerRuleFinderImpl implements WebServerRuleFinder {
  private final DbClient dbClient;
  private final RuleFinder defaultFinder;
  @VisibleForTesting
  RuleFinder delegate;

  public WebServerRuleFinderImpl(DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.defaultFinder = new DefaultRuleFinder(dbClient, defaultOrganizationProvider);
    this.delegate = this.defaultFinder;
  }

  @Override
  public void startCaching() {
    this.delegate = new CachingRuleFinder(dbClient);
  }

  @Override
  public void stopCaching() {
    this.delegate = this.defaultFinder;
  }

  @Override
  @CheckForNull
  @Deprecated
  public Rule findById(int ruleId) {
    return delegate.findById(ruleId);
  }

  @Override
  @CheckForNull
  public Rule findByKey(String repositoryKey, String key) {
    return delegate.findByKey(repositoryKey, key);
  }

  @Override
  @CheckForNull
  public Rule findByKey(RuleKey key) {
    return delegate.findByKey(key);
  }

  @Override
  @CheckForNull
  public Rule find(RuleQuery query) {
    return delegate.find(query);
  }

  @Override
  public Collection<Rule> findAll(RuleQuery query) {
    return delegate.findAll(query);
  }

}
