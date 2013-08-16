/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

class ExceptionHandling {

  static RuntimeException handle(Exception e, Log log) throws MojoExecutionException {
    Throwable source = e;
    if (e.getClass().getName().equals("org.sonar.runner.impl.RunnerException") && e.getCause() != null) {
      source = e.getCause();
    }
    log.error(source.getMessage());
    throw new MojoExecutionException(source.getMessage(), source);
  }

  static RuntimeException handle(String message, Log log) throws MojoExecutionException {
    return handle(new MojoExecutionException(message), log);
  }
}
