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
package org.sonar.server.user.ws;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.user.UserQuery;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.EsUtils;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.ExternalIdentity;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.server.user.index.UserIndexDefinition.FIELD_ACTIVE;
import static org.sonar.server.user.index.UserIndexDefinition.FIELD_UUID;

public class AnonymizeActionTest {
  private final System2 system2 = new AlwaysIncreasingSystem2();

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final UserIndexer userIndexer = new UserIndexer(dbClient, es.client());
  private final UserAnonymizer userAnonymizer = new UserAnonymizer(db.getDbClient());
  private final WsActionTester ws = new WsActionTester(new AnonymizeAction(dbClient, userIndexer, userSession, userAnonymizer));

  @Test
  public void anonymize_user() {
    UserDto user = db.users().insertUser(u -> u
      .setLogin("ada.lovelace")
      .setName("Ada Lovelace")
      .setActive(false)
      .setEmail(null)
      .setScmAccounts((String) null)
      .setExternalIdentityProvider("provider")
      .setExternalLogin("external.login")
      .setExternalId("external.id"));
    logInAsSystemAdministrator();

    TestResponse response = anonymize(user.getLogin());

    verifyThatUserIsAnonymized(user.getUuid());
    verifyThatUserIsAnonymizedOnEs(user.getUuid());
    assertThat(response.getInput()).isEmpty();
  }

  @Test
  public void cannot_anonymize_active_user() {
    createAdminUser();
    UserDto user = db.users().insertUser();
    userSession.logIn(user.getLogin()).setSystemAdministrator();

    String login = user.getLogin();
    assertThatThrownBy(() -> anonymize(login))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("User '" + user.getLogin() + "' is not deactivated");
  }

  @Test
  public void requires_to_be_logged_in() {
    createAdminUser();

    assertThatThrownBy(() -> anonymize("someone"))
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  @Test
  public void requires_administrator_permission_on_sonarqube() {
    createAdminUser();
    userSession.logIn();

    assertThatThrownBy(() -> anonymize("someone"))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_if_user_does_not_exist() {
    createAdminUser();
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> anonymize("someone"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User 'someone' doesn't exist");
  }

  @Test
  public void fail_if_login_is_blank() {
    createAdminUser();
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> anonymize(""))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'login' parameter is missing");
  }

  @Test
  public void fail_if_login_is_missing() {
    createAdminUser();
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> anonymize(null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'login' parameter is missing");
  }

  @Test
  public void test_definition() {
    assertThat(ws.getDef().isPost()).isTrue();
    assertThat(ws.getDef().isInternal()).isFalse();
    assertThat(ws.getDef().params()).hasSize(1);
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }

  private TestResponse anonymize(@Nullable String login) {
    return anonymize(ws, login);
  }

  private TestResponse anonymize(WsActionTester ws, @Nullable String login) {
    TestRequest request = ws.newRequest()
      .setMethod("POST");
    Optional.ofNullable(login).ifPresent(t -> request.setParam("login", login));
    return request.execute();
  }

  private void verifyThatUserIsAnonymizedOnEs(String uuid) {
    SearchHits hits = es.client().search(EsClient.prepareSearch(UserIndexDefinition.TYPE_USER)
        .source(new SearchSourceBuilder()
          .query(boolQuery()
            .must(termQuery(FIELD_UUID, uuid))
            .must(termQuery(FIELD_ACTIVE, "false")))))
      .getHits();
    List<UserDoc> userDocs = EsUtils.convertToDocs(hits, UserDoc::new);
    assertThat(userDocs).hasSize(1);
    assertThat(userDocs.get(0).login()).startsWith("sq-removed-");
    assertThat(userDocs.get(0).name()).startsWith("sq-removed-");
  }

  private void verifyThatUserIsAnonymized(String uuid) {
    List<UserDto> users = dbClient.userDao().selectUsers(db.getSession(), UserQuery.builder().includeDeactivated().build());
    assertThat(users).hasSize(1);

    UserDto anonymized = dbClient.userDao().selectByUuid(db.getSession(), uuid);
    assertThat(anonymized.getLogin()).startsWith("sq-removed-");
    assertThat(anonymized.getName()).isEqualTo(anonymized.getLogin());
    assertThat(anonymized.getExternalLogin()).isEqualTo(anonymized.getLogin());
    assertThat(anonymized.getExternalId()).isEqualTo(anonymized.getLogin());
    assertThat(anonymized.getExternalIdentityProvider()).isEqualTo(ExternalIdentity.SQ_AUTHORITY);
    assertThat(anonymized.isActive()).isFalse();
  }

  private UserDto createAdminUser() {
    UserDto admin = db.users().insertUser();
    db.users().insertPermissionOnUser(admin, ADMINISTER);
    db.commit();
    return admin;
  }
}
