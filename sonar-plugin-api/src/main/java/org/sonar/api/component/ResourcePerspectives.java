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
package org.sonar.api.component;

import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.resources.Resource;

/**
 * @since 3.5
 * @deprecated since 5.6 all features are available on {@link SensorContext}
 */
@Deprecated
public interface ResourcePerspectives {

  @CheckForNull
  <P extends Perspective> P as(Class<P> perspectiveClass, Resource resource);

  /**
   * Allow to create perspective from {@link InputPath}. In particular from {@link InputFile}.
   * @since 4.5.2
   */
  @CheckForNull
  <P extends Perspective> P as(Class<P> perspectiveClass, InputPath inputPath);
}
