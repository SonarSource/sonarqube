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
package org.sonar.education.sensors;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.rule.ActiveRules;

public class EducationWith2LinkedCodeSnippetsSensor extends EducationRuleSensor {

  public static final String EDUCATION_WITH_2_LINKED_CODE_SNIPPETS_RULE_KEY = "2_LINKED_CODE_SNIPPETS";

  public EducationWith2LinkedCodeSnippetsSensor(FileSystem fs, ActiveRules activeRules) {
    super(fs, activeRules, EDUCATION_WITH_2_LINKED_CODE_SNIPPETS_RULE_KEY);
  }
}
