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
package org.sonar.ce.user;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.sonar.test.ExceptionCauseMatcher.hasType;

@RunWith(DataProviderRunner.class)
public class CeUserSessionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CeUserSession underTest = new CeUserSession();

  @DataProvider
  public static Object[][] ceUserSessionPublicMethods() {
    List<Method> declaredMethods = Arrays.stream(CeUserSession.class.getDeclaredMethods())
      .filter(m -> Modifier.isPublic(m.getModifiers()))
      .collect(Collectors.toList());
    Object[][] res = new Object[declaredMethods.size()][1];
    int i = 0;
    for (Method declaredMethod : declaredMethods) {
      res[i][0] = declaredMethod;
      i++;
    }
    return res;
  }

  @Test
  @UseDataProvider("ceUserSessionPublicMethods")
  public void all_methods_of_CeUserSession_throw_UOE(Method method) throws InvocationTargetException, IllegalAccessException {
    int parametersCount = method.getParameterTypes().length;
    switch (parametersCount) {
      case 2:
        expectUOE();
        method.invoke(underTest, null, null);
        break;
      case 1:
        expectUOE();
        method.invoke(underTest, (Object) null);
        break;
      case 0:
        expectUOE();
        method.invoke(underTest);
        break;
      default:
        throw new IllegalArgumentException("Unsupported number of parameters " + parametersCount);
    }
  }

  private void expectUOE() {
    expectedException.expect(InvocationTargetException.class);
    expectedException.expectCause(
      hasType(UnsupportedOperationException.class)
        .andMessage("UserSession must not be used from within the Compute Engine"));
  }
}
