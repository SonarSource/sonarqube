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

import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.qualityprofile.BulkChangeResult;
import org.sonar.server.qualityprofile.QProfileRules;
import org.sonar.server.rule.ws.RuleQueryFactory;
import org.sonar.server.user.UserSession;

import static org.sonar.core.util.Uuids.UUID_EXAMPLE_03;
import static org.sonar.server.qualityprofile.ws.BulkChangeWsResponse.writeResponse;
import static org.sonar.server.qualityprofile.ws.QProfileReference.fromKey;
import static org.sonar.server.rule.ws.SearchAction.defineRuleSearchParameters;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ACTIVATE_RULES;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_TARGET_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_TARGET_SEVERITY;

public class ActivateRulesAction implements QProfileWsAction {

  private final RuleQueryFactory ruleQueryFactory;
  private final UserSession userSession;
  private final QProfileRules qProfileRules;
  private final DbClient dbClient;
  private final QProfileWsSupport wsSupport;

  public ActivateRulesAction(RuleQueryFactory ruleQueryFactory, UserSession userSession, QProfileRules qProfileRules, QProfileWsSupport wsSupport, DbClient dbClient) {
    this.ruleQueryFactory = ruleQueryFactory;
    this.userSession = userSession;
    this.qProfileRules = qProfileRules;
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
  }

  public void define(WebService.NewController controller) {
    WebService.NewAction activate = controller
      .createAction(ACTION_ACTIVATE_RULES)
      .setDescription("Bulk-activate rules on one quality profile.<br> " +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Profiles'</li>" +
        "  <li>Edit right on the specified quality profile</li>" +
        "</ul>")
      .setPost(true)
      .setSince("4.4")
      .setHandler(this);

    defineRuleSearchParameters(activate);

    activate.createParam(PARAM_TARGET_KEY)
      .setDescription("Quality Profile key on which the rule activation is done. To retrieve a quality profile key please see <code>api/qualityprofiles/search</code>")
      .setDeprecatedKey("profile_key", "6.5")
      .setRequired(true)
      .setExampleValue(UUID_EXAMPLE_03);

    activate.createParam(PARAM_TARGET_SEVERITY)
      .setDescription("Severity to set on the activated rules")
      .setDeprecatedKey("activation_severity", "6.5")
      .setPossibleValues(Severity.ALL);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String qualityProfileKey = request.mandatoryParam(PARAM_TARGET_KEY);
    userSession.checkLoggedIn();
    BulkChangeResult result;
    try (DbSession dbSession = dbClient.openSession(false)) {
      QProfileDto profile = wsSupport.getProfile(dbSession, fromKey(qualityProfileKey));
      OrganizationDto organization = wsSupport.getOrganization(dbSession, profile);
      wsSupport.checkCanEdit(dbSession, organization, profile);
      wsSupport.checkNotBuiltInt(profile);
      result = qProfileRules.bulkActivateAndCommit(dbSession, profile, ruleQueryFactory.createRuleQuery(dbSession, request), request.param(PARAM_TARGET_SEVERITY));
    }

    writeResponse(result, response);
  }
}
