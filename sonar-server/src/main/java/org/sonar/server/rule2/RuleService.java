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
package org.sonar.server.rule2;

import org.sonar.api.rule.RuleKey;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.search.Hit;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @since 4.4
 */
public class RuleService {

  private RuleDao dao;
  private RuleIndex index;

  public RuleService(RuleDao dao, RuleIndex index){
    this.dao = dao;
    this.index = index;
  }

  @CheckForNull
  public Rule getByKey(RuleKey key) {
    return null;
  }

  public Collection<Hit> search(RuleQuery query){
    return Collections.emptyList();
  }

  public static Rule toRule(RuleDto ruleDto){
    return new RuleImpl();
  }

  public static Rule toRule(Hit hit){
    return new RuleImpl();
  }
}
