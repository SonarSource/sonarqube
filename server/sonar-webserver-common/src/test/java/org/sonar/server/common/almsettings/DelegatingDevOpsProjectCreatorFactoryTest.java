/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.common.almsettings;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.sonar.db.DbSession;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DelegatingDevOpsProjectCreatorFactoryTest {

  private static final DbSession DB_SESSION = mock();
  private static final Map<String, String> CHARACTERISTICS = Map.of("toto", "tata");

  @Test
  public void getDevOpsProjectDescriptor_whenNoDelegates_shouldReturnEmptyOptional() {
    DelegatingDevOpsProjectCreatorFactory noDelegates = new DelegatingDevOpsProjectCreatorFactory(emptySet());
    Optional<DevOpsProjectCreator> devOpsProjectCreator = noDelegates.getDevOpsProjectCreator(DB_SESSION, CHARACTERISTICS);
    assertThat(devOpsProjectCreator).isEmpty();
  }

  @Test
  public void getDevOpsProjectDescriptor_whenNoDelegatesReturningACreator_shouldReturnEmptyOptional() {
    DelegatingDevOpsProjectCreatorFactory delegates = new DelegatingDevOpsProjectCreatorFactory(Set.of(mock(), mock()));
    Optional<DevOpsProjectCreator> devOpsProjectCreator = delegates.getDevOpsProjectCreator(DB_SESSION, CHARACTERISTICS);

    assertThat(devOpsProjectCreator).isEmpty();
  }

  @Test
  public void getDevOpsProjectDescriptor_whenOneDelegatesReturningACreator_shouldDelegate() {
    DevOpsProjectCreatorFactory successfulDelegate = mock();
    DevOpsProjectCreator devOpsProjectCreator = mock();
    when(successfulDelegate.getDevOpsProjectCreator(DB_SESSION, CHARACTERISTICS)).thenReturn(Optional.of(devOpsProjectCreator));
    DelegatingDevOpsProjectCreatorFactory delegates = new DelegatingDevOpsProjectCreatorFactory(Set.of(mock(), successfulDelegate));

    assertThat(delegates.getDevOpsProjectCreator(DB_SESSION, CHARACTERISTICS)).contains(devOpsProjectCreator);
  }

}
