/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.dbcleaner.period;

import java.util.Date;
import java.util.GregorianCalendar;

import org.sonar.api.database.model.Snapshot;

class KeepOneSnapshotByPeriodBetweenTwoDatesFilter extends SnapshotFilter {

  private final Date before;
  private final Date after;
  private GregorianCalendar calendar = new GregorianCalendar();
  private int lastFieldValue = -1;
  private final int dateField;

  KeepOneSnapshotByPeriodBetweenTwoDatesFilter(int dateField, Date before, Date after) {
    this.before = before;
    this.after = after;
    this.dateField = dateField;
  }

  @Override
  boolean filter(Snapshot snapshot) {
    boolean result = false;
    Date createdAt = snapshot.getCreatedAt();
    calendar.setTime(createdAt);
    int currentFieldValue = calendar.get(dateField);
    if (lastFieldValue != currentFieldValue && snapshot.getCreatedAt().after(after) && snapshot.getCreatedAt().before(before)) {
      result = true;
    }
    lastFieldValue = currentFieldValue;
    return result;
  }

}
