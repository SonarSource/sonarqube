/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.api.batch.fs.internal;

import javax.annotation.concurrent.Immutable;

import org.sonar.api.batch.bootstrap.ImmutableProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputModule;

/**
 * @since 5.2
 */
@Immutable
public class DefaultInputModule extends DefaultInputComponent implements InputModule {

  private final String moduleKey;
  private final ImmutableProjectDefinition definition;

  /**
   * For testing only!
   */
  public DefaultInputModule(String moduleKey) {
    this(ProjectDefinition.create().setKey(moduleKey).build(), TestInputFileBuilder.nextBatchId());
  }

  public DefaultInputModule(ImmutableProjectDefinition definition, int batchId) {
    super(batchId);
    this.definition = definition;
    this.moduleKey = definition.getKey();
  }

  /**
   * Module key without branch
   */
  @Override
  public String key() {
    return moduleKey;
  }

  @Override
  public boolean isFile() {
    return false;
  }

  public ImmutableProjectDefinition definition() {
    return definition;
  }

}
