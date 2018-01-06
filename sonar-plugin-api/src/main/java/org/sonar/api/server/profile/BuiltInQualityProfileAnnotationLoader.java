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
package org.sonar.api.server.profile;

import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rules.RuleAnnotationUtils;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.NewBuiltInActiveRule;
import org.sonar.check.BelongsToProfile;

/**
 * Read definitions of quality profiles based on the annotation {@link BelongsToProfile} provided by sonar-check-api. It is used
 * to feed {@link BuiltInQualityProfilesDefinition}.
 *
 * @see BuiltInQualityProfilesDefinition
 * @since 6.6
 */
@ServerSide
public class BuiltInQualityProfileAnnotationLoader {

  public void load(BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile builtInProfile, String repositoryKey, Class... annotatedClasses) {
    for (Class<?> annotatedClass : annotatedClasses) {
      loadActiveRule(builtInProfile, repositoryKey, annotatedClass);
    }
  }

  @CheckForNull
  void loadActiveRule(BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile profile, String repositoryKey, Class<?> clazz) {
    BelongsToProfile belongsToProfile = clazz.getAnnotation(BelongsToProfile.class);
    if ((belongsToProfile != null) && StringUtils.equals(belongsToProfile.title(), profile.name())) {
      String ruleKey = RuleAnnotationUtils.getRuleKey(clazz);
      NewBuiltInActiveRule activeRule = profile.activateRule(repositoryKey, ruleKey);
      activeRule.overrideSeverity(belongsToProfile.priority().name());
    }
  }

}
