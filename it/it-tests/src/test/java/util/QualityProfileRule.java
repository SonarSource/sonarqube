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
package util;

import com.sonar.orchestrator.Orchestrator;
import java.util.List;
import java.util.function.Consumer;
import org.junit.rules.ExternalResource;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.QualityProfiles.CreateWsResponse.QualityProfile;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.qualityprofile.ActivateRuleWsRequest;
import org.sonarqube.ws.client.qualityprofile.CreateRequest;
import org.sonarqube.ws.client.qualityprofile.DeleteRequest;
import org.sonarqube.ws.client.qualityprofile.QualityProfilesService;
import org.sonarqube.ws.client.rule.SearchWsRequest;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.newWsClient;

public class QualityProfileRule extends ExternalResource implements QualityProfileSupport {

  private final Orchestrator orchestrator;
  private QualityProfileSupport support;

  public QualityProfileRule(Orchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  @Override
  protected void before() throws Throwable {
    support = new QualityProfileSupportImpl(ItUtils.newAdminWsClient(orchestrator));
  }

  public QualityProfileSupport as(String login, String password) {
    return new QualityProfileSupportImpl(newUserWsClient(orchestrator, login, password));
  }

  public QualityProfileSupport asAnonymous() {
    return new QualityProfileSupportImpl(newWsClient(orchestrator));
  }

  @Override
  public QualityProfilesService getWsService() {
    return support.getWsService();
  }

  @Override
  public QualityProfile createXooProfile(Organizations.Organization organization, Consumer<CreateRequest.Builder>... populators) {
    return support.createXooProfile(organization, populators);
  }

  @Override
  public QualityProfileSupport delete(String profileKey) {
    return support.delete(profileKey);
  }

  @Override
  public QualityProfileSupport activateRule(String profileKey, String ruleKey) {
    return support.activateRule(profileKey, ruleKey);
  }

  @Override
  public QualityProfileSupport deactivateRule(String profileKey, String ruleKey) {
    return support.deactivateRule(profileKey, ruleKey);
  }

  @Override
  public QualityProfileSupport assertThatNumberOfActiveRulesEqualsTo(String profileKey, int expectedActiveRules) {
    return support.assertThatNumberOfActiveRulesEqualsTo(profileKey, expectedActiveRules);
  }

  private static class QualityProfileSupportImpl implements QualityProfileSupport {

    private final WsClient wsClient;

    private QualityProfileSupportImpl(WsClient wsClient) {
      this.wsClient = wsClient;
    }

    @Override
    public QualityProfilesService getWsService() {
      return wsClient.qualityProfiles();
    }

    @Override
    public QualityProfile createXooProfile(Organizations.Organization organization, Consumer<CreateRequest.Builder>... populators) {
      CreateRequest.Builder request = CreateRequest.builder()
        .setOrganizationKey(organization.getKey())
        .setLanguage("xoo")
        .setProfileName(randomAlphanumeric(10));
      stream(populators).forEach(p -> p.accept(request));
      return getWsService().create(request.build()).getProfile();
    }

    @Override
    public QualityProfileSupport delete(String profileKey) {
      getWsService().delete(new DeleteRequest(profileKey));
      return this;
    }

    @Override
    public QualityProfileSupport activateRule(String profileKey, String ruleKey) {
      ActivateRuleWsRequest request = ActivateRuleWsRequest.builder()
        .setProfileKey(profileKey)
        .setRuleKey(ruleKey)
        .build();
      getWsService().activateRule(request);
      return this;
    }

    @Override
    public QualityProfileSupport deactivateRule(String profileKey, String ruleKey) {
      getWsService().deactivateRule(profileKey, ruleKey);
      return this;
    }

    @Override
    public QualityProfileSupport assertThatNumberOfActiveRulesEqualsTo(String profileKey, int expectedActiveRules) {
      try {
        List<String> facetIds = asList("active_severities", "repositories", "languages", "severities", "statuses", "types");
        SearchWsRequest request = new SearchWsRequest()
          .setQProfile(profileKey)
          .setActivation(true)
          .setFacets(facetIds)
          .setFields(singletonList("actives"));
        Rules.SearchResponse response = wsClient.rules().search(request);

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
}
