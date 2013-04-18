/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.batch.scan;

import com.google.common.base.Joiner;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.SonarException;

import java.util.ArrayList;
import java.util.List;

/**
 * This class aims at validating project reactor
 * @since 3.6
 */
public class ProjectReactorValidator {

  private static final String VALID_MODULE_KEY_REGEXP = "[0-9a-zA-Z\\-_\\.:]+";

  public void validate(ProjectReactor reactor) {
    List<String> validationMessages = new ArrayList<String>();
    for (ProjectDefinition def : reactor.getProjects()) {
      validate(def, validationMessages);
    }

    if (!validationMessages.isEmpty()) {
      throw new SonarException("Validation of project reactor failed:\n  o " + Joiner.on("\n  o ").join(validationMessages));
    }
  }

  private void validate(ProjectDefinition def, List<String> validationMessages) {
    if (!def.getKey().matches(VALID_MODULE_KEY_REGEXP)) {
      validationMessages.add(String.format("%s is not a valid project or module key", def.getKey()));
    }
  }

}
