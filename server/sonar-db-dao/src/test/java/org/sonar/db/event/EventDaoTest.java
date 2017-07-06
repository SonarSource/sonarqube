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
package org.sonar.db.event;

import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.event.EventTesting.newEvent;

public class EventDaoTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private EventDao underTest = db.getDbClient().eventDao();

  @Test
  public void select_by_uuid() {
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(db.organizations().insert()));
    db.events().insertEvent(newEvent(analysis).setUuid("A1"));
    db.events().insertEvent(newEvent(analysis).setUuid("A2"));
    db.events().insertEvent(newEvent(analysis).setUuid("A3"));

    Optional<EventDto> result = underTest.selectByUuid(dbSession, "A2");

    assertThat(result).isPresent();
    assertThat(result.get().getUuid()).isEqualTo("A2");
  }

  @Test
  public void select_by_component_uuid() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<EventDto> dtos = underTest.selectByComponentUuid(db.getSession(), "ABCD");
    assertThat(dtos).hasSize(3);

    dtos = underTest.selectByComponentUuid(db.getSession(), "BCDE");
    assertThat(dtos).hasSize(1);

    EventDto dto = dtos.get(0);
    assertThat(dto.getId()).isEqualTo(4L);
    assertThat(dto.getUuid()).isEqualTo("E4");
    assertThat(dto.getAnalysisUuid()).isEqualTo("uuid_1");
    assertThat(dto.getComponentUuid()).isEqualTo("BCDE");
    assertThat(dto.getName()).isEqualTo("1.0");
    assertThat(dto.getCategory()).isEqualTo("Version");
    assertThat(dto.getDescription()).isEqualTo("Version 1.0");
    assertThat(dto.getData()).isEqualTo("some data");
    assertThat(dto.getDate()).isEqualTo(1413407091086L);
    assertThat(dto.getCreatedAt()).isEqualTo(1225630680000L);
  }

  @Test
  public void select_by_analysis_uuid() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(project);
    SnapshotDto otherAnalysis = dbClient.snapshotDao().insert(dbSession, newAnalysis(project));
    db.commit();
    db.events().insertEvent(newEvent(analysis).setUuid("A1"));
    db.events().insertEvent(newEvent(otherAnalysis).setUuid("O1"));
    db.events().insertEvent(newEvent(analysis).setUuid("A2"));
    db.events().insertEvent(newEvent(otherAnalysis).setUuid("O2"));
    db.events().insertEvent(newEvent(analysis).setUuid("A3"));
    db.events().insertEvent(newEvent(otherAnalysis).setUuid("O3"));

    List<EventDto> result = underTest.selectByAnalysisUuid(dbSession, analysis.getUuid());

    assertThat(result).hasSize(3);
    assertThat(result).extracting(EventDto::getUuid).containsOnly("A1", "A2", "A3");
  }

  @Test
  public void select_by_analysis_uuids() {
    ComponentDto project = db.components().insertPrivateProject();
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(project));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(project));
    SnapshotDto a42 = db.components().insertSnapshot(newAnalysis(project));
    db.events().insertEvent(newEvent(newAnalysis(project)));
    db.events().insertEvent(newEvent(a1).setUuid("A11"));
    db.events().insertEvent(newEvent(a1).setUuid("A12"));
    db.events().insertEvent(newEvent(a1).setUuid("A13"));
    db.events().insertEvent(newEvent(a2).setUuid("A21"));
    db.events().insertEvent(newEvent(a2).setUuid("A22"));
    db.events().insertEvent(newEvent(a2).setUuid("A23"));
    db.events().insertEvent(newEvent(a42).setUuid("AO1"));
    db.events().insertEvent(newEvent(a42).setUuid("AO2"));
    db.events().insertEvent(newEvent(a42).setUuid("AO3"));

    List<EventDto> result = underTest.selectByAnalysisUuids(dbSession, newArrayList(a1.getUuid(), a2.getUuid()));

    assertThat(result).hasSize(6);
    assertThat(result).extracting(EventDto::getUuid).containsOnly("A11", "A12", "A13", "A21", "A22", "A23");
  }

  @Test
  public void selectByQuery_filters_by_component_uuid() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(project1));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(project2));
    SnapshotDto a3 = db.components().insertSnapshot(newAnalysis(project3));
    EventDto e1 = db.events().insertEvent(newEvent(a1));
    EventDto e2 = db.events().insertEvent(newEvent(a2));
    EventDto e3 = db.events().insertEvent(newEvent(a3));
    EventQuery dbQuery = EventQuery.builder().setComponentUuids(newArrayList(project1.uuid(), project2.uuid())).setFrom(asList(0L, 0L)).build();

    List<EventDto> result = underTest.selectByQuery(db.getSession(), dbQuery);

    assertThat(result)
      .extracting(EventDto::getUuid)
      .containsExactlyInAnyOrder(e1.getUuid(), e2.getUuid());
  }

  @Test
  public void selectByQuery_filters_by_date_per_project() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(project1));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(project2));
    SnapshotDto a3 = db.components().insertSnapshot(newAnalysis(project3));
    long from = 1_500_000_000_000L;
    EventDto e1 = db.events().insertEvent(newEvent(a1).setDate(from));
    EventDto e2 = db.events().insertEvent(newEvent(a2).setDate(from));
    EventDto e3 = db.events().insertEvent(newEvent(a3).setDate(from));
    EventQuery dbQuery = EventQuery.builder()
      .setComponentUuids(newArrayList(project1.uuid(), project2.uuid(), project3.uuid()))
      .setFrom(asList(from - 1L, from, from + 1L)).build();

    List<EventDto> result = underTest.selectByQuery(db.getSession(), dbQuery);

    assertThat(result)
      .extracting(EventDto::getUuid)
      .containsExactlyInAnyOrder(e1.getUuid(), e2.getUuid());
  }

  @Test
  public void selectByQuery_filters_by_functional_date() {
    ComponentDto project = db.components().insertPrivateProject();
    long from = 1_500_000_000_000L;
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(project));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(project));
    SnapshotDto a3 = db.components().insertSnapshot(newAnalysis(project));
    EventDto e1 = db.events().insertEvent(newEvent(a1).setDate(from - 1L));
    EventDto e2 = db.events().insertEvent(newEvent(a2).setDate(from));
    EventDto e3 = db.events().insertEvent(newEvent(a3).setDate(from + 1L));
    EventQuery dbQuery = EventQuery.builder().setComponentUuids(singletonList(project.uuid())).setFrom(singletonList(from)).build();

    List<EventDto> result = underTest.selectByQuery(db.getSession(), dbQuery);

    assertThat(result)
      .extracting(EventDto::getUuid)
      .containsExactlyInAnyOrder(e2.getUuid(), e3.getUuid());
  }

  @Test
  public void selectByQuery_order_by_functional_date_ascending() {
    ComponentDto project = db.components().insertPrivateProject();
    long from = 1_500_000_000_000L;
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(project));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(project));
    SnapshotDto a3 = db.components().insertSnapshot(newAnalysis(project));
    EventDto e1 = db.events().insertEvent(newEvent(a1).setDate(from - 1L));
    EventDto e3 = db.events().insertEvent(newEvent(a3).setDate(from + 1L));
    EventDto e2 = db.events().insertEvent(newEvent(a2).setDate(from));
    EventQuery dbQuery = EventQuery.builder().setComponentUuids(singletonList(project.uuid())).setFrom(singletonList(from - 1L)).build();

    List<EventDto> result = underTest.selectByQuery(db.getSession(), dbQuery);

    assertThat(result)
      .extracting(EventDto::getUuid)
      .containsExactly(e1.getUuid(), e2.getUuid(), e3.getUuid());
  }

  @Test
  public void return_different_categories() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<EventDto> dtos = underTest.selectByComponentUuid(db.getSession(), "ABCD");
    assertThat(dtos).extracting("category").containsOnly(EventDto.CATEGORY_ALERT, EventDto.CATEGORY_PROFILE, EventDto.CATEGORY_VERSION);
  }

  @Test
  public void insert() {
    db.prepareDbUnit(getClass(), "empty.xml");

    underTest.insert(db.getSession(), new EventDto()
      .setUuid("E1")
      .setAnalysisUuid("uuid_1")
      .setComponentUuid("ABCD")
      .setName("1.0")
      .setCategory(EventDto.CATEGORY_VERSION)
      .setDescription("Version 1.0")
      .setData("some data")
      .setDate(1413407091086L)
      .setCreatedAt(1225630680000L));
    db.getSession().commit();

    db.assertDbUnit(getClass(), "insert-result.xml", new String[] {"id"}, "events");
  }

  @Test
  public void update_name_and_description() {
    SnapshotDto analysis = db.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(db.organizations().insert()));
    db.events().insertEvent(newEvent(analysis).setUuid("E1"));

    underTest.update(dbSession, "E1", "New Name", "New Description");

    EventDto result = dbClient.eventDao().selectByUuid(dbSession, "E1").get();
    assertThat(result.getName()).isEqualTo("New Name");
    assertThat(result.getDescription()).isEqualTo("New Description");
  }

  @Test
  public void delete_by_id() {
    db.prepareDbUnit(getClass(), "delete.xml");

    underTest.delete(db.getSession(), 1L);
    db.getSession().commit();

    assertThat(db.countRowsOfTable("events")).isEqualTo(0);
  }

  @Test
  public void delete_by_uuid() {
    db.events().insertEvent(newEvent(newAnalysis(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()))).setUuid("E1"));

    underTest.delete(db.getSession(), "E1");
    db.commit();

    assertThat(db.countRowsOfTable("events")).isEqualTo(0);
  }

}
