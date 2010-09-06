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
package org.sonar.api.profiles;

import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.check.AnnotationIntrospector;
import org.sonar.check.BelongsToProfile;

import java.io.Reader;
import java.util.Collection;

/**
 * @since 2.3
 */
public final class AnnotationProfileImporter extends ProfileImporter {

  private String repositoryKey;
  private Collection<Class> annotatedClasses;

  AnnotationProfileImporter(String repositoryKey, Collection<Class> annotatedClasses) {
    this.repositoryKey = repositoryKey;
    this.annotatedClasses = annotatedClasses;
  }

  public static AnnotationProfileImporter create(String repositoryKey, Collection<Class> annotatedClasses) {
    return new AnnotationProfileImporter(repositoryKey, annotatedClasses);
  }

  @Override
  public ProfilePrototype importProfile(Reader reader, ValidationMessages messages) {
    ProfilePrototype profile = ProfilePrototype.create();
    if (annotatedClasses != null) {
      for (Class aClass : annotatedClasses) {
        BelongsToProfile belongsToProfile = (BelongsToProfile) aClass.getAnnotation(BelongsToProfile.class);
        registerRule(aClass, belongsToProfile, profile);
      }
    }

    return profile;
  }

  private void registerRule(Class aClass, BelongsToProfile belongsToProfile, ProfilePrototype profile) {
    if (belongsToProfile != null) {
      RulePriority priority = null;
      if (belongsToProfile.priority() != null) {
        priority = RulePriority.fromCheckPriority(belongsToProfile.priority());
      }
      profile.activateRule(repositoryKey, AnnotationIntrospector.getCheckKey(aClass), priority);
    }
  }
}
