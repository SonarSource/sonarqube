/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.checks.profiles;

import org.sonar.check.AnnotationIntrospector;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.BelongsToProfiles;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 2.1 (experimental)
 * @deprecated since 2.3
 */
@Deprecated
public final class AnnotationCheckProfileFactory {

  private AnnotationCheckProfileFactory() {
  }

  public static Collection<CheckProfile> create(String repositoryKey, String language, Collection<Class> checkClasses) {
    Map<String, CheckProfile> profilesByTitle = new HashMap<String, CheckProfile>();

    if (checkClasses != null) {
      for (Class aClass : checkClasses) {
        BelongsToProfiles belongsToProfiles = (BelongsToProfiles) aClass.getAnnotation(BelongsToProfiles.class);
        if (belongsToProfiles != null) {
          for (BelongsToProfile belongsToProfile : belongsToProfiles.value()) {
            registerProfile(profilesByTitle, aClass, belongsToProfile, repositoryKey, language);
          }
        }
        BelongsToProfile belongsToProfile = (BelongsToProfile) aClass.getAnnotation(BelongsToProfile.class);
        registerProfile(profilesByTitle, aClass, belongsToProfile, repositoryKey, language);
      }
    }

    return profilesByTitle.values();
  }

  private static void registerProfile(Map<String, CheckProfile> profilesByTitle, Class aClass, BelongsToProfile belongsToProfile, String repositoryKey, String language) {
    if (belongsToProfile != null) {
      String title = belongsToProfile.title();
      CheckProfile profile = profilesByTitle.get(title);
      if (profile == null) {
        profile = new CheckProfile(title, language);
        profilesByTitle.put(title, profile);
      }
      Check check = new Check(repositoryKey, AnnotationIntrospector.getCheckKey(aClass));
      check.setPriority(belongsToProfile.priority());
      profile.addCheck(check);
    }
  }
}
