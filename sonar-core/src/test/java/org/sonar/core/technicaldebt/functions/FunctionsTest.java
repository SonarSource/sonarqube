/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.technicaldebt.functions;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class FunctionsTest {

  @Test
  public void register_functions() {
    Functions functions = new Functions(new Function[]{new LinearFunction(null), new LinearWithOffsetFunction(null),
      new ConstantFunction(null)});
    assertThat(functions.getFunction(LinearFunction.FUNCTION_LINEAR)).isInstanceOf(LinearFunction.class);
    assertThat(functions.getFunction(LinearWithOffsetFunction.FUNCTION_LINEAR_WITH_OFFSET)).isInstanceOf(LinearWithOffsetFunction.class);
    assertThat(functions.getFunction(ConstantFunction.FUNCTION_CONSTANT_RESOURCE)).isInstanceOf(ConstantFunction.class);
    assertThat(functions.getFunction("foo")).isNull();
  }
}
