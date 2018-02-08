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
package org.sonarqube.tests.user;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Favorites.Favorite;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.favorites.AddRequest;
import org.sonarqube.ws.client.favorites.RemoveRequest;
import org.sonarqube.ws.client.favorites.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;

/**
 * TODO This test should not require an analysis, only provisioning the project should be enough
 */
public class FavoritesWsTest {

  @ClassRule
  public static final Orchestrator orchestrator = UserSuite.ORCHESTRATOR;

  private static WsClient adminClient;

  @Before
  public void inspectProject() {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));
    adminClient = newAdminWsClient(orchestrator);
  }

  @Test
  public void favorites_web_service() {
    // GET (nothing)
    List<Favorite> favorites = adminClient.favorites().search(new SearchRequest()).getFavoritesList();
    assertThat(favorites).isEmpty();

    // POST (create favorites)
    adminClient.favorites().add(new AddRequest().setComponent("sample"));
    adminClient.favorites().add(new AddRequest().setComponent("sample:src/main/xoo/sample/Sample.xoo"));

    // GET (created favorites)
    favorites = adminClient.favorites().search(new SearchRequest()).getFavoritesList();
    assertThat(favorites.stream().map(Favorite::getKey)).containsOnly("sample", "sample:src/main/xoo/sample/Sample.xoo");

    // DELETE (a favorite)
    adminClient.favorites().remove(new RemoveRequest().setComponent("sample"));
    favorites = adminClient.favorites().search(new SearchRequest()).getFavoritesList();
    assertThat(favorites.stream().map(Favorite::getKey)).containsOnly("sample:src/main/xoo/sample/Sample.xoo");
  }

}
