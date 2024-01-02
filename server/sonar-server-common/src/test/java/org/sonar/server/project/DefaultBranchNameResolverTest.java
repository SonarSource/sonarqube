/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.project;

import java.util.Optional;
import org.junit.Test;
import org.sonar.api.config.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.CorePropertyDefinitions.SONAR_PROJECTCREATION_MAINBRANCHNAME;

public class DefaultBranchNameResolverTest {

  @Test
  public void getEffectiveMainBranchName_givenEmptyConfiguration_returnMain() {
    Configuration config = mock(Configuration.class);
    DefaultBranchNameResolver defaultBranchNameResolver = new DefaultBranchNameResolver(config);
    String effectiveMainBranchName = defaultBranchNameResolver.getEffectiveMainBranchName();

    assertThat(effectiveMainBranchName).isEqualTo("main");
  }

  @Test
  public void getEffectiveMainBranchName_givenDevelopInConfiguration_returnDevelop() {
    Configuration config = mock(Configuration.class);
    when(config.get(SONAR_PROJECTCREATION_MAINBRANCHNAME)).thenReturn(Optional.of("develop"));
    DefaultBranchNameResolver defaultBranchNameResolver = new DefaultBranchNameResolver(config);
    String effectiveMainBranchName = defaultBranchNameResolver.getEffectiveMainBranchName();

    assertThat(effectiveMainBranchName).isEqualTo("develop");
  }
}
