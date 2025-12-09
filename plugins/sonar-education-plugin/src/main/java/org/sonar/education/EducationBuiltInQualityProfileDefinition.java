/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.education;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import static org.sonar.education.EducationRulesDefinition.EDUCATION_KEY;
import static org.sonar.education.EducationRulesDefinition.EDUCATION_RULE_REPOSITORY_KEY;
import static org.sonar.education.sensors.EducationPrinciplesSensor.EDUCATION_WITH_GENERIC_CONCEPTS_RULE_KEY;
import static org.sonar.education.sensors.EducationWithContextsSensor.EDUCATION_WITH_CONTEXTS_RULE_KEY;
import static org.sonar.education.sensors.EducationWithDetectedContextSensor.EDUCATION_WITH_DETECTED_CONTEXT_RULE_KEY;
import static org.sonar.education.sensors.EducationWithSingleContextSensor.EDUCATION_WITH_SINGLE_CONTEXT_RULE_KEY;

public class EducationBuiltInQualityProfileDefinition implements BuiltInQualityProfilesDefinition {
  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile("Built in QP for Education", EDUCATION_KEY);
    profile.setDefault(true);
    profile.activateRule(EDUCATION_RULE_REPOSITORY_KEY, EDUCATION_WITH_GENERIC_CONCEPTS_RULE_KEY);
    profile.activateRule(EDUCATION_RULE_REPOSITORY_KEY, EDUCATION_WITH_SINGLE_CONTEXT_RULE_KEY);
    profile.activateRule(EDUCATION_RULE_REPOSITORY_KEY, EDUCATION_WITH_CONTEXTS_RULE_KEY);
    profile.activateRule(EDUCATION_RULE_REPOSITORY_KEY, EDUCATION_WITH_DETECTED_CONTEXT_RULE_KEY);
    profile.done();
  }
}
