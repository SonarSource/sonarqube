/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.picocontainer.PicoLifecycleException;
import org.sonar.api.platform.ComponentContainer;

import static org.junit.Assert.assertThat;

public class PicoUtilsTest {
  @Test
  public void shouldSanitizePicoLifecycleException() {
    Throwable th = PicoUtils.sanitizeException(newPicoLifecycleException());

    assertThat(th, Is.is(IllegalStateException.class));
    assertThat(th.getMessage(), Is.is("A good reason to fail"));
  }

  @Test
  public void shouldNotSanitizeOtherExceptions() {
    Throwable th = PicoUtils.sanitizeException(new IllegalArgumentException("foo"));

    assertThat(th, Is.is(IllegalArgumentException.class));
    assertThat(th.getMessage(), Is.is("foo"));
  }


  private PicoLifecycleException newPicoLifecycleException() {
    ComponentContainer componentContainer = new ComponentContainer();
    componentContainer.addSingleton(FailureComponent.class);
    try {
      componentContainer.startComponents();
      return null;

    } catch (PicoLifecycleException e) {
      return e;
    }
  }

  public static class FailureComponent {
    public void start() {
      throw new IllegalStateException("A good reason to fail");
    }
  }
}
