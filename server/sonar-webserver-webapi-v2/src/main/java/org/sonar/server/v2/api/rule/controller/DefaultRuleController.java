/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.HashMap;
import java.util.Map;
import org.sonar.api.rule.RuleKey;
import org.sonar.server.common.rule.ReactivationException;
import org.sonar.server.common.rule.service.NewCustomRule;
import org.sonar.server.common.rule.service.RuleInformation;
import org.sonar.server.common.rule.service.RuleService;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.rule.converter.RuleRestResponseGenerator;
import org.sonar.server.v2.api.rule.enums.RuleStatusRestEnum;
import org.sonar.server.v2.api.rule.request.RuleCreateRestRequest;
import org.sonar.server.v2.api.rule.resource.Impact;
import org.sonar.server.v2.api.rule.response.RuleRestResponse;
import org.springframework.http.HttpStatus;

import static java.util.Optional.ofNullable;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;

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
    userSession
      .checkLoggedIn()
      .checkPermission(ADMINISTER_QUALITY_PROFILES);
    try {
      RuleInformation ruleInformation = ruleService.createCustomRule(toNewCustomRule(request));
      return ruleRestResponseGenerator.toRuleRestResponse(ruleInformation);
    } catch (ReactivationException e) {
      throw new ServerException(HttpStatus.CONFLICT.value(), e.getMessage());
    }
  }

  private static NewCustomRule toNewCustomRule(RuleCreateRestRequest request) {
    NewCustomRule newCustomRule = NewCustomRule.createForCustomRule(RuleKey.parse(request.key()), RuleKey.parse(request.templateKey()))
      .setName(request.name())
      .setMarkdownDescription(request.markdownDescription())
      .setStatus(ofNullable(request.status()).map(RuleStatusRestEnum::getRuleStatus).orElse(null))
      .setCleanCodeAttribute(request.cleanCodeAttribute().getCleanCodeAttribute())
      .setImpacts(request.impacts().stream().map(DefaultRuleController::toNewCustomRuleImpact).toList())
      .setPreventReactivation(true);
    if (request.parameters() != null) {
      Map<String, String> params = new HashMap<>();
      request.parameters().forEach(p -> params.put(p.key(), p.defaultValue()));
      newCustomRule.setParameters(params);
    }
    return newCustomRule;
  }

  private static NewCustomRule.Impact toNewCustomRuleImpact(Impact impact) {
    return new NewCustomRule.Impact(impact.softwareQuality().getSoftwareQuality(), impact.severity().getSeverity());
  }
}
