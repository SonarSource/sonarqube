/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.batch;

import org.apache.commons.configuration.Configuration;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.batch.bootstrap.BootstrapModule;
import org.sonar.batch.bootstrap.Module;
import org.sonar.batch.bootstrapper.Reactor;

public final class Batch {

  private Module bootstrapModule;

  /**
   * @deprecated since 2.9. Replaced by the factory method.
   */
  @Deprecated
  public Batch(Configuration configuration, Object... bootstrapperComponents) {
    this.bootstrapModule = new BootstrapModule(extractProjectReactor(bootstrapperComponents), configuration, bootstrapperComponents).init();
  }

  static ProjectReactor extractProjectReactor(Object[] components) {
    Reactor deprecatedReactor = null;
    for (Object component : components) {
      if (component instanceof ProjectReactor) {
        return (ProjectReactor) component;
      }
      if (component instanceof Reactor) {
        deprecatedReactor = (Reactor) component;
      }
    }

    if (deprecatedReactor == null) {
      throw new IllegalArgumentException("Project reactor is not defined");
    }
    return deprecatedReactor.toProjectReactor();
  }

  private Batch(ProjectReactor reactor, Configuration configuration, Object... bootstrapperComponents) {
    this.bootstrapModule = new BootstrapModule(reactor, configuration, bootstrapperComponents).init();
  }

  public static Batch create(ProjectReactor projectReactor, Configuration configuration, Object... bootstrapperComponents) {
    return new Batch(projectReactor, configuration, bootstrapperComponents);
  }

  /**
   * for unit tests
   */
  Batch(Module bootstrapModule) {
    this.bootstrapModule = bootstrapModule;
  }

  public void execute() {
    try {
      bootstrapModule.start();
    } finally {
      bootstrapModule.stop();
    }
  }
}
