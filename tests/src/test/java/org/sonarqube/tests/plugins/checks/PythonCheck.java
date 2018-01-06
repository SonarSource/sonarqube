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
package org.sonarqube.tests.plugins.checks;

import org.sonarqube.tests.plugins.Project;

public class PythonCheck implements Check {

  public static final String DIR = "src/python";

  @Override
  public void validate(Validation validation) {
    // all files have size measures, even empty __init__.py
    validation.mustHaveSize(DIR);

    for (String filePath : Project.allFilesInDir(DIR)) {
      if (filePath.endsWith("__init__.py")) {
        validation.mustHaveSource(filePath);
      } else {
        validation.mustHaveNonEmptySource(filePath);
        validation.mustHaveComments(filePath);
        validation.mustHaveComplexity(filePath);
      }
    }

    validation.mustHaveIssues(DIR + "/hasissues.py");
  }

}
