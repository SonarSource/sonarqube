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

import java.lang.reflect.Field;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.runners.model.FrameworkMethod;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

@Aspect("percflow(execution(void org.junit.internal.runners.statements.RunBefores.evaluate()))")
public class RunBeforesAspect {
  private static final Logger logger = Loggers.get(RunBeforesAspect.class);
  private String testClass = "";
  private String testMethod = "";

  @Pointcut("execution(void org.junit.internal.runners.statements.RunBefores.evaluate())")
  void runBefores() {
  }

  @Pointcut("@annotation(org.junit.Before)")
  void anyBefore() {
  }

  @Pointcut("@annotation(org.junit.BeforeClass)")
  void anyBeforeClass() {
  }

  @Before("runBefores()")
  public void prepareBefore(JoinPoint jp) {
    try {
      Field nextField = RunBefores.class.getDeclaredField("next");
      nextField.setAccessible(true);
      Object invokeMethod = nextField.get(jp.getTarget());
      if (invokeMethod instanceof InvokeMethod) {
        Field testMethodField = InvokeMethod.class.getDeclaredField("testMethod");
        testMethodField.setAccessible(true);
        FrameworkMethod frameworkMethod = (FrameworkMethod) testMethodField.get(invokeMethod);
        testClass = frameworkMethod.getDeclaringClass().getName();
        testMethod = frameworkMethod.getName();
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      logger.error("Error in getting reflection information", e);
    }
  }

  @Before("anyBeforeClass()")
  public void prepareBeforeClass(JoinPoint jp) {
    testClass = jp.getStaticPart().getSignature().getDeclaringType().getName();
  }

  @Around("anyBefore()")
  public Object measureTotalBefore(ProceedingJoinPoint jp) throws Throwable {
    return measureBefore(jp);
  }

  @Around("anyBeforeClass()")
  public Object measureTotalBeforeClass(ProceedingJoinPoint jp) throws Throwable {
    return measureBeforeClass(jp);
  }

  private Object measureBefore(ProceedingJoinPoint jp) throws Throwable {
    return AspectAssistant.measure(jp, measure -> measure
      .setTestClass(testClass)
      .setTestMethod(testMethod)
      .setKind(MeasureKind.BEFORE));
  }

  private Object measureBeforeClass(ProceedingJoinPoint jp) throws Throwable {
    return AspectAssistant.measure(jp, measure -> measure
      .setTestClass(testClass)
      .setKind(MeasureKind.BEFORECLASS));
  }

}
