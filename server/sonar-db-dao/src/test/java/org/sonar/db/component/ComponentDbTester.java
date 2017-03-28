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
package org.sonar.db.component;

import java.util.Arrays;
import java.util.function.Consumer;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;

import static java.util.Arrays.asList;
import static org.sonar.db.component.ComponentTesting.newDeveloper;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;

public class ComponentDbTester {
  private final DbTester db;
  private final DbClient dbClient;
  private final DbSession dbSession;

  public ComponentDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  public SnapshotDto insertProjectAndSnapshot(ComponentDto component) {
    return insertComponentAndSnapshot(component);
  }

  public SnapshotDto insertViewAndSnapshot(ComponentDto component) {
    return insertComponentAndSnapshot(component);
  }

  public SnapshotDto insertDeveloperAndSnapshot(ComponentDto component) {
    return insertComponentAndSnapshot(component);
  }

  private SnapshotDto insertComponentAndSnapshot(ComponentDto component) {
    dbClient.componentDao().insert(dbSession, component);
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(component));
    db.commit();

    return snapshot;
  }

  public ComponentDto insertComponent(ComponentDto component) {
    return insertComponentImpl(component, noExtraConfiguration());
  }

  public ComponentDto insertProject() {
    return insertComponentImpl(newProjectDto(db.getDefaultOrganization()), noExtraConfiguration());
  }

  public ComponentDto insertProject(Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(newProjectDto(db.getDefaultOrganization()), dtoPopulators);
  }

  public ComponentDto insertProject(OrganizationDto organizationDto, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(newProjectDto(organizationDto), dtoPopulators);
  }

  public ComponentDto insertProject(OrganizationDto organizationDto) {
    return insertComponentImpl(newProjectDto(organizationDto), noExtraConfiguration());
  }

  public ComponentDto insertProject(OrganizationDto organizationDto, String uuid) {
    return insertComponentImpl(newProjectDto(organizationDto, uuid), noExtraConfiguration());
  }

  public ComponentDto insertView() {
    return insertComponentImpl(newView(db.getDefaultOrganization()), noExtraConfiguration());
  }

  public ComponentDto insertView(Consumer<ComponentDto> dtoPopulator) {
    return insertComponentImpl(newView(db.getDefaultOrganization()), dtoPopulator);
  }

  public ComponentDto insertView(OrganizationDto organizationDto) {
    return insertComponentImpl(newView(organizationDto), noExtraConfiguration());
  }

  public ComponentDto insertView(OrganizationDto organizationDto, Consumer<ComponentDto> dtoPopulator) {
    return insertComponentImpl(newView(organizationDto), dtoPopulator);
  }

  public ComponentDto insertView(String uuid) {
    return insertComponentImpl(newView(db.getDefaultOrganization(), uuid), noExtraConfiguration());
  }

  public ComponentDto insertView(OrganizationDto organizationDto, String uuid) {
    return insertComponentImpl(newView(organizationDto, uuid), noExtraConfiguration());
  }

  public ComponentDto insertDeveloper(String name, Consumer<ComponentDto> dtoPopulator) {
    return insertComponentImpl(newDeveloper(db.getDefaultOrganization(), name), dtoPopulator);
  }

  public ComponentDto insertDeveloper(String name) {
    return insertComponentImpl(newDeveloper(db.getDefaultOrganization(), name), noExtraConfiguration());
  }

  public ComponentDto insertDeveloper(String name, String uuid) {
    return insertComponentImpl(newDeveloper(db.getDefaultOrganization(), name, uuid), noExtraConfiguration());
  }

  private static <T> Consumer<T> noExtraConfiguration() {
    return (t) -> {
    };
  }

  private ComponentDto insertComponentImpl(ComponentDto component, Consumer<ComponentDto>... dtoPopulators) {
    Arrays.stream(dtoPopulators)
      .forEach(dtoPopulator -> dtoPopulator.accept(component));
    dbClient.componentDao().insert(dbSession, component);
    db.commit();

    return component;
  }

  public void insertComponents(ComponentDto... components) {
    dbClient.componentDao().insert(dbSession, asList(components));
    db.commit();
  }

  public SnapshotDto insertSnapshot(SnapshotDto snapshotDto) {
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, snapshotDto);
    db.commit();
    return snapshot;
  }

  public SnapshotDto insertSnapshot(ComponentDto componentDto) {
    return insertSnapshot(componentDto, noExtraConfiguration());
  }

  public SnapshotDto insertSnapshot(ComponentDto componentDto, Consumer<SnapshotDto> consumer) {
    SnapshotDto snapshotDto = SnapshotTesting.newAnalysis(componentDto);
    consumer.accept(snapshotDto);
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, snapshotDto);
    db.commit();
    return snapshot;
  }
}
