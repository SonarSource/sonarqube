/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.profiles;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerSide;
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
@ServerSide
public final class AnnotationProfileParser {

  private final RuleFinder ruleFinder;

  public AnnotationProfileParser(RuleFinder ruleFinder) {
    this.ruleFinder = ruleFinder;
  }

  public RulesProfile parse(String repositoryKey, String profileName, String language, Collection<Class> annotatedClasses, ValidationMessages messages) {
    RulesProfile profile = RulesProfile.create(profileName, language);
    for (Class<?> aClass : annotatedClasses) {
      BelongsToProfile belongsToProfile = aClass.getAnnotation(BelongsToProfile.class);
      addRule(aClass, belongsToProfile, profile, repositoryKey, messages);
    }
    return profile;
  }

  private void addRule(Class aClass, BelongsToProfile annotation, RulesProfile profile, String repositoryKey, ValidationMessages messages) {
    if ((annotation != null) && StringUtils.equals(annotation.title(), profile.getName())) {
      String ruleKey = RuleAnnotationUtils.getRuleKey(aClass);
      Rule rule = ruleFinder.findByKey(repositoryKey, ruleKey);
      if (rule == null) {
        messages.addWarningText("Rule not found: [repository=" + repositoryKey + ", key=" + ruleKey + "]");

      } else {
        RulePriority priority = null;
        if (annotation.priority() != null) {
          priority = RulePriority.fromCheckPriority(annotation.priority());
        }
        profile.activateRule(rule, priority);
      }
    }
  }
}
