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
package org.sonar.ce.task.step;

import org.sonar.api.ce.ComputeEngineSide;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Execute {@link StatelessInitExtension} instances in no specific order.
 * If an extension fails (throws an exception), consecutive extensions
 * won't be called.
 */
@ComputeEngineSide
public class ExecuteStatelessInitExtensionsStep implements ComputationStep {

  private final StatelessInitExtension[] extensions;

  @Autowired(required = false)
  public ExecuteStatelessInitExtensionsStep(StatelessInitExtension[] extensions) {
    this.extensions = extensions;
  }

  /**
   * Used when zero {@link StatelessInitExtension} are registered into container.
   */
  @Autowired(required = false)
  public ExecuteStatelessInitExtensionsStep() {
    this(new StatelessInitExtension[0]);
  }

  @Override
  public void execute(Context context) {
    for (StatelessInitExtension extension : extensions) {
      extension.onInit();
    }
  }

  @Override
  public String getDescription() {
    return "Initialize";
  }
}
