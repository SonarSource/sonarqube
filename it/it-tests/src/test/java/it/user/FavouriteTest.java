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
package it.user;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category4Suite;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Favourite;
import org.sonar.wsclient.services.FavouriteCreateQuery;
import org.sonar.wsclient.services.FavouriteDeleteQuery;
import org.sonar.wsclient.services.FavouriteQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

/**
 * TODO This test should not require an analysis, only provisioning the project should be enough
 */
public class FavouriteTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Before
  public void inspectProject() {
    orchestrator.resetData();
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));
  }

  @Test
  public void favourites_web_service() {
    Sonar adminWsClient = orchestrator.getServer().getAdminWsClient();

    // GET (nothing)
    List<Favourite> favourites = adminWsClient.findAll(new FavouriteQuery());
    assertThat(favourites).isEmpty();

    // POST (create favourites)
    Favourite favourite = adminWsClient.create(new FavouriteCreateQuery("sample"));
    assertThat(favourite).isNotNull();
    assertThat(favourite.getKey()).isEqualTo("sample");
    adminWsClient.create(new FavouriteCreateQuery("sample:src/main/xoo/sample/Sample.xoo"));

    // GET (created favourites)
    favourites = adminWsClient.findAll(new FavouriteQuery());
    assertThat(favourites.stream().map(Favourite::getKey)).containsOnly("sample", "sample:src/main/xoo/sample/Sample.xoo");

    // DELETE (a favourite)
    adminWsClient.delete(new FavouriteDeleteQuery("sample"));
    favourites = adminWsClient.findAll(new FavouriteQuery());
    assertThat(favourites.stream().map(Favourite::getKey)).containsOnly("sample:src/main/xoo/sample/Sample.xoo");
  }

}
