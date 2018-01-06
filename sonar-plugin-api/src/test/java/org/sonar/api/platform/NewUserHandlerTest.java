/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.platform;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class NewUserHandlerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void build_context() {
    NewUserHandler.Context context = NewUserHandler.Context.builder().setLogin("marius").setName("Marius").setEmail("marius@lesbronzes.fr").build();

    assertThat(context.getLogin()).isEqualTo("marius");
    assertThat(context.getName()).isEqualTo("Marius");
    assertThat(context.getEmail()).isEqualTo("marius@lesbronzes.fr");
  }

  @Test
  public void login_is_mandatory() {
    thrown.expect(NullPointerException.class);

    NewUserHandler.Context.builder().setName("Marius").build();
  }
}
