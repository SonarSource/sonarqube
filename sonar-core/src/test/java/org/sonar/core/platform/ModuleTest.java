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
package org.sonar.core.platform;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleTest {
  ComponentContainer container = new ComponentContainer();
  int initialSize = sizeOf(container);

  @Test(expected = NullPointerException.class)
  public void configure_throws_NPE_if_container_is_empty() {
    new Module() {
      @Override
      protected void configureModule() {
        // empty
      }
    }.configure(null);
  }

  @Test
  public void module_with_empty_configureModule_method_adds_no_component() {
    new Module() {
      @Override
      protected void configureModule() {
        // empty
      }
    }.configure(container);

    assertThat(sizeOf(container)).isSameAs(initialSize);
  }

  @Test
  public void add_method_supports_null_and_adds_nothing_to_container() {
    new Module() {
      @Override
      protected void configureModule() {
        add(null);
      }
    }.configure(container);

    assertThat(sizeOf(container)).isEqualTo(initialSize);
  }

  @Test
  public void add_method_filters_out_null_inside_vararg_parameter() {
    new Module() {
      @Override
      protected void configureModule() {
        add(new Object(), null, "");
      }
    }.configure(container);

    assertThat(sizeOf(container)).isEqualTo(initialSize + 2);
  }

  private static int sizeOf(ComponentContainer container) {
    return container.getPicoContainer().getComponentAdapters().size();
  }
}
