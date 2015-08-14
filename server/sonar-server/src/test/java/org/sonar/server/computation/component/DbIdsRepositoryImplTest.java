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

package org.sonar.server.computation.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class DbIdsRepositoryImplTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  Component component = ReportComponent.DUMB_PROJECT;

  @Test
  public void add_and_get_component_id() {
    DbIdsRepositoryImpl cache = new DbIdsRepositoryImpl();
    cache.setComponentId(component, 10L);

    assertThat(cache.getComponentId(component)).isEqualTo(10L);
  }

  @Test
  public void fail_to_get_component_id_on_unknown_ref() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Component ref '" + component.getReportAttributes().getRef() + "' has no component id");

    new DbIdsRepositoryImpl().getComponentId(ReportComponent.DUMB_PROJECT);
  }

  @Test
  public void fail_if_component_id_already_set() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Component ref '" + component.getReportAttributes().getRef() + "' has already a component id");

    DbIdsRepositoryImpl cache = new DbIdsRepositoryImpl();
    cache.setComponentId(component, 10L);
    cache.setComponentId(component, 11L);
  }

  @Test
  public void add_and_get_snapshot_id() {
    DbIdsRepositoryImpl cache = new DbIdsRepositoryImpl();
    cache.setSnapshotId(component, 100L);

    assertThat(cache.getSnapshotId(component)).isEqualTo(100L);
  }

  @Test
  public void fail_to_get_snapshot_id_on_unknown_ref() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Component ref '" + component.getReportAttributes().getRef() + "' has no snapshot id");

    new DbIdsRepositoryImpl().getSnapshotId(ReportComponent.DUMB_PROJECT);
  }

  @Test
  public void fail_if_snapshot_id_already_set() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Component ref '" + component.getReportAttributes().getRef() + "' has already a snapshot id");

    DbIdsRepositoryImpl cache = new DbIdsRepositoryImpl();
    cache.setSnapshotId(component, 10L);
    cache.setSnapshotId(component, 11L);
  }

}
