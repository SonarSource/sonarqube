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

import java.util.HashSet;
import java.util.Set;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.runtime.reflect.FieldSignatureImpl;

@Aspect("percflow(execution(void org.junit.runners.ParentRunner+.run(..)))")
public class ClassRuleAspect {
  private String testClass = "";
  private Set<String> classRules = new HashSet<>();

  @Pointcut("@annotation(org.junit.ClassRule)")
  void targetAnnotation() {
  }

  @Pointcut("target(org.junit.rules.ExternalResource+) && execution(void before())"
    + " && !cflow(execution(void org.junit.runners.ParentRunner+.runLeaf(..)))")
  void classRuleBefore() {
  }

  @Pointcut("target(org.junit.rules.ExternalResource+) && execution(void after())"
    + " && !cflow(execution(void org.junit.runners.ParentRunner+.runLeaf(..)))")
  void classRuleAfter() {
  }

  @Before("targetAnnotation()")
  public void targetAnnotationCall(JoinPoint jp) {
    testClass = jp.getStaticPart().getSourceLocation().getWithinType().getName();
    if (jp.getStaticPart().getSignature() instanceof FieldSignatureImpl) {
      FieldSignatureImpl fieldSignature = (FieldSignatureImpl) jp.getStaticPart().getSignature();
      classRules.add(fieldSignature.getFieldType().getName());
    }
  }

  @Around("classRuleBefore()")
  public Object classRuleBeforeCall(ProceedingJoinPoint jp) throws Throwable {
    return measure(jp, MeasureKind.CLASS_RULE_BEFORE);
  }

  @Around("classRuleAfter()")
  public Object classRuleAfterCall(ProceedingJoinPoint jp) throws Throwable {
    return measure(jp, MeasureKind.CLASS_RULE_AFTER);
  }

  private Object measure(ProceedingJoinPoint jp, MeasureKind measureKind) throws Throwable {
    String measureClass = jp.getTarget().getClass().getName();
    if (classRules.contains(measureClass)) {
      return AspectAssistant.measure(jp, measure -> measure
        .setTestClass(testClass)
        .setKind(measureKind));
    }
    return jp.proceed();
  }

}
