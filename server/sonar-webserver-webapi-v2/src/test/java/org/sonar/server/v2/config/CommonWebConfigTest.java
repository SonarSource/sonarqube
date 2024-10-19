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
package org.sonar.server.v2.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.util.UrlPathHelper;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CommonWebConfigTest {

  @Test
  public void configurePathMatch_shouldDisableUrlDecode() {
    CommonWebConfig commonWebConfig = new CommonWebConfig();
    PathMatchConfigurer pathMatchConfigurer = new PathMatchConfigurer();
    commonWebConfig.configurePathMatch(pathMatchConfigurer);

    UrlPathHelper actualUrlPathHelper = requireNonNull(pathMatchConfigurer.getUrlPathHelper());

    assertThat(actualUrlPathHelper.isUrlDecode()).isFalse();
  }

}
