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

import static org.sonar.core.util.Uuids.UUID_EXAMPLE_04;
import static org.sonar.server.qualityprofile.ws.BulkChangeWsResponse.writeResponse;
import static org.sonar.server.rule.ws.SearchAction.defineRuleSearchParameters;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_DEACTIVATE_RULES;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_TARGET_KEY;

public class DeactivateRulesAction implements QProfileWsAction {
  public static final String SEVERITY = "activation_severity";

  private final RuleQueryFactory ruleQueryFactory;
  private final UserSession userSession;
  private final QProfileRules ruleActivator;
  private final QProfileWsSupport wsSupport;
  private final DbClient dbClient;

  public DeactivateRulesAction(RuleQueryFactory ruleQueryFactory, UserSession userSession, QProfileRules ruleActivator, QProfileWsSupport wsSupport, DbClient dbClient) {
    this.ruleQueryFactory = ruleQueryFactory;
    this.userSession = userSession;
    this.ruleActivator = ruleActivator;
    this.wsSupport = wsSupport;
    this.dbClient = dbClient;
  }

  public void define(WebService.NewController controller) {
    WebService.NewAction deactivate = controller
      .createAction(ACTION_DEACTIVATE_RULES)
      .setDescription("Bulk deactivate rules on Quality profiles.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Profiles'</li>" +
        "  <li>Edit right on the specified quality profile</li>" +
        "</ul>")
      .setPost(true)
      .setSince("4.4")
      .setHandler(this);

    defineRuleSearchParameters(deactivate);

    deactivate.createParam(PARAM_TARGET_KEY)
      .setDescription("Quality Profile key on which the rule deactivation is done. To retrieve a profile key please see <code>api/qualityprofiles/search</code>")
      .setDeprecatedKey("profile_key", "6.5")
      .setRequired(true)
      .setExampleValue(UUID_EXAMPLE_04);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String qualityProfileKey = request.mandatoryParam(PARAM_TARGET_KEY);
    userSession.checkLoggedIn();
    BulkChangeResult result;
    try (DbSession dbSession = dbClient.openSession(false)) {
      QProfileDto profile = wsSupport.getProfile(dbSession, QProfileReference.fromKey(qualityProfileKey));
      OrganizationDto organization = wsSupport.getOrganization(dbSession, profile);
      wsSupport.checkCanEdit(dbSession, organization, profile);
      result = ruleActivator.bulkDeactivateAndCommit(dbSession, profile, ruleQueryFactory.createRuleQuery(dbSession, request));
    }
    writeResponse(result, response);
  }
}
