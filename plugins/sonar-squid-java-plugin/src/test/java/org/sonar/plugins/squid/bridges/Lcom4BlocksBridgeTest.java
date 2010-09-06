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
package org.sonar.plugins.squid.bridges;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmField;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.asm.AsmResource;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class Lcom4BlocksBridgeTest {

  @Test
  public void serialize() throws IOException {
    Set<AsmResource> firstBlock = new HashSet<AsmResource>();
    AsmClass firstClass = new AsmClass("Foo");
    firstBlock.add(new AsmMethod(firstClass, "getActionMessage()V"));
    firstBlock.add(new AsmField(firstClass, "message"));

    Set<AsmResource> secondBlock = new HashSet<AsmResource>();
    AsmClass secondClass = new AsmClass("Bar");
    secondBlock.add(new AsmMethod(secondClass, "getName()Ljava/lang/String;"));
    secondBlock.add(new AsmMethod(secondClass, "setNull(Z)V"));
    secondBlock.add(new AsmField(secondClass, "configured"));

    assertEquals(loadFile("/org/sonar/plugins/squid/bridges/Lcom4BlocksBridgeTest/serialize.json"), new Lcom4BlocksBridge().serialize(Lists.newArrayList(firstBlock, secondBlock)));
  }

  @Test
  public void sortBlocks() {
    Set<AsmResource> bigBlock = new HashSet<AsmResource>();
    AsmClass firstClass = new AsmClass("Foo");
    bigBlock.add(new AsmMethod(firstClass, "getActionMessage()V"));
    bigBlock.add(new AsmField(firstClass, "message"));

    Set<AsmResource> littleBlock = new HashSet<AsmResource>();
    AsmClass secondClass = new AsmClass("Bar");
    littleBlock.add(new AsmMethod(secondClass, "getName()Ljava/lang/String;"));

    List<Set<AsmResource>> blocks = Lists.newArrayList(bigBlock, littleBlock);
    new Lcom4BlocksBridge().sortBlocks(blocks);

    assertThat(blocks.get(0), is(littleBlock));
    assertThat(blocks.get(1), is(bigBlock));
  }

  @Test
  public void sortResourcesInBlock() {
    Set<AsmResource> block = new HashSet<AsmResource>();
    AsmClass aClass = new AsmClass("Foo");
    block.add(new AsmMethod(aClass, "getName()Ljava/lang/String;"));
    block.add(new AsmField(aClass, "message"));
    block.add(new AsmMethod(aClass, "getActionMessage()V"));
    block.add(new AsmField(aClass, "context"));

    List<AsmResource> resources = new Lcom4BlocksBridge().sortResourcesInBlock(block);

    // fields then methods (alphabetical sort)
    assertThat(resources.get(0).toString(), is("context"));
    assertThat(resources.get(1).toString(), is("message"));
    assertThat(resources.get(2).toString(), is("getActionMessage()V"));
    assertThat(resources.get(3).toString(), is("getName()Ljava/lang/String;"));
  }

  private static String loadFile(String path) throws IOException {
    return IOUtils.toString(Lcom4BlocksBridgeTest.class.getResourceAsStream(path));
  }

}
