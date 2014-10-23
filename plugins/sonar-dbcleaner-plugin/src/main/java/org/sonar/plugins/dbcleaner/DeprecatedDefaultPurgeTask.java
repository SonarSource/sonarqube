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

package org.sonar.plugins.dbcleaner;

import org.sonar.api.config.Settings;
import org.sonar.core.computation.dbcleaner.DefaultPurgeTask;
import org.sonar.core.computation.dbcleaner.period.DefaultPeriodCleaner;
import org.sonar.core.purge.PurgeDao;
import org.sonar.core.purge.PurgeProfiler;
import org.sonar.plugins.dbcleaner.api.PurgeTask;

/**
 * @since 2.14
 */
@Deprecated
public class DeprecatedDefaultPurgeTask implements PurgeTask {
  private final DefaultPurgeTask defaultPurgeTask;

  public DeprecatedDefaultPurgeTask(PurgeDao purgeDao, Settings settings, DefaultPeriodCleaner periodCleaner, PurgeProfiler profiler) {
    defaultPurgeTask = new DefaultPurgeTask(purgeDao, settings, periodCleaner, profiler);
  }

  @Override
  public DeprecatedDefaultPurgeTask delete(long resourceId) {
    defaultPurgeTask.delete(resourceId);
    return this;
  }

  @Override
  public DeprecatedDefaultPurgeTask purge(long resourceId) {
    defaultPurgeTask.purge(resourceId);
    return this;
  }
}
