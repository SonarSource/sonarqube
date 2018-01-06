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
package org.sonar.scanner.postjob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.postjob.internal.DefaultPostJobDescriptor;
import org.sonar.api.config.Settings;

@ScannerSide
public class PostJobOptimizer {

  private static final Logger LOG = LoggerFactory.getLogger(PostJobOptimizer.class);

  private final Settings settings;

  public PostJobOptimizer(Settings settings) {
    this.settings = settings;
  }

  /**
   * Decide if the given PostJob should be executed.
   */
  public boolean shouldExecute(DefaultPostJobDescriptor descriptor) {
    if (!settingsCondition(descriptor)) {
      LOG.debug("'{}' skipped because one of the required properties is missing", descriptor.name());
      return false;
    }
    return true;
  }

  private boolean settingsCondition(DefaultPostJobDescriptor descriptor) {
    if (!descriptor.properties().isEmpty()) {
      for (String propertyKey : descriptor.properties()) {
        if (!settings.hasKey(propertyKey)) {
          return false;
        }
      }
    }
    return true;
  }

}
