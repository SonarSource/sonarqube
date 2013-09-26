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
package org.sonar.server.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.server.technicaldebt.RuleCache;
import org.sonar.server.technicaldebt.TechnicalDebtManager;

public final class RegisterTechnicalDebtModel {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterTechnicalDebtModel.class);

  public static final String TECHNICAL_DEBT_MODEL = "TECHNICAL_DEBT";

  private final TechnicalDebtManager technicalDebtManager;
  private final RuleFinder ruleFinder;

  /**
   * @param registerRulesBeforeModels used only to be started after the creation of check templates
   */
  public RegisterTechnicalDebtModel(TechnicalDebtManager technicalDebtManager, RuleFinder ruleFinder, RegisterRules registerRulesBeforeModels) {
    this.technicalDebtManager = technicalDebtManager;
    this.ruleFinder = ruleFinder;
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler(LOGGER).start("Register Technical Debt Model");
    RuleCache ruleCache = new RuleCache(ruleFinder);
    technicalDebtManager.init(ValidationMessages.create(), ruleCache);
    profiler.stop();
  }

}
