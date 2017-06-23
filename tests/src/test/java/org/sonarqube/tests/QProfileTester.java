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
package org.sonarqube.tests;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.QualityProfiles.CreateWsResponse.QualityProfile;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.qualityprofile.ActivateRuleWsRequest;
import org.sonarqube.ws.client.qualityprofile.CreateRequest;
import org.sonarqube.ws.client.qualityprofile.QualityProfilesService;
import org.sonarqube.ws.client.rule.SearchWsRequest;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class QProfileTester {
  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private final Session session;

  QProfileTester(Session session) {
    this.session = session;
  }

  public QualityProfilesService service() {
    return session.wsClient().qualityProfiles();
  }

  @SafeVarargs
  public final QualityProfile createXooProfile(Organization organization, Consumer<CreateRequest.Builder>... populators) {
    int id = ID_GENERATOR.getAndIncrement();
    CreateRequest.Builder request = CreateRequest.builder()
      .setOrganizationKey(organization.getKey())
      .setLanguage("xoo")
      .setProfileName("Profile" + id);
    stream(populators).forEach(p -> p.accept(request));
    return service().create(request.build()).getProfile();
  }

  public QProfileTester activateRule(QualityProfile profile, String ruleKey) {
    return activateRule(profile.getKey(), ruleKey);
  }

  public QProfileTester activateRule(String profileKey, String ruleKey) {
    ActivateRuleWsRequest request = ActivateRuleWsRequest.builder()
      .setProfileKey(profileKey)
      .setRuleKey(ruleKey)
      .build();
    service().activateRule(request);
    return this;
  }

  public QProfileTester deactivateRule(QualityProfile profile, String ruleKey) {
    service().deactivateRule(profile.getKey(), ruleKey);
    return this;
  }

  public QProfileTester assertThatNumberOfActiveRulesEqualsTo(QualityProfile profile, int expectedActiveRules) {
    return assertThatNumberOfActiveRulesEqualsTo(profile.getKey(), expectedActiveRules);
  }

  public QProfileTester assertThatNumberOfActiveRulesEqualsTo(String profileKey, int expectedActiveRules) {
    try {
      List<String> facetIds = asList("active_severities", "repositories", "languages", "severities", "statuses", "types");
      SearchWsRequest request = new SearchWsRequest()
        .setQProfile(profileKey)
        .setActivation(true)
        .setFacets(facetIds)
        .setFields(singletonList("actives"));
      Rules.SearchResponse response = session.wsClient().rules().search(request);

      // assume that expectedActiveRules fits in first page of results
      assertThat(response.getRulesCount()).isEqualTo(expectedActiveRules);
      assertThat(response.getTotal()).isEqualTo(expectedActiveRules);
      assertThat(response.getActives().getActives()).as(response.toString()).hasSize(expectedActiveRules);

      // verify facets
      assertThat(response.getFacets().getFacetsCount()).isEqualTo(facetIds.size());
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
