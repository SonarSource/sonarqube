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

import java.util.function.Consumer;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.QualityProfiles.CreateWsResponse.QualityProfile;
import org.sonarqube.ws.client.qualityprofile.CreateRequest;
import org.sonarqube.ws.client.qualityprofile.QualityProfilesService;

public interface QualityProfileSupport {

  QualityProfilesService getWsService();

  QualityProfile createXooProfile(Organizations.Organization organization, Consumer<CreateRequest.Builder>... populators);

  QualityProfileSupport delete(String profileKey);

  default QualityProfileSupport delete(QualityProfile profile) {
    return delete(profile.getKey());
  }

  default QualityProfileSupport delete(QualityProfiles.SearchWsResponse.QualityProfile profile) {
    return delete(profile.getKey());
  }

  QualityProfileSupport activateRule(String profileKey, String ruleKey);

  default QualityProfileSupport activateRule(QualityProfile profile, String ruleKey) {
    return activateRule(profile.getKey(), ruleKey);
  }

  default QualityProfileSupport activateRule(QualityProfiles.SearchWsResponse.QualityProfile profile, String ruleKey) {
    return activateRule(profile.getKey(), ruleKey);
  }

  QualityProfileSupport deactivateRule(String profileKey, String ruleKey);

  default QualityProfileSupport deactivateRule(QualityProfile profile, String ruleKey) {
    return deactivateRule(profile.getKey(), ruleKey);
  }

  default QualityProfileSupport deactivateRule(QualityProfiles.SearchWsResponse.QualityProfile profile, String ruleKey) {
    return deactivateRule(profile.getKey(), ruleKey);
  }

  QualityProfileSupport assertThatNumberOfActiveRulesEqualsTo(String profileKey, int expectedActiveRules);

  default QualityProfileSupport assertThatNumberOfActiveRulesEqualsTo(QualityProfile profile, int expectedActiveRules) {
    return assertThatNumberOfActiveRulesEqualsTo(profile.getKey(), expectedActiveRules);
  }

  default QualityProfileSupport assertThatNumberOfActiveRulesEqualsTo(QualityProfiles.SearchWsResponse.QualityProfile profile, int expectedActiveRules) {
    return assertThatNumberOfActiveRulesEqualsTo(profile.getKey(), expectedActiveRules);
  }
}
