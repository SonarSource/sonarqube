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
package org.sonar.xoo.rule;

import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.xoo.Xoo;

/**
 * Define all the coding rules that are supported on the repository named "xoo".
 */
public class XooRulesDefinition implements RulesDefinition {

  public static final String XOO_REPOSITORY = "xoo";

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(XOO_REPOSITORY, Xoo.KEY).setName("Xoo");

    // define a single rule programmatically. Note that rules
    // can be loaded from JSON or XML files too.
    NewRule x1Rule = repository.createRule("x1")
      .setName("No empty line")
      .setHtmlDescription("Generate an issue on empty lines of Xoo source files")

        // optional tags
      .setTags("style", "security")

        // optional status. Default value is READY.
      .setStatus(RuleStatus.BETA)

        // default severity when the rule is activated on a Quality profile. Default value is MAJOR.
      .setSeverity(Severity.MINOR);

    // debt-related information
    x1Rule
      .setDebtSubCharacteristic(SubCharacteristics.INTEGRATION_TESTABILITY)
      .setDebtRemediationFunction(x1Rule.debtRemediationFunctions().linearWithOffset("1h", "30min"));

    x1Rule.createParam("acceptWhitespace")
      .setDefaultValue("false")
      .setType(RuleParamType.BOOLEAN)
      .setDescription("Accept whitespaces on the line");

    // don't forget to call done() to finalize the definition
    repository.done();
  }

}
