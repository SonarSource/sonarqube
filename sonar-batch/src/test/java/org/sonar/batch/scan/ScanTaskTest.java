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
package org.sonar.batch.scan;

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.batch.ProjectConfigurator;
import org.sonar.batch.bootstrap.TaskContainer;
import org.sonar.batch.phases.Phases;
import org.sonar.core.resource.ResourceDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ScanTaskTest {
  @Test
  public void test_definition() {
    assertThat(ScanTask.DEFINITION).isNotNull();
    assertThat(ScanTask.DEFINITION.key()).isEqualTo(CoreProperties.SCAN_TASK);
  }

  @Test
  public void should_enable_all_phases() {
    ScanTask task = new ScanTask(mock(TaskContainer.class));
    ComponentContainer projectScanContainer = new ComponentContainer();
    projectScanContainer.add(mock(ProjectConfigurator.class), new ProjectReactor(ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "foo")),
      mock(Settings.class),
      mock(ResourceDao.class));
    task.scan(projectScanContainer);

    Phases phases = projectScanContainer.getComponentByType(Phases.class);
    assertThat(phases.isFullyEnabled()).isTrue();
  }
}
