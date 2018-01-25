/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.qualityprofile.QProfileRules;
import org.sonar.server.user.UserSession;

import static java.util.Collections.singletonList;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_DEACTIVATE_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_RULE;

public class DeactivateRuleAction implements QProfileWsAction {

  private final DbClient dbClient;
  private final QProfileRules ruleActivator;
  private final UserSession userSession;
  private final QProfileWsSupport wsSupport;

  public DeactivateRuleAction(DbClient dbClient, QProfileRules ruleActivator, UserSession userSession, QProfileWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.ruleActivator = ruleActivator;
    this.userSession = userSession;
    this.wsSupport = wsSupport;
  }

  public void define(WebService.NewController controller) {
    WebService.NewAction deactivate = controller
      .createAction(ACTION_DEACTIVATE_RULE)
      .setDescription("Deactivate a rule on a quality profile.<br> " +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Profiles'</li>" +
        "  <li>Edit right on the specified quality profile</li>" +
        "</ul>")
      .setHandler(this)
      .setPost(true)
      .setSince("4.4");

    deactivate.createParam(PARAM_KEY)
      .setDescription("Quality Profile key. Can be obtained through <code>api/qualityprofiles/search</code>")
      .setDeprecatedKey("profile_key", "6.5")
      .setRequired(true)
      .setExampleValue(UUID_EXAMPLE_01);

    deactivate.createParam(PARAM_RULE)
      .setDescription("Rule key")
      .setDeprecatedKey("rule_key", "6.5")
      .setRequired(true)
      .setExampleValue("squid:AvoidCycles");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    RuleKey ruleKey = RuleKey.parse(request.mandatoryParam(PARAM_RULE));
    String qualityProfileKey = request.mandatoryParam(PARAM_KEY);
    userSession.checkLoggedIn();
    try (DbSession dbSession = dbClient.openSession(false)) {
      RuleDefinitionDto rule = wsSupport.getRule(dbSession, ruleKey);
      QProfileDto profile = wsSupport.getProfile(dbSession, QProfileReference.fromKey(qualityProfileKey));
      OrganizationDto organization = wsSupport.getOrganization(dbSession, profile);
      wsSupport.checkCanEdit(dbSession, organization, profile);
      ruleActivator.deactivateAndCommit(dbSession, profile, singletonList(rule.getId()));
    }
    response.noContent();
  }
}
