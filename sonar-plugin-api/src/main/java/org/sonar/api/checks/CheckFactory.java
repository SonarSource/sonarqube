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
package org.sonar.api.checks;

import com.google.common.collect.Maps;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;

import java.util.Collection;
import java.util.Map;

/**
 * @since 2.3
 */
public abstract class CheckFactory<C> {
  
  private Map<ActiveRule, C> checkByActiveRule = Maps.newIdentityHashMap();
  private Map<C, ActiveRule> activeRuleByCheck = Maps.newIdentityHashMap();
  private RulesProfile profile;
  private String repositoryKey;

  protected CheckFactory(RulesProfile profile, String repositoryKey) {
    this.repositoryKey = repositoryKey;
    this.profile = profile;
  }

  protected void init() {
    checkByActiveRule.clear();
    activeRuleByCheck.clear();
    for (ActiveRule activeRule : profile.getActiveRulesByRepository(repositoryKey)) {
      C check = createCheck(activeRule);
      checkByActiveRule.put(activeRule, check);
      activeRuleByCheck.put(check, activeRule);
    }
  }

  abstract C createCheck(ActiveRule activeRule);

  public final String getRepositoryKey() {
    return repositoryKey;
  }

  public final Collection<C> getChecks() {
    return checkByActiveRule.values();
  }

  public final C getCheck(ActiveRule activeRule) {
    return checkByActiveRule.get(activeRule);
  }

  public final ActiveRule getActiveRule(C check) {
    return activeRuleByCheck.get(check);
  }
}
