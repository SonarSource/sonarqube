/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonarqube.ws.tester;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Qualityprofiles;
import org.sonarqube.ws.Qualityprofiles.CreateWsResponse.QualityProfile;
import org.sonarqube.ws.client.qualityprofiles.ActivateRuleRequest;
import org.sonarqube.ws.client.qualityprofiles.AddProjectRequest;
import org.sonarqube.ws.client.qualityprofiles.CreateRequest;
import org.sonarqube.ws.client.qualityprofiles.DeactivateRuleRequest;
import org.sonarqube.ws.client.qualityprofiles.DeleteRequest;
import org.sonarqube.ws.client.qualityprofiles.QualityprofilesService;
import org.sonarqube.ws.client.qualityprofiles.SearchRequest;

import static java.util.Arrays.stream;

public class QProfileTester {
  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private final TesterSession session;

  QProfileTester(TesterSession session) {
    this.session = session;
  }

  public QualityprofilesService service() {
    return session.wsClient().qualityprofiles();
  }

  void deleteAll() {
    List<Qualityprofiles.SearchWsResponse.QualityProfile> qualityProfiles = session.wsClient().qualityprofiles().search(new SearchRequest()).getProfilesList().stream()
      .filter(qp -> !qp.getIsDefault())
      .filter(qp -> !qp.getIsBuiltIn())
      .filter(qp -> qp.getParentKey() == null || qp.getParentKey().equals(""))
      .toList();

    qualityProfiles.forEach(
      qp -> session.wsClient().qualityprofiles().delete(new DeleteRequest().setQualityProfile(qp.getName()).setLanguage(qp.getLanguage())));
  }

  @SafeVarargs
  public final QualityProfile createXooProfile(Consumer<CreateRequest>... populators) {
    CreateRequest request = new CreateRequest()
      .setLanguage("xoo")
      .setName(generateName());
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

  public QProfileTester activateRule(QualityProfile profile, String ruleKey, String severity, List<String> params) {
    return activateRule(profile.getKey(), ruleKey, severity, params);
  }

  public QProfileTester activateRule(QualityProfile profile, String ruleKey, String severity) {
    return activateRule(profile.getKey(), ruleKey, severity, null);
  }

  public QProfileTester activateRule(String profileKey, String ruleKey, String severity) {
    return activateRule(profileKey, ruleKey, severity, null);
  }

  public QProfileTester activateRule(String profileKey, String ruleKey, String severity, @Nullable List<String> params) {
    service().activateRule(new ActivateRuleRequest()
      .setKey(profileKey)
      .setRule(ruleKey)
      .setSeverity(severity)
      .setParams(params));
    return this;
  }

  public QProfileTester deactivateRule(QualityProfile profile, String ruleKey) {
    service().deactivateRule(new DeactivateRuleRequest().setKey(profile.getKey()).setRule(ruleKey));
    return this;
  }

  public QProfileTester assignQProfileToProject(QualityProfile profile, Project project) {
    return assignQProfileToProject(profile, project.getKey());
  }

  public QProfileTester assignQProfileToProject(QualityProfile profile, String projectKey) {
    return assignQProfileToProject(profile.getName(), profile.getLanguage(), projectKey);
  }

  public QProfileTester assignQProfileToProject(String profileName, String profileLanguage, String projectKey) {
    service().addProject(new AddProjectRequest()
      .setProject(projectKey)
      .setQualityProfile(profileName)
      .setLanguage(profileLanguage));
    return this;
  }

  public String generateName() {
    int id = ID_GENERATOR.getAndIncrement();
    return "Profile" + id;
  }
}
