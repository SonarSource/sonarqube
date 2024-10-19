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
package org.sonar.server.rule.ws;

import java.util.Objects;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.QProfileRules;
import org.sonar.server.rule.index.RuleIndexer;

import static com.google.common.base.Preconditions.checkArgument;

public class DeleteAction implements RulesWsAction {

  public static final String PARAM_KEY = "key";
  public static final String PARAM_ORGANIZATION = "organization";

  private final System2 system2;
  private final RuleIndexer ruleIndexer;
  private final DbClient dbClient;
  private final QProfileRules qProfileRules;
  private final RuleWsSupport ruleWsSupport;

  public DeleteAction(System2 system2, RuleIndexer ruleIndexer, DbClient dbClient, QProfileRules qProfileRules, RuleWsSupport ruleWsSupport) {
    this.system2 = system2;
    this.ruleIndexer = ruleIndexer;
    this.dbClient = dbClient;
    this.qProfileRules = qProfileRules;
    this.ruleWsSupport = ruleWsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("delete")
      .setDescription("Delete custom rule.<br/>" +
        "Requires the 'Administer Quality Profiles' permission")
      .setSince("4.4")
      .setPost(true)
      .setHandler(this);

    action
      .createParam(PARAM_KEY)
      .setDescription("Rule key")
      .setRequired(true)
      .setExampleValue("java:S1144");

    action.createParam(PARAM_ORGANIZATION)
            .setDescription("Organization key")
            .setRequired(true)
            .setExampleValue("org-key");
  }

  @Override
  public void handle(Request request, Response response) {
    RuleKey key = RuleKey.parse(request.mandatoryParam(PARAM_KEY));
    delete(key, request.mandatoryParam(PARAM_ORGANIZATION));
  }

  public void delete(RuleKey ruleKey, String organizationKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = ruleWsSupport.getOrganizationByKey(dbSession, organizationKey);
      ruleWsSupport.checkQProfileAdminPermission(organization);

      RuleDto rule = dbClient.ruleDao().selectOrFailByKey(dbSession, ruleKey);
      checkArgument(rule.isCustomRule(), "Rule '%s' cannot be deleted because it is not a custom rule", rule.getKey().toString());

      OrganizationDto organizationDto = dbClient.organizationDao().selectByKey(dbSession, organizationKey)
              .orElseThrow(() -> new NotFoundException("No organization with key " + organizationKey));
      checkArgument(Objects.equals(rule.getOrganizationUuid(), organizationDto.getUuid()),
              "Rule '%s' cannot be deleted because it's organization does not equals the input param", rule.getKey().toString());

      qProfileRules.deleteRule(dbSession, rule);

      rule.setStatus(RuleStatus.REMOVED);
      rule.setUpdatedAt(system2.now());
      dbClient.ruleDao().update(dbSession, rule);

      ruleIndexer.commitAndIndex(dbSession, rule.getUuid());
    }
  }
}
