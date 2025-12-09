/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.Arrays;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class ListContainerTest {

  @Test
  public void register_beans() {
    ListContainer container = new ListContainer();
    container.add(
      A.class,
      new VirtualModule(),
      Arrays.asList(C.class, D.class)
    );
    assertThat(container.getAddedObjects()).contains(A.class, B.class, C.class, D.class);
  }

  @Test
  public void addExtension_register_beans() {
    ListContainer container = new ListContainer();
    container
      .addExtension("A", A.class)
      .addExtension("B", B.class);
    assertThat(container.getAddedObjects()).contains(A.class, B.class);
  }

  @Test
  public void declareExtension_does_nothing() {
    ListContainer container = new ListContainer();
    assertThatNoException().isThrownBy(() -> container
      .declareExtension("A", A.class)
      .declareExtension(mock(PluginInfo.class), B.class));
  }

  @Test
  public void unsupported_method_should_throw_exception() {
    ListContainer container = new ListContainer();
    container.add(A.class);
    assertThatThrownBy(() -> container.getComponentByType(A.class)).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> container.getOptionalComponentByType(A.class)).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> container.getComponentsByType(A.class)).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(container::getParent).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void addWebApiV2ConfigurationClass_whenClassIsAdded_isReturnedByGetWebApiV2ConfigurationClasses() {
    ListContainer container = new ListContainer();
    container.addWebApiV2ConfigurationClass(org.sonar.core.test.Test.class);
    assertThat(container.getWebApiV2ConfigurationClasses()).containsOnly(org.sonar.core.test.Test.class);
  }

  class A {
  }

  class B {
  }

  class C {
  }

  class D {
  }

  class VirtualModule extends Module {

    @Override protected void configureModule() {
      add(B.class);
    }
  }

}
