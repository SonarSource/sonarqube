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
package org.sonarqube.tests.user;

import com.sonar.orchestrator.Orchestrator;
import java.util.concurrent.TimeUnit;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.tests.Byteman;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.WsUsers.CreateWsResponse.User;
import org.sonarqube.ws.client.user.SearchRequest;
import org.sonarqube.ws.client.user.UpdateRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.expectHttpError;

public class UserEsResilienceTest {

  @ClassRule
  public static final Orchestrator orchestrator;

  static {
    orchestrator = Byteman.enableScript(Orchestrator.builderEnv(), "resilience/user_indexer.btm").build();
  }

  @Rule
  public TestRule timeout = new DisableOnDebug(Timeout.builder()
    .withLookingForStuckThread(true)
    .withTimeout(60L, TimeUnit.SECONDS)
    .build());

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void creation_and_update_of_user_are_resilient_to_indexing_errors() throws Exception {
    String login = "error";

    // creation of user succeeds but index is not up-to-date (indexing
    // failures are not propagated to web services)
    User user = tester.users().generate(u -> u.setLogin(login));

    // user exists in db, it can't be created again.
    // However he's not indexed.
    expectHttpError(400, "An active user with login '" + login + "' already exists",
      () -> tester.users().generate(u -> u.setLogin(login)));
    assertThat(isReturnedInSearch(user.getLogin())).isFalse();

    while (!isReturnedInSearch(user.getLogin())) {
      // user is indexed by the recovery daemon, which runs every 5 seconds
      Thread.sleep(1_000L);
    }

    // update the name. Db operation succeeds but not ES indexing.
    // Renaming is not propagated to index as long as recovery does not
    // run.
    String newName = "renamed";
    tester.users().service().update(UpdateRequest.builder().setLogin(login).setName(newName).build());
    assertThat(isReturnedInSearch(newName)).isFalse();

    while (!isReturnedInSearch(newName)) {
      // user is indexed by the recovery daemon, which runs every 5 seconds
      Thread.sleep(1_000L);
    }
  }

  @Test
  public void creation_and_update_of_user_are_resilient_to_indexing_crash() throws Exception {
    String login = "crash";

    // creation of user succeeds in db but indexing crashes --> ws fails
    expectHttpError(500, () -> tester.users().generate(u -> u.setLogin(login)));

    // user exists in db, it can't be created again.
    // However he's not indexed.
    expectHttpError(400, "An active user with login '" + login + "' already exists",
      () -> tester.users().generate(u -> u.setLogin(login)));
    assertThat(isReturnedInSearch(login)).isFalse();

    while (!isReturnedInSearch(login)) {
      // user is indexed by the recovery daemon, which runs every 5 seconds
      Thread.sleep(1_000L);
    }

    // update the name. Db operation succeeds but ES indexing crashes.
    // Renaming is not propagated to index as long as recovery does not
    // run.
    String newName = "renamed";
    expectHttpError(500, () -> tester.users().service().update(UpdateRequest.builder().setLogin(login).setName(newName).build()));
    assertThat(isReturnedInSearch(newName)).isFalse();

    while (!isReturnedInSearch(newName)) {
      // user is indexed by the recovery daemon, which runs every 5 seconds
      Thread.sleep(1_000L);
    }
  }

  private boolean isReturnedInSearch(String name) {
    return tester.users().service().search(SearchRequest.builder().setQuery(name).build()).getUsersCount() == 1L;
  }

}
