/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.workflow.condition;

import com.google.common.annotations.Beta;
import org.sonar.api.config.Settings;
import org.sonar.api.workflow.Review;
import org.sonar.api.workflow.WorkflowContext;

import javax.annotation.Nullable;

/**
 * Checks that a project property is set, whatever its value.
 *
 * @since 3.1
 */
@Beta
public final class HasProjectPropertyCondition extends ProjectPropertyCondition {

  public HasProjectPropertyCondition(String propertyKey) {
    super(propertyKey);
  }

  @Override
  public boolean doVerify(@Nullable Review review, WorkflowContext context) {
    Settings settings = context.getProjectSettings();
    return settings.hasKey(getPropertyKey()) || settings.getDefaultValue(getPropertyKey()) != null;
  }

  @Override
  public String toString() {
    return "Property " + getPropertyKey() + " must be set";
  }
}
