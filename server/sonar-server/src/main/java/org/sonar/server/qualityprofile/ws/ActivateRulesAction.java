/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.qualityprofile.BulkChangeResult;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.rule.ws.RuleQueryFactory;
import org.sonar.server.user.UserSession;

import static org.sonar.server.rule.ws.SearchAction.defineRuleSearchParameters;

@ServerSide
public class ActivateRulesAction implements QProfileWsAction {

  public static final String PROFILE_KEY = "profile_key";
  public static final String SEVERITY = "activation_severity";

  public static final String ACTIVATE_RULES_ACTION = "activate_rules";

  private final RuleQueryFactory ruleQueryFactory;
  private final UserSession userSession;
  private final RuleActivator ruleActivator;
  private final DbClient dbClient;
  private final QProfileWsSupport wsSupport;

  public ActivateRulesAction(RuleQueryFactory ruleQueryFactory, UserSession userSession, RuleActivator ruleActivator, QProfileWsSupport wsSupport, DbClient dbClient) {
    this.ruleQueryFactory = ruleQueryFactory;
    this.userSession = userSession;
    this.ruleActivator = ruleActivator;
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
  }

  public void define(WebService.NewController controller) {
    WebService.NewAction activate = controller
      .createAction(ACTIVATE_RULES_ACTION)
      .setDescription("Bulk-activate rules on one or several Quality profiles")
      .setPost(true)
      .setSince("4.4")
      .setHandler(this);

    defineRuleSearchParameters(activate);

    activate.createParam(PROFILE_KEY)
      .setDescription("Quality Profile Key. To retrieve a profile key for a given language please see <code>api/qualityprofiles/search</code>")
      .setRequired(true)
      .setExampleValue("java:MyProfile");

    activate.createParam(SEVERITY)
      .setDescription("Optional severity of rules activated in bulk")
      .setPossibleValues(Severity.ALL);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String qualityProfileKey = request.mandatoryParam(PROFILE_KEY);
    userSession.checkLoggedIn();
    BulkChangeResult result;
    try (DbSession dbSession = dbClient.openSession(false)) {
      wsSupport.checkPermission(dbSession, qualityProfileKey);
      result = ruleActivator.bulkActivate(ruleQueryFactory.createRuleQuery(dbSession, request), qualityProfileKey, request.param(SEVERITY));
    }
    BulkChangeWsResponse.writeResponse(result, response);
  }
}
