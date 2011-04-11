/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.java.ast.visitor;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.SquidTestUtils;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceMethod;
import org.sonar.squid.indexer.QueryByType;
import org.sonar.squid.measures.Metric;

public class MethodVisitorTest {

  private Squid squid;

  @Before
  public void setup() {
    squid = new Squid(new JavaSquidConfiguration());
  }

  @Test
  public void analyseClassWithStaticMethods() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/methods/ClassWithStaticMethods.java"));
    SourceCode prj = squid.decorateSourceCodeTreeWith(Metric.values());
    assertEquals(3, prj.getInt(Metric.METHODS));
    assertEquals(8, prj.getInt(Metric.COMPLEXITY));
  }

  @Test
  public void methodWithAnonymousInnerClass() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/methods/MethodWithAnonymousInnerClass.java"));
    SourceCode prj = squid.decorateSourceCodeTreeWith(Metric.values());
    assertEquals(4, prj.getInt(Metric.METHODS));
    assertEquals(4, squid.search(new QueryByType(SourceMethod.class)).size());
    assertEquals(4, prj.getInt(Metric.COMPLEXITY));
  }

  @Test
  public void testStartAtLine() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/methods/ClassWithStaticMethods.java"));
    SourceCode doJob2Method = squid.search("ClassWithStaticMethods#doJob1()V");
    assertEquals(3, doJob2Method.getStartAtLine());
  }

  @Test
  public void testMethodSignature() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/methods/ClassWithDifferentMethodSignatures.java"));
    assertNotNull(squid.search("ClassWithDifferentMethodSignatures#<init>()V"));
    assertNotNull(squid.search("ClassWithDifferentMethodSignatures#<init>(LList;)V"));
    assertNotNull(squid.search("ClassWithDifferentMethodSignatures#method()V"));
    assertNotNull(squid.search("ClassWithDifferentMethodSignatures#method(I)[D"));
    assertNotNull(squid.search("ClassWithDifferentMethodSignatures#method(I)LString;"));
    assertNotNull(squid.search("ClassWithDifferentMethodSignatures#method(I[D)[LSquid;"));
    assertNotNull(squid.search("ClassWithDifferentMethodSignatures#method(LString;)V"));
    assertNotNull(squid.search("ClassWithDifferentMethodSignatures#method([LString;)V"));
    assertNotNull(squid.search("ClassWithDifferentMethodSignatures#method(LArrayList;)V"));
    assertNotNull(squid.search("ClassWithDifferentMethodSignatures#method(LEntry;)V"));
    assertNotNull(squid.search("ClassWithDifferentMethodSignatures#method(LMap;)[LSquid;"));
    assertNotNull(squid.search("ClassWithDifferentMethodSignatures#unusedPrivateMethod(LList;[I)V"));
  }

  @Test
  public void testConstructorsMetric() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/methods/ClassWithDifferentMethodSignatures.java"));
    SourceCode source = squid.decorateSourceCodeTreeWith(Metric.values());
    assertEquals(2, source.getInt(Metric.CONSTRUCTORS));
  }

  @Test
  public void detectSuppressWarningsAnnotation() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/rules/ClassWithSuppressWarningsAnnotation.java"));

    assertThat(getMethod("ClassWithSuppressWarningsAnnotation#fullyQualifiedName()V").isSuppressWarnings(), is(true));
    assertThat(getMethod("ClassWithSuppressWarningsAnnotation#singleValue()V").isSuppressWarnings(), is(true));
    assertThat(getMethod("ClassWithSuppressWarningsAnnotation#arrayWithSingleValue()V").isSuppressWarnings(), is(true));
    assertThat(getMethod("ClassWithSuppressWarningsAnnotation#arrayWithMultipleValues()V").isSuppressWarnings(), is(true));
    assertThat(getMethod("ClassWithSuppressWarningsAnnotation$1#methodInAnonymousInnerClass()V").isSuppressWarnings(), is(true));

    assertThat(getMethod("ClassWithSuppressWarningsAnnotation#notHandled()V").isSuppressWarnings(), is(false));
    assertThat(getMethod("ClassWithSuppressWarningsAnnotation#notHandled2()V").isSuppressWarnings(), is(false));
    assertThat(getMethod("ClassWithSuppressWarningsAnnotation#notHandled3()V").isSuppressWarnings(), is(false));
  }

  private SourceMethod getMethod(String key) {
    return (SourceMethod) squid.search(key);
  }

}
