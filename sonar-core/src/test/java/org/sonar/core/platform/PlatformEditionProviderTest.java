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
package org.sonar.core.platform;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.platform.EditionProvider.Edition.COMMUNITY;
import static org.sonar.core.platform.EditionProvider.Edition.DATACENTER;
import static org.sonar.core.platform.EditionProvider.Edition.DEVELOPER;

public class PlatformEditionProviderTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void returns_COMMUNITY_when_there_is_no_other_EditionProvider() {
    ComponentContainer container = new ComponentContainer();
    ComponentContainer child = new ComponentContainer(container);
    child.add(PlatformEditionProvider.class);

    assertThat(container.getComponentByType(PlatformEditionProvider.class)).isNull();
    PlatformEditionProvider platformEditionProvider = child.getComponentByType(PlatformEditionProvider.class);
    assertThat(platformEditionProvider.get()).contains(COMMUNITY);
  }

  @Test
  public void returns_edition_from_other_EditionProvider_in_any_container() {
    ComponentContainer container = new ComponentContainer();
    container.add((EditionProvider) () -> of(DATACENTER));
    ComponentContainer child = new ComponentContainer(container);
    child.add(PlatformEditionProvider.class);

    assertThat(container.getComponentByType(PlatformEditionProvider.class)).isNull();
    PlatformEditionProvider platformEditionProvider = child.getComponentByType(PlatformEditionProvider.class);
    assertThat(platformEditionProvider.get()).contains(DATACENTER);
  }

  @Test
  public void get_fails_with_ISE_if_there_is_more_than_one_other_EditionProvider_in_any_container() {
    ComponentContainer container = new ComponentContainer();
    container.add((EditionProvider) () -> of(DATACENTER));
    ComponentContainer child = new ComponentContainer(container);
    child.add((EditionProvider) () -> of(DEVELOPER));
    child.add(PlatformEditionProvider.class);

    assertThat(container.getComponentByType(PlatformEditionProvider.class)).isNull();
    PlatformEditionProvider platformEditionProvider = child.getComponentByType(PlatformEditionProvider.class);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("There can't be more than 1 other EditionProvider");

    platformEditionProvider.get();
  }
}
