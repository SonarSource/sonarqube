/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.event;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventComponentChangeDto.ChangeCategory;

public class EventDbTester {
  private final DbTester db;
  private final DbClient dbClient;
  private final DbSession dbSession;

  public EventDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  public EventDto insertEvent(EventDto event) {
    dbClient.eventDao().insert(dbSession, event);
    db.commit();

    return event;
  }

  public EventDto insertEvent(SnapshotDto analysis) {
    EventDto event = EventTesting.newEvent(analysis);
    dbClient.eventDao().insert(dbSession, event);
    db.commit();

    return event;
  }

  public EventComponentChangeDto insertEventComponentChanges(EventDto event, SnapshotDto analysis,
    ChangeCategory changeCategory, ComponentDto component, @Nullable BranchDto branch) {

    EventComponentChangeDto eventComponentChange = new EventComponentChangeDto()
      .setUuid(UuidFactoryFast.getInstance().create())
      .setCategory(changeCategory)
      .setEventUuid(event.getUuid())
      .setComponentUuid(component.uuid())
      .setComponentKey(component.getKey())
      .setComponentName(component.name())
      .setComponentBranchKey(Optional.ofNullable(branch).map(BranchDto::getKey).orElse(null));
    EventPurgeData eventPurgeData = new EventPurgeData(analysis.getComponentUuid(), analysis.getUuid());
    
    dbClient.eventComponentChangeDao().insert(dbSession, eventComponentChange, eventPurgeData);
    db.commit();

    return eventComponentChange;
  }

}
