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
package org.sonar.server.technicaldebt;

import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.technicaldebt.DefaultRequirement;
import org.sonar.core.technicaldebt.db.RequirementDao;
import org.sonar.core.technicaldebt.db.RequirementDto;
import org.sonar.server.exceptions.BadRequestException;

import javax.annotation.CheckForNull;

public class RubyTechnicalDebtService implements ServerComponent {

  private final RequirementDao requirementDao;
  private final RuleFinder rulefinder;

  public RubyTechnicalDebtService(RequirementDao requirementDao, RuleFinder rulefinder) {
    this.requirementDao = requirementDao;
    this.rulefinder = rulefinder;
  }

  @CheckForNull
  public DefaultRequirement requirement(RuleKey ruleKey) {
    Rule rule = rulefinder.findByKey(ruleKey);
    if (rule == null) {
      throw new BadRequestException("Unknown rule: " + ruleKey);
    }

    RequirementDto dto = requirementDao.selectByRuleId(rule.getId());
    return dto != null ? dto.toDefaultRequirement() : null;
  }

}
