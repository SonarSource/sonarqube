/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.ce.task.projectanalysis.component.Component.Type;
import org.sonar.ce.task.projectanalysis.component.MutableTreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.pushevent.PushEvent;
import org.sonar.ce.task.projectanalysis.pushevent.PushEventRepository;
import org.sonar.ce.task.projectanalysis.pushevent.PushEventRepositoryImpl;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbTester;
import org.sonar.db.pushevent.PushEventDto;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PersistPushEventsStepTest {

  private final TestSystem2 system2 = new TestSystem2().setNow(1L);

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public MutableTreeRootHolderRule treeRootHolder = new MutableTreeRootHolderRule();

  private final PushEventRepository pushEventRepository = new PushEventRepositoryImpl();

  private final PersistPushEventsStep underTest = new PersistPushEventsStep(db.getDbClient(), pushEventRepository, treeRootHolder);

  @Before
  public void before() {
    treeRootHolder.setRoot(ReportComponent.builder(Type.PROJECT, 1)
      .setUuid("uuid_1")
      .build());
  }

  @Test
  public void store_push_events() {
    pushEventRepository.add(new PushEvent<>().setName("event1").setData("data1"));
    pushEventRepository.add(new PushEvent<>().setName("event2").setData("data2"));
    pushEventRepository.add(new PushEvent<>().setName("event3").setData("data3"));

    underTest.execute(mock(ComputationStep.Context.class));

    assertThat(db.countSql(db.getSession(), "SELECT count(uuid) FROM push_events")).isEqualTo(3);
  }

  @Test
  public void event_data_should_be_serialized() {
    system2.setNow(1L);
    pushEventRepository.add(new PushEvent<>().setName("event1").setData(new Data().setEventData("something")));

    underTest.execute(mock(ComputationStep.Context.class));

    assertThat(db.getDbClient().pushEventDao().selectByUuid(db.getSession(), "1"))
      .extracting(PushEventDto::getUuid, PushEventDto::getProjectUuid, PushEventDto::getPayload, PushEventDto::getCreatedAt)
      .containsExactly("1", "uuid_1", "{\"eventData\":\"something\"}".getBytes(UTF_8), 1L);
  }

  @Test
  public void store_push_events_in_batches() {
    IntStream.range(1, 252)
      .forEach(value -> pushEventRepository.add(new PushEvent<>().setName("event" + value).setData("data" + value)));

    underTest.execute(mock(ComputationStep.Context.class));

    assertThat(db.countSql(db.getSession(), "SELECT count(uuid) FROM push_events")).isEqualTo(251);
  }

  @Test
  public void skip_persist_if_no_push_events() {
    underTest.execute(mock(ComputationStep.Context.class));

    assertThat(db.countSql(db.getSession(), "SELECT count(uuid) FROM push_events")).isZero();
  }

  private static class Data {
    private String eventData;

    public Data() {
      // nothing to do
    }

    public String getEventData() {
      return eventData;
    }

    public Data setEventData(String eventData) {
      this.eventData = eventData;
      return this;
    }
  }

}
