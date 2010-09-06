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
package org.sonar.java.bytecode.asm;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.signature.SignatureReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsmSignatureVisitorTest {

  private AsmSignatureVisitor visitor;

  @Before
  public void init() {
    visitor = new AsmSignatureVisitor();
  }

  @Test
  public void analyzeFieldSignatureWithGenerics() {
    String signature = "Ljava/util/List<-Ljava/lang/Integer;>;";
    new SignatureReader(signature).accept(visitor);
    assertTrue(visitor.getInternalNames().contains("java/util/List"));
    assertTrue(visitor.getInternalNames().contains("java/lang/Integer"));
    assertEquals(2, visitor.getInternalNames().size());
  }

  @Test
  public void analyseFieldSignatureWithoutGenerics() {
    String signature = "Ljava/util/List;";
    new SignatureReader(signature).accept(visitor);
    assertTrue(visitor.getInternalNames().contains("java/util/List"));
  }

  @Test
  public void analyseFieldSignatureWithoutObjects() {
    String signature = "I";
    new SignatureReader(signature).accept(visitor);
    assertEquals(0, visitor.getInternalNames().size());
  }

  @Test
  public void analyzeMethodSignatureWithGenerics() {
    String signature = "(Ljava/util/List<-Ljava/lang/Integer;>;)Ljava/lang/Number;";
    new SignatureReader(signature).accept(visitor);
    assertTrue(visitor.getInternalNames().contains("java/util/List"));
    assertTrue(visitor.getInternalNames().contains("java/lang/Integer"));
    assertTrue(visitor.getInternalNames().contains("java/lang/Number"));
    assertEquals(3, visitor.getInternalNames().size());
  }

  @Test
  public void analyseMethodSignatureWithoutGenerics() {
    String signature = "([I)Ljava/lang/String;";
    new SignatureReader(signature).accept(visitor);
    assertTrue(visitor.getInternalNames().contains("java/lang/String"));
    assertEquals(1, visitor.getInternalNames().size());
  }

  @Test
  public void analyseMethodSignatureWithoutObjects() {
    String signature = "([B)I";
    new SignatureReader(signature).accept(visitor);
    assertEquals(0, visitor.getInternalNames().size());
  }
}
