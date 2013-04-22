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
package org.sonar.batch;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.batch.bootstrapper.Reactor;

import java.util.Iterator;
import java.util.Properties;

/**
 * @deprecated in 2.14. Replaced by {@link org.sonar.batch.bootstrapper.Batch}.
 */
@Deprecated
public final class Batch {

  private Object[] bootstrapExtensions;
  private ProjectReactor reactor;

  public Batch(ProjectReactor reactor, Object... bootstrapperComponents) {
    this.reactor = reactor;
    this.bootstrapExtensions = bootstrapperComponents;
  }

  /**
   * Used by sonar-runner 1.x and ant-task 1.x
   *
   * @deprecated since 2.9 because commons-configuration is replaced by ProjectDefinition#properties. Used by Ant Task 1.1
   */
  @Deprecated
  public Batch(Configuration configuration, Object... bootstrapperComponents) {
    // configuration is not needed
    // because it's already included in ProjectDefinition.
    this.bootstrapExtensions = bootstrapperComponents;
    this.reactor = extractProjectReactor(bootstrapperComponents);
  }

  /**
   * Used by sonar-runner 2.0.
   *
   * @deprecated in version 2.12.
   */
  @Deprecated
  public static Batch create(ProjectReactor projectReactor, Configuration configuration, Object... bootstrapperComponents) {
    if (configuration != null) {
      projectReactor.getRoot().setProperties(convertToProperties(configuration));
    }
    return new Batch(projectReactor, bootstrapperComponents);
  }

  static Properties convertToProperties(Configuration configuration) {
    Properties props = new Properties();
    Iterator keys = configuration.getKeys();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      // Configuration#getString() automatically splits strings by comma separator.
      String value = StringUtils.join(configuration.getStringArray(key), ",");
      props.setProperty(key, value);
    }
    return props;
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

  public void execute() {
    org.sonar.batch.bootstrapper.Batch.Builder builder = org.sonar.batch.bootstrapper.Batch.builder();
    builder.setProjectReactor(reactor);
    builder.addComponents(bootstrapExtensions);
    builder.build().execute();
  }
}
