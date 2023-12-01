/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.v2.api.rule.controller;

import org.sonar.server.common.rule.service.RuleInformation;
import org.sonar.server.common.rule.service.RuleService;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.rule.converter.RuleRestResponseGenerator;
import org.sonar.server.v2.api.rule.request.RuleCreateRestRequest;
import org.sonar.server.v2.api.rule.response.RuleRestResponse;

public class DefaultRuleController implements RuleController {
  private final UserSession userSession;
  private final RuleService ruleService;
  private final RuleRestResponseGenerator ruleRestResponseGenerator;

  public DefaultRuleController(UserSession userSession, RuleService ruleService, RuleRestResponseGenerator ruleRestResponseGenerator) {
    this.userSession = userSession;

    this.ruleService = ruleService;
    this.ruleRestResponseGenerator = ruleRestResponseGenerator;
  }

  @Override
  public RuleRestResponse create(RuleCreateRestRequest request) {


    RuleInformation ruleInformation = ruleService.create(null);
    return ruleRestResponseGenerator.toRuleRestResponse(ruleInformation);
  }
}
