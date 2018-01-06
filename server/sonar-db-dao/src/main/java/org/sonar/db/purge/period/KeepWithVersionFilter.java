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
package org.sonar.db.purge.period;

import com.google.common.base.Strings;
import java.util.Date;
import java.util.List;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.purge.PurgeableAnalysisDto;

class KeepWithVersionFilter implements Filter {

  private final Date before;

  KeepWithVersionFilter(Date before) {
    this.before = before;
  }

  @Override
  public List<PurgeableAnalysisDto> filter(List<PurgeableAnalysisDto> history) {
    return history.stream()
      .filter(analysis -> analysis.getDate().before(before))
      .filter(KeepWithVersionFilter::isDeletable)
      .collect(MoreCollectors.toList());
  }

  @Override
  public void log() {
    Loggers.get(getClass()).debug("-> Keep analyses with a version prior to {}", DateUtils.formatDate(before));
  }

  private static boolean isDeletable(PurgeableAnalysisDto snapshot) {
    return !snapshot.isLast() && Strings.isNullOrEmpty(snapshot.getVersion());
  }

}
