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
package org.sonar.ce.task.projectanalysis.dbmigration;

import java.util.Objects;
import org.junit.Test;
import org.sonar.core.platform.ComponentContainer;

import static org.assertj.core.api.Assertions.assertThat;

public class DbMigrationModuleTest {
  private DbMigrationModule underTest = new DbMigrationModule();

  @Test
  public void module_configure_ProjectAnalysisDataChanges_implementation() {
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    assertThat(container.getPicoContainer().getComponentAdapters(ProjectAnalysisDataChanges.class))
      .hasSize(1);
  }

  @Test
  public void module_includes_ProjectAnalysisDataChange_classes() {
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    assertThat(ProjectAnalysisDataChangesImpl.getDataChangeClasses()
      .stream()
      .map(t -> container.getPicoContainer().getComponentAdapter(t))
      .filter(Objects::nonNull)).hasSize(ProjectAnalysisDataChangesImpl.getDataChangeClasses().size());
  }
}
