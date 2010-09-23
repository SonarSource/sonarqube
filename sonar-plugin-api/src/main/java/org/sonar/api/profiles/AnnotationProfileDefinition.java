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

import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleAnnotationUtils;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.check.BelongsToProfile;

import java.util.Collection;

/**
 * @since 2.3
 */
public abstract class AnnotationProfileDefinition extends ProfileDefinition {

  private String name;
  private String language;
  private String repositoryKey;
  private Collection<Class> annotatedClasses;
  private RuleFinder ruleFinder;

  protected AnnotationProfileDefinition(String repositoryKey, String profileName, String language, Collection<Class> annotatedClasses, RuleFinder ruleFinder) {
    this.name = profileName;
    this.language = language;
    this.repositoryKey = repositoryKey;
    this.annotatedClasses = annotatedClasses;
    this.ruleFinder = ruleFinder;
  }

  @Override
  public RulesProfile createProfile(ValidationMessages validation) {
    RulesProfile profile = RulesProfile.create(name, language);
    if (annotatedClasses != null) {
      for (Class aClass : annotatedClasses) {
        BelongsToProfile belongsToProfile = (BelongsToProfile) aClass.getAnnotation(BelongsToProfile.class);
        registerRule(aClass, belongsToProfile, profile, validation);
      }
    }
    return profile;
  }

  private void registerRule(Class aClass, BelongsToProfile belongsToProfile, RulesProfile profile, ValidationMessages validation) {
    if (belongsToProfile != null) {
      String ruleKey = RuleAnnotationUtils.getRuleKey(aClass);
      Rule rule = ruleFinder.findByKey(repositoryKey, ruleKey);
      if (rule == null) {
        validation.addErrorText("Rule not found: [repository=" + repositoryKey + ", key=" + ruleKey + "]");

      } else {
        RulePriority priority = null;
        if (belongsToProfile.priority() != null) {
          priority = RulePriority.fromCheckPriority(belongsToProfile.priority());
        }
        profile.activateRule(rule, priority);
      }
    }
  }
}
