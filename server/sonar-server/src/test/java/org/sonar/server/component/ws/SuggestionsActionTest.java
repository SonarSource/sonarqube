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
package org.sonar.server.component.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.component.index.ComponentIndexer;
import org.sonar.server.es.EsTester;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.db.component.ComponentTesting.newProjectDto;

public class SuggestionsActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = new EsTester(new ComponentIndexDefinition(new MapSettings()));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private ComponentIndexer componentIndexer = new ComponentIndexer(db.getDbClient(), es.client());
  private SuggestionsAction action;
  private OrganizationDto organization;
  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, componentIndexer);

  @Before
  public void setUp() {
    ComponentIndex index = new ComponentIndex(es.client(), new AuthorizationTypeSupport(userSessionRule));
    action = new SuggestionsAction(db.getDbClient(), index);
    organization = db.organizations().insert();
  }

  @Test
  public void exact_match_in_one_qualifier() {
    ComponentDto project = db.components().insertComponent(newProjectDto(organization));

    componentIndexer.index();
    authorizationIndexerTester.allowOnlyAnyone(project);

    SuggestionsWsResponse response = action.doHandle(project.getKey());

    // assert match in qualifier "TRK"
    assertThat(response.getResultsList())
      .filteredOn(q -> q.getItemsCount() > 0)
      .extracting(SuggestionsWsResponse.Qualifier::getQ)
      .containsExactly(Qualifiers.PROJECT);

    // assert correct id to be found
    assertThat(response.getResultsList())
      .flatExtracting(SuggestionsWsResponse.Qualifier::getItemsList)
      .extracting(WsComponents.Component::getKey, WsComponents.Component::getOrganization)
      .containsExactly(tuple(project.getKey(), organization.getKey()));
  }

}
