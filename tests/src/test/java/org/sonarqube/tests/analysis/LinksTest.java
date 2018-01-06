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
package org.sonarqube.tests.analysis;

import com.google.common.collect.ImmutableMap;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarScanner;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.tests.Category3Suite;
import org.sonarqube.ws.ProjectLinks;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.projectlinks.SearchRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class LinksTest {

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  private static final String PROJECT_KEY = "com.sonarsource.it.samples:simple-sample";

  @After
  public void cleanProjectLinksTable() {
    orchestrator.getServer().post("api/projects/delete", ImmutableMap.of("key", PROJECT_KEY));
  }

  /**
   * SONAR-3676
   */
  @Test
  public void shouldUseLinkProperties() {
    SonarScanner runner = SonarScanner.create(ItUtils.projectDir("analysis/links-project"))
      .setProperty("sonar.scm.disabled", "true");
    orchestrator.executeBuild(runner);

    verifyLinks();
  }

  /**
   * SONAR-3676
   */
  @Test
  public void shouldUseLinkPropertiesOverPomLinksInMaven() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("analysis/links-project"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.scm.disabled", "true");
    orchestrator.executeBuild(build);

    verifyLinks();
  }

  private void verifyLinks() {
    WsClient wsClient = ItUtils.newWsClient(orchestrator);
    List<ProjectLinks.Link> links = wsClient.projectLinks().search(new SearchRequest().setProjectKey(PROJECT_KEY)).getLinksList();
    verifyLink(links, "homepage", "http://www.simplesample.org_OVERRIDDEN");
    verifyLink(links, "ci", "http://bamboo.ci.codehaus.org/browse/SIMPLESAMPLE");
    verifyLink(links, "issue", "http://jira.codehaus.org/browse/SIMPLESAMPLE");
    verifyLink(links, "scm", "https://github.com/SonarSource/simplesample");
    verifyLink(links, "scm_dev", "scm:git:git@github.com:SonarSource/simplesample.git");
  }

  private void verifyLink(List<ProjectLinks.Link> links, String expectedType, String expectedUrl) {
    Optional<ProjectLinks.Link> link = links.stream()
      .filter(l -> l.getType().equals(expectedType))
      .findFirst();
    assertThat(link).isPresent();
    assertThat(link.get().getUrl()).isEqualTo(expectedUrl);
  }
}
