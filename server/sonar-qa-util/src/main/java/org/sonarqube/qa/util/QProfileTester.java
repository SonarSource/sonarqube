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
package org.sonarqube.qa.util;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Qualityprofiles.CreateWsResponse.QualityProfile;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.qualityprofiles.ActivateRuleRequest;
import org.sonarqube.ws.client.qualityprofiles.AddProjectRequest;
import org.sonarqube.ws.client.qualityprofiles.CreateRequest;
import org.sonarqube.ws.client.qualityprofiles.DeactivateRuleRequest;
import org.sonarqube.ws.client.qualityprofiles.QualityprofilesService;
import org.sonarqube.ws.client.rules.SearchRequest;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class QProfileTester {
  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private final TesterSession session;

  QProfileTester(TesterSession session) {
    this.session = session;
  }

  public QualityprofilesService service() {
    return session.wsClient().qualityprofiles();
  }

  @SafeVarargs
  public final QualityProfile createXooProfile(Organization organization, Consumer<CreateRequest>... populators) {
    int id = ID_GENERATOR.getAndIncrement();
    CreateRequest request = new CreateRequest()
      .setOrganization(organization.getKey())
      .setLanguage("xoo")
      .setName("Profile" + id);
    stream(populators).forEach(p -> p.accept(request));
    return service().create(request).getProfile();
  }

  public QProfileTester activateRule(QualityProfile profile, String ruleKey) {
    return activateRule(profile.getKey(), ruleKey);
  }

  public QProfileTester activateRule(String profileKey, String ruleKey) {
    ActivateRuleRequest request = new ActivateRuleRequest()
      .setKey(profileKey)
      .setRule(ruleKey);
    service().activateRule(request);
    return this;
  }

  public QProfileTester activateRule(QualityProfile profile, String ruleKey, String severity) {
    return activateRule(profile.getKey(), ruleKey, severity);
  }

  public QProfileTester activateRule(String profileKey, String ruleKey, String severity) {
    ActivateRuleRequest request = new ActivateRuleRequest()
      .setKey(profileKey)
      .setRule(ruleKey)
      .setSeverity(severity);
    service().activateRule(request);
    return this;
  }

  public QProfileTester deactivateRule(QualityProfile profile, String ruleKey) {
    service().deactivateRule(new DeactivateRuleRequest().setKey(profile.getKey()).setRule(ruleKey));
    return this;
  }

  public QProfileTester assignQProfileToProject(QualityProfile profile, Project project) {
    service().addProject(new AddProjectRequest()
      .setProject(project.getKey())
      .setKey(profile.getKey()));
    return this;
  }

  public QProfileTester assertThatNumberOfActiveRulesEqualsTo(QualityProfile profile, int expectedActiveRules) {
    return assertThatNumberOfActiveRulesEqualsTo(profile.getKey(), expectedActiveRules);
  }

  public QProfileTester assertThatNumberOfActiveRulesEqualsTo(String profileKey, int expectedActiveRules) {
    try {
      List<String> facetIds = asList("active_severities", "repositories", "languages", "severities", "statuses", "types");
      SearchRequest request = new SearchRequest()
        .setQprofile(profileKey)
        .setActivation("true")
        .setFacets(facetIds)
        .setF(singletonList("actives"));
      Rules.SearchResponse response = session.wsClient().rules().search(request);

      // assume that expectedActiveRules fits in first page of results
      Assertions.assertThat(response.getRulesCount()).isEqualTo(expectedActiveRules);
      Assertions.assertThat(response.getTotal()).isEqualTo(expectedActiveRules);
      Assertions.assertThat(response.getActives().getActives()).as(response.toString()).hasSize(expectedActiveRules);

      // verify facets
      Assertions.assertThat(response.getFacets().getFacetsCount()).isEqualTo(facetIds.size());
      response.getFacets().getFacetsList().forEach(facet -> {
        long total = facet.getValuesList().stream()
          .mapToLong(Common.FacetValue::getCount)
          .sum();
        assertThat(total).as("Facet " + facet.getProperty()).isEqualTo((long) expectedActiveRules);
      });
    } catch (HttpException e) {
      if (expectedActiveRules == 0 && e.code() == 404) {
        // profile does not exist, do nothing
        return this;
      }
      throw e;
    }
    return this;
  }
}
