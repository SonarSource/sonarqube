/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.resources;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MethodTest {

  @Test
  public void shouldAcceptOverridding() {
    Method method = Method.createMethod("Foo.foo()", Java.INSTANCE);
    Method fakeMethod = new FakeMethod("Foo.foo()");

    assertThat(method.equals(fakeMethod), is(true));
    assertThat(fakeMethod.equals(method), is(true));

    assertThat(method.hashCode(), is(fakeMethod.hashCode()));
    assertThat(fakeMethod.hashCode(), is(method.hashCode()));
  }

  static class FakeMethod extends Method {
    protected FakeMethod(String key) {
      super(key, Qualifiers.METHOD, Java.INSTANCE);
    }
  }
}
