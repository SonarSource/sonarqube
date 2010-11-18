/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
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
package org.sonar.plugins.dbcleaner;

import java.util.Date;
import java.util.GregorianCalendar;

import org.sonar.api.database.model.Snapshot;

public class Utils {
  
  public static Snapshot createSnapshot(int id, String version) {
    Snapshot snapshot = new Snapshot();
    snapshot.setId(id);
    snapshot.setVersion(version);
    snapshot.setCreatedAt(new GregorianCalendar().getTime());
    return snapshot;
  }

  public static  Snapshot createSnapshot(int id, Date createdAt) {
    Snapshot snapshot = new Snapshot();
    snapshot.setId(id);
    snapshot.setCreatedAt(createdAt);
    return snapshot;
  }

  public static  Date day(int delta) {
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.add(GregorianCalendar.DAY_OF_YEAR, delta);
    return calendar.getTime();
  }

  public static  Date week(int delta, int dayOfWeek) {
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.add(GregorianCalendar.WEEK_OF_YEAR, delta);
    calendar.set(GregorianCalendar.DAY_OF_WEEK, dayOfWeek);
    return calendar.getTime();
  }

}
