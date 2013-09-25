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

import org.sonar.api.qualitymodel.Model;
import org.sonar.api.qualitymodel.ModelDefinition;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.ValidationMessages;

public final class TechnicalDebtModelDefinition extends ModelDefinition {

  public static final String TECHNICAL_DEBT_MODEL = "TECHNICAL_DEBT";

  private final TechnicalDebtManager technicalDebtManager;
  private final RuleFinder ruleFinder;

  public TechnicalDebtModelDefinition(TechnicalDebtManager technicalDebtManager, RuleFinder ruleFinder) {
    super(TECHNICAL_DEBT_MODEL);
    this.technicalDebtManager = technicalDebtManager;
    this.ruleFinder = ruleFinder;
  }

  @Override
  public Model createModel() {
    RuleCache ruleCache = new RuleCache(ruleFinder);
    return technicalDebtManager.createInitialModel(ValidationMessages.create(), ruleCache);
  }

}
