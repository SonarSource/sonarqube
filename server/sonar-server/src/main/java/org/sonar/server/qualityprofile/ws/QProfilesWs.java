/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.sonar.api.server.ws.WebService;

public class QProfilesWs implements WebService {

  public static final String API_ENDPOINT = "api/qualityprofiles";

  private final RuleActivationActions ruleActivationActions;
  private final BulkRuleActivationActions bulkRuleActivationActions;
  private final ProjectAssociationActions projectAssociationActions;
  private final QProfileWsAction[] actions;

  public QProfilesWs(RuleActivationActions ruleActivationActions,
                     BulkRuleActivationActions bulkRuleActivationActions,
                     ProjectAssociationActions projectAssociationActions,
                     QProfileWsAction... actions) {
    this.ruleActivationActions = ruleActivationActions;
    this.bulkRuleActivationActions = bulkRuleActivationActions;
    this.projectAssociationActions = projectAssociationActions;
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController(API_ENDPOINT)
      .setDescription("Quality Profiles")
      .setSince("4.4");

    ruleActivationActions.define(controller);
    bulkRuleActivationActions.define(controller);
    projectAssociationActions.define(controller);
    for(QProfileWsAction action: actions) {
      action.define(controller);
    }

    controller.done();
  }
}
