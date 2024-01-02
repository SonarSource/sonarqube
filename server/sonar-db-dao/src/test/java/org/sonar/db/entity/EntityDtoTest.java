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
package org.sonar.db.entity;

import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityDtoTest {

  @Test
  public void equals_whenEmptyObjects_shouldReturnTrue() {
    PortfolioDto p1 = new PortfolioDto();
    PortfolioDto p2 = new PortfolioDto();

    boolean equals = p1.equals(p2);

    assertThat(equals).isTrue();
  }

  @Test
  public void equals_whenSameUuid_shouldReturnTrue() {
    PortfolioDto e1 = new PortfolioDto().setUuid("uuid1");
    PortfolioDto e2 = new PortfolioDto().setUuid("uuid1");
    assertThat(e1).isEqualTo(e2);
  }

  @Test
  public void equals_whenDifferentUuid_shouldReturnFalse() {
    PortfolioDto e1 = new PortfolioDto().setUuid("uuid1");
    PortfolioDto e2 = new PortfolioDto().setUuid("uuid2");
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

  @Test
  public void hashCode_whenEmptyObjects_shouldBeTheSame() {
    PortfolioDto p1 = new PortfolioDto();
    PortfolioDto p2 = new PortfolioDto();

    int hash1 = p1.hashCode();
    int hash2 = p2.hashCode();

    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  public void getAuthUuid_whenEntityIsSubportfolio_shouldReturnAuthUuid() {
    PortfolioDto portfolioDto = new PortfolioDto();
    portfolioDto.qualifier = Qualifiers.SUBVIEW;
    portfolioDto.authUuid = "authUuid";
    portfolioDto.setUuid("uuid");

    String authUuid = portfolioDto.getAuthUuid();

    assertThat(authUuid).isEqualTo("authUuid");
  }

  @Test
  public void isProjectOrApp_whenQualifierIsProject_shouldReturnTrue() {
    ProjectDto projectDto = new ProjectDto();
    projectDto.setQualifier(Qualifiers.PROJECT);

    boolean projectOrApp = projectDto.isProjectOrApp();

    assertThat(projectOrApp).isTrue();
  }

  @Test
  public void isProjectOrApp_whenQualifierIsPortfolio_shouldReturnFalse() {
    ProjectDto projectDto = new ProjectDto();
    projectDto.setQualifier(Qualifiers.VIEW);

    boolean projectOrApp = projectDto.isProjectOrApp();

    assertThat(projectOrApp).isFalse();
  }

  @Test
  public void isPortfolio_whenQualifierIsPortfolio_shouldReturnTrue() {
    ProjectDto projectDto = new ProjectDto();
    projectDto.setQualifier(Qualifiers.VIEW);

    boolean projectOrApp = projectDto.isPortfolio();

    assertThat(projectOrApp).isTrue();
  }

  @Test
  public void isPortfolio_whenQualifierIsProject_shouldReturnFalse() {
    ProjectDto projectDto = new ProjectDto();
    projectDto.setQualifier(Qualifiers.PROJECT);

    boolean projectOrApp = projectDto.isPortfolio();

    assertThat(projectOrApp).isFalse();
  }
}