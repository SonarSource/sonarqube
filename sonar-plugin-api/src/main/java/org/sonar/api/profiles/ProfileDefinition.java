/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.profiles;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.ValidationMessages;

/**
 * Define a quality profile which is automatically registered during SonarQube startup (ie built-in quality profile).
 * The component {@link XMLProfileParser} can be used to help implementing the method create().
 * <p>
 * Example:
 * <pre>
 * public class MyProfile extends ProfileDefinition {
 * 
 *   private final RuleFinder ruleFinder;
 * 
 *   public MyProfile(RuleFinder ruleFinder) {
 *     this.ruleFinder = ruleFinder;
 *   }
 * 
 *   {@literal @}Override
 *   public RulesProfile createProfile(ValidationMessages validation) {
 *     final RulesProfile profile = RulesProfile.create("My Profile", "java");

 *     // activate some rules overriding the default severity
 *     profile.activateRule(Rule.create("checkstyle", "UnusedVariable"), Severity.MAJOR);
 *     
 *     // Activate all rules from a repo and keep default severity
 *     for (Rule r : ruleFinder.findAll(RuleQuery.create().withRepository("pmd"))) {
 *       profile.activateRule(r, null);
 *     }
 *     // Discouraged with curent implementation since it will do one SQL query per rule
 *     // Activate a single rule and keep default severity
 *     profile.activateRule(ruleFinder.findByKey("checkstyle", "UnusedParameter"), null);
 *     // ...
 *
 *     return profile;
 *   }
 * }
 * </pre>
 * @since 2.3
 */
@ServerSide
@ExtensionPoint
public abstract class ProfileDefinition {

  public abstract RulesProfile createProfile(ValidationMessages validation);

}
