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
package org.sonar.java.bytecode;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.sonar.java.ast.SquidTestUtils.getFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.bytecode.asm.AsmResource;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceCodeEdgeUsage;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.measures.Metric;

public class BytecodeVisitorsTest {

  static Squid squid;
  static SourceCode todo;
  static SourceCode fixme;
  static SourceCode file;
  static SourceCode tag;
  static SourceCode tagFile;
  static SourceCode line;
  static SourceCode sourceFile;
  static SourceCode language;
  static SourceCode tagName;
  static SourceCode tagException;
  static SourceCode pacTag;
  static SourceCode pacImpl;

  @BeforeClass
  public static void setup() {
    squid = new Squid(new JavaSquidConfiguration());
    squid.register(JavaAstScanner.class).scanDirectory(getFile("/bytecode/src"));
    squid.register(BytecodeScanner.class).scanDirectory(getFile("/bytecode/bin"));
    squid.decorateSourceCodeTreeWith(Metric.values());
    tag = squid.search("tags/Tag");
    tagFile = squid.search("tags/Tag.java");
    file = squid.search("tags/File");
    line = squid.search("tags/Line");
    tagName = squid.search("tags/TagName");
    tagException = squid.search("tags/TagException");
    language = squid.search("tags/Language");
    sourceFile = squid.search("tags/SourceFile");
    todo = squid.search("tags/impl/Todo");
    fixme = squid.search("tags/impl/FixMe");
    pacTag = squid.search("tags");
    pacImpl = squid.search("tags/impl");
  }

  @Test
  public void testLCOM4Visitor() {
    assertEquals(3, squid.search("tags/impl/Todo").getInt(Metric.LCOM4));
    assertEquals(3, squid.search("tags/impl/Todo.java").getInt(Metric.LCOM4));

    List<Set<AsmResource>> lcom4Blocks = (List<Set<AsmResource>>) squid.search("tags/impl/Todo.java").getData(Metric.LCOM4_BLOCKS);
    assertEquals(3, lcom4Blocks.size());

    assertEquals(1, squid.search("tags/Tag").getInt(Metric.LCOM4));
    assertEquals(1, squid.search("tags/TagName").getInt(Metric.LCOM4));
  }

  @Test
  public void testRFCVisitor() {
    assertEquals(9, todo.getInt(Metric.RFC));
    assertEquals(9, squid.search("tags/impl/Todo.java").getInt(Metric.RFC));
    assertEquals(5, sourceFile.getInt(Metric.RFC));
  }

  @Test
  public void testNOCVisitor() {
    assertEquals(1, squid.search("tags/File").getInt(Metric.NOC));
    assertEquals(2, squid.search("tags/Tag").getInt(Metric.NOC)); // Tag is an interface
    assertEquals(0, squid.search("tags/SourceFile").getInt(Metric.NOC));
    assertEquals(2, squid.search("tags/Content").getInt(Metric.NOC)); // Content has only one direct child
  }

  @Test
  public void testDITVisitor() {
    assertEquals(3, squid.search("tags/SourceFile").getInt(Metric.DIT));
    assertEquals(2, squid.search("tags/File").getInt(Metric.DIT));
    assertEquals(1, squid.search("tags/Content").getInt(Metric.DIT));
    assertEquals(3, squid.search("tags/TagException").getInt(Metric.DIT));
  }

  @Test
  public void testDITVisitorOnInterfaces() {
    assertEquals(0, squid.search("tags/Comment").getInt(Metric.DIT));
    assertEquals(1, squid.search("tags/Tag").getInt(Metric.DIT));
  }

  @Test
  public void testExtendsRelationShips() {
    assertEquals(SourceCodeEdgeUsage.EXTENDS, squid.getEdge(sourceFile, file).getUsage());
  }

  @Test
  public void testClassDefinitionWithGenerics() {
    assertEquals(SourceCodeEdgeUsage.USES, squid.getEdge(todo, language).getUsage());
  }

  @Test
  public void testImplementsRelationShips() {
    assertEquals(SourceCodeEdgeUsage.IMPLEMENTS, squid.getEdge(todo, tag).getUsage());
  }

