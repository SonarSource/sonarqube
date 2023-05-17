/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.db.entity;

import org.junit.Test;
import org.sonar.db.portfolio.PortfolioDto;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityDtoTest {
  @Test
  public void equals_whenSameUuid_shouldReturnTrue() {
    PortfolioDto e1 = new PortfolioDto().setUuid("uuid1");
    PortfolioDto e2 = new PortfolioDto().setUuid("uuid1");;
    assertThat(e1).isEqualTo(e2);
  }

  @Test
  public void equals_whenDifferentUuid_shouldReturnFalse() {
    PortfolioDto e1 = new PortfolioDto().setUuid("uuid1");
    PortfolioDto e2 = new PortfolioDto().setUuid("uuid2");;
    assertThat(e1).isNotEqualTo(e2);
  }

  @Test
  public void equals_whenSameObject_shouldReturnFalse() {
    PortfolioDto e1 = new PortfolioDto().setUuid("uuid1");
    assertThat(e1).isEqualTo(e1);
  }

  @Test
  public void equals_whenDifferentType_shouldReturnFalse() {
    PortfolioDto e1 = new PortfolioDto().setUuid("uuid1");
    assertThat(e1).isNotEqualTo(new Object());
  }

}