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

import java.lang.reflect.Method;
import org.junit.Test;
import org.picocontainer.Characteristics;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoLifecycleException;
import org.picocontainer.Startable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class PicoUtilsTest {

  @Test
  public void shouldSanitizePicoLifecycleException() throws NoSuchMethodException {
    UncheckedFailureComponent instance = new UncheckedFailureComponent();
    Method method = UncheckedFailureComponent.class.getMethod("start");
    try {
      instance.start();
      fail("Start should have thrown a IllegalStateException");
    }
    catch (IllegalStateException e) {
      Throwable th = PicoUtils.sanitize(new PicoLifecycleException(method, instance, e));

      assertThat(th).isInstanceOf(IllegalStateException.class);
      assertThat(th.getMessage()).isEqualTo("A good reason to fail");
    }
  }

  @Test
  public void shouldSanitizePicoLifecycleException_no_wrapper_message() {
    Throwable th = PicoUtils.sanitize(new PicoLifecycleException(null, null, new IllegalStateException("msg")));

    assertThat(th).isInstanceOf(IllegalStateException.class);
    assertThat(th.getMessage()).isEqualTo("msg");
  }

  @Test
  public void shouldNotSanitizeOtherExceptions() {
    Throwable th = PicoUtils.sanitize(new IllegalArgumentException("foo"));

    assertThat(th).isInstanceOf(IllegalArgumentException.class);
    assertThat(th.getMessage()).isEqualTo("foo");
  }

  @Test
  public void shouldPropagateInitialUncheckedException() {
    try {
      PicoUtils.propagate(newPicoLifecycleException());
      fail();
    } catch (RuntimeException e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
    }
  }


  private PicoLifecycleException newPicoLifecycleException() {
    MutablePicoContainer container = ComponentContainer.createPicoContainer().as(Characteristics.CACHE);
    container.addComponent(UncheckedFailureComponent.class);
    try {
      container.start();
      throw new IllegalStateException("An exception should have been thrown by start()");

    } catch (PicoLifecycleException e) {
      return e;
    }
  }

  public static class UncheckedFailureComponent implements Startable {
    public void start() {
      throw new IllegalStateException("A good reason to fail");
    }

    @Override
    public void stop() {
      // nothing to do
    }
  }

}
