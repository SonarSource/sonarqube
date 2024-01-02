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
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.runners.model.FrameworkMethod;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

@Aspect("percflow(execution(void org.junit.internal.runners.statements.RunAfters.evaluate()))")
public class RunAftersAspect {
  private static final Logger logger = Loggers.get(RunAftersAspect.class);
  private String testClass = "";
  private String testMethod = "";

  @Pointcut("execution(void org.junit.internal.runners.statements.RunAfters.evaluate())")
  void runAfters() {
  }

  @Pointcut("@annotation(org.junit.After)")
  void anyAfter() {
  }

  @Pointcut("@annotation(org.junit.AfterClass)")
  void anyAfterClass() {
  }

  @Before("runAfters()")
  public void prepareAfter(JoinPoint jp) {
    try {
      Field nextField = RunAfters.class.getDeclaredField("next");
      nextField.setAccessible(true);
      Object next = nextField.get(jp.getTarget());
      if (next instanceof RunBefores) {
        nextField = RunBefores.class.getDeclaredField("next");
        nextField.setAccessible(true);
        next = nextField.get(next);
      }
      if (next instanceof InvokeMethod) {
        Field testMethodField = InvokeMethod.class.getDeclaredField("testMethod");
        testMethodField.setAccessible(true);
        FrameworkMethod frameworkMethod = (FrameworkMethod) testMethodField.get(next);
        testClass = frameworkMethod.getDeclaringClass().getName();
        testMethod = frameworkMethod.getName();
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      logger.error("Error in getting reflection information", e);
    }
  }

  @Before("anyAfterClass()")
  public void prepareAfterClass(JoinPoint jp) {
    testClass = jp.getStaticPart().getSignature().getDeclaringType().getName();
  }

  @Around("anyAfter()")
  public Object measureTotalAfter(ProceedingJoinPoint jp) throws Throwable {
    return measureAfter(jp);
  }

  @Around("anyAfterClass()")
  public Object measureTotalAfterClass(ProceedingJoinPoint jp) throws Throwable {
    return measureAfterClass(jp);
  }

  private Object measureAfter(ProceedingJoinPoint jp) throws Throwable {
    return AspectAssistant.measure(jp, measure -> measure
      .setTestClass(testClass)
      .setTestMethod(testMethod)
      .setKind(MeasureKind.AFTER));
  }

  private Object measureAfterClass(ProceedingJoinPoint jp) throws Throwable {
    return AspectAssistant.measure(jp, measure -> measure
      .setTestClass(testClass)
      .setKind(MeasureKind.AFTERCLASS));
  }
}
