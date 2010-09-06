/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.java.signature;

import java.util.List;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ParameterSignatureScannerTest {

  @Test
  public void testScanVoid() {
    Parameter param = ParameterSignatureScanner.scan("V");
    assertThat(param.isVoid(), is(true));
  }

  @Test
  public void testScanObject() {
    Parameter param = ParameterSignatureScanner.scan("Ljava/lang/String;");
    assertThat(param.isOject(), is(true));
    assertThat(param.getClassName(), is("String"));
  }
  
  @Test
  public void testScanSimpleGenericObject() {
    Parameter param = ParameterSignatureScanner.scan("TU;");
    assertThat(param.isOject(), is(true));
    assertThat(param.getClassName(), is("U"));
  }
  
  @Test
  public void testScanComplexGenericObject() {
    Parameter param = ParameterSignatureScanner.scan("TU<TV;Ljava/util/Map$Entry<TY>>;");
    assertThat(param.isOject(), is(true));
    assertThat(param.getClassName(), is("U"));
  }
  
  @Test
  public void testScanInnerClassObject() {
    Parameter param = ParameterSignatureScanner.scan("LMap$Entry;");
    assertThat(param.isOject(), is(true));
    assertThat(param.getClassName(), is("Entry"));
  }

  @Test
  public void testScanPrimitiveType() {
    Parameter param = ParameterSignatureScanner.scan("B");
    assertThat(param.isOject(), is(false));
    assertThat(param.getJvmJavaType(), is(JvmJavaType.B));
  }

  @Test
  public void testScanArray() {
    Parameter param = ParameterSignatureScanner.scan("[B");
    assertThat(param.isArray(), is(true));
    assertThat(param.getJvmJavaType(), is(JvmJavaType.B));

    param = ParameterSignatureScanner.scan("B");
    assertThat(param.isArray(), is(false));
    assertThat(param.getJvmJavaType(), is(JvmJavaType.B));

    param = ParameterSignatureScanner.scan("[LString;");
    assertThat(param.isOject(), is(true));
    assertThat(param.getClassName(), is("String"));
  }
  
  @Test
  public void testScanArrayOfArray() {
    Parameter param = ParameterSignatureScanner.scan("[[[[B");
    assertThat(param.isArray(), is(true));
    assertThat(param.getJvmJavaType(), is(JvmJavaType.B));
  }

  @Test
  public void testScanSeveralPrimitiveArguments() {
    List<Parameter> params = ParameterSignatureScanner.scanArguments("BIZ");
    assertThat(params.size(), is(3));

    Parameter param1 = params.get(0);
    assertThat(param1.isOject(), is(false));
    assertThat(param1.getJvmJavaType(), is(JvmJavaType.B));
  }

  @Test
  public void testScanSeveralComplexArguments() {
    List<Parameter> params = ParameterSignatureScanner.scanArguments("B[LString;IZ");
    assertThat(params.size(), is(4));

    Parameter param1 = params.get(0);
    assertThat(param1.isOject(), is(false));
    assertThat(param1.getJvmJavaType(), is(JvmJavaType.B));

    Parameter param2 = params.get(1);
    assertThat(param2.isOject(), is(true));
    assertThat(param2.getClassName(), is("String"));

    Parameter param3 = params.get(2);
    assertThat(param3.isOject(), is(false));
    assertThat(param3.getJvmJavaType(), is(JvmJavaType.I));
  }
}
