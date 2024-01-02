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
package org.sonarqube.ut.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect("percflow(execution(@org.junit.Test * *(..)))")
public class TestCaseAspect {
  private String testClass = "";
  private String testMethod = "";

  @Pointcut("execution(@org.junit.Test * *(..))")
  void anyTest() {
  }

  @Around("anyTest()")
  public Object measureTestCase(ProceedingJoinPoint jp) throws Throwable {
    return measure(jp);
  }

  @Before("anyTest()")
  public void beforeCall(JoinPoint jp) {
    testClass = jp.getTarget().getClass().getName();
    testMethod = jp.getSignature().getName();
  }

  private Object measure(ProceedingJoinPoint jp) throws Throwable {
    return AspectAssistant.measure(jp, measure -> measure
      .setTestClass(testClass)
      .setTestMethod(testMethod)
      .setKind(MeasureKind.TESTCASE));
  }
}
