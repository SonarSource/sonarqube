/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.deprecated.components;

import org.sonar.api.batch.ResourceCreationLock;

/**
 * This lock is used to ensure that Sonar resources (files, packages, directories) are not created by buggy plugins
 * when saving measures/violations on unknown resources.
 *
 * @since 2.3
 * @deprecated not used since 4.2
 */
@Deprecated
public final class DefaultResourceCreationLock implements ResourceCreationLock {

  @Override
  public void lock() {
    // does nothing since 4.2. Creation of components (ex-resources) is
    // the responsibility of core, not plugins
  }

}