  @Test
  public void testLdcRelationShips() {
    assertEquals(SourceCodeEdgeUsage.USES, squid.getEdge(tagName, tagException).getUsage());
  }

  @Test
  public void testFieldRelationShip() {
    assertEquals(SourceCodeEdgeUsage.USES, squid.getEdge(todo, file).getUsage());
  }

  @Test
  public void testFieldRelationShipWithGenerics() {
    assertEquals(SourceCodeEdgeUsage.USES, squid.getEdge(todo, line).getUsage());
  }

  @Test
  public void testMethodReturnType() {
    assertEquals(SourceCodeEdgeUsage.USES, squid.getEdge(todo, tagName).getUsage());
  }

  @Test
  public void testMethodArgs() {
    assertEquals(SourceCodeEdgeUsage.USES, squid.getEdge(todo, sourceFile).getUsage());
  }

  @Test
  public void testMethodException() {
    assertEquals(SourceCodeEdgeUsage.USES, squid.getEdge(todo, tagException).getUsage());
  }

  @Test
  public void testAccessFieldOfAnObject() {
    assertEquals(SourceCodeEdgeUsage.USES, squid.getEdge(fixme, sourceFile).getUsage());
  }

  @Test
  public void testTypeInsn() {
    assertEquals(SourceCodeEdgeUsage.USES, squid.getEdge(fixme, file).getUsage());
  }

  @Test
  public void testAccessMethodOfAnObject() {
    assertEquals(SourceCodeEdgeUsage.USES, squid.getEdge(fixme, tagException).getUsage());
  }

  @Test
  public void testTryCatchBlock() {
    assertEquals(SourceCodeEdgeUsage.USES, squid.getEdge(sourceFile, tagException).getUsage());
  }

  @Test
  public void testPackageDependencies() {
    assertEquals(SourceCodeEdgeUsage.USES, squid.getEdge(pacImpl, pacTag).getUsage());
    assertEquals(13, squid.getEdge(pacImpl, pacTag).getWeight());
  }

  @Test
  public void noDependencyFromOneSquidUnitToItself() {
    assertNull(squid.getEdge(pacTag, pacTag));
    assertNull(squid.getEdge(fixme, fixme));
  }

  @Test
  public void testFileDependencies() {
    assertEquals(SourceCodeEdgeUsage.USES, squid.getEdge(sourceFile.getParent(), tagException.getParent()).getUsage());
  }

  @Test
  public void testAfferentCouplingAtClassLevel() {
    assertEquals(2, tag.getInt(Metric.CA));
  }

  @Test
  public void testEfferentCouplingAtClassLevel() {
    assertEquals(3, tag.getInt(Metric.CE));
  }

  @Test
  public void testAfferentCouplingAtFileLevel() {
    assertEquals(2, tagFile.getInt(Metric.CA));
  }

  @Test
  public void testEfferentCouplingAtFileLevel() {
    assertEquals(3, tagFile.getInt(Metric.CE));
  }

  @Test
  public void testAfferentCouplingAtPackageLevel() {
    assertEquals(2, pacTag.getInt(Metric.CA));
    assertEquals(0, pacImpl.getInt(Metric.CA));
  }

  @Test
  public void testEfferentCouplingAtPackageLevel() {
    assertEquals(0, pacTag.getInt(Metric.CE));
  }

  @Test
  @Ignore
  public void testClassWithEnum() throws FileNotFoundException, IOException {
    SourceFile classWithEnum = (SourceFile) squid.search("specialCases/ClassWithEnum.java");
    SourceClass myEnum = (SourceClass) squid.search("specialCases/ClassWithEnum$MyEnum");

    ClassReader asmReader = new ClassReader(new FileInputStream(getFile("/bytecode/bin/specialCases/ClassWithEnum$MyEnum.class")));
    TraceClassVisitor classVisitor = new TraceClassVisitor(new PrintWriter(System.out));
    asmReader.accept(classVisitor, 0);
    classVisitor.print(new PrintWriter(System.out));

    assertThat(classWithEnum, is(notNullValue()));
    assertThat(myEnum, is(notNullValue()));
  }
}
