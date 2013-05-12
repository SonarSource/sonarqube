/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.wsclient.issue;

import org.sonar.wsclient.rule.Rule;

import java.util.*;

/**
 * @since 3.6
 */
public class Issues {

  private final List<Issue> list = new ArrayList<Issue>();
  private final Map<String,Rule> rulesByKey = new HashMap<String, Rule>();
  private Paging paging;
  private Boolean securityExclusions;

  Issues add(Issue issue) {
    list.add(issue);
    return this;
  }

  public List<Issue> list() {
    return list;
  }

  public int size() {
    return list.size();
  }

  Issues add(Rule rule) {
    rulesByKey.put(rule.key(), rule);
    return this;
  }

  public Collection<Rule> rules() {
    return rulesByKey.values();
  }

  public Rule rule(Issue issue) {
    return rulesByKey.get(issue.ruleKey());
  }

  Issues setPaging(Paging paging) {
    this.paging = paging;
    return this;
  }

  public Paging paging(){
    return paging;
  }

  public Boolean securityExclusions() {
    return securityExclusions;
  }

  Issues setSecurityExclusions(Boolean securityExclusions) {
    this.securityExclusions = securityExclusions;
    return this;
  }
}
