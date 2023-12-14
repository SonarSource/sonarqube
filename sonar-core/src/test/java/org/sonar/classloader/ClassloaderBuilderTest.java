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
package org.sonar.classloader;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ClassloaderBuilderTest {

  ClassloaderBuilder sut = new ClassloaderBuilder();

  @Test
  public void minimal_system_classloader() throws Exception {
    // create a classloader based on system classloader
    // -> access only to JRE
    Map<String, ClassLoader> classloaders = sut.newClassloader("example").build();

    assertThat(classloaders).hasSize(1);
    ClassLoader classloader = classloaders.get("example");
    assertThat(classloader).hasToString("ClassRealm{example}");
    assertThat(canLoadClass(classloader, HashMap.class.getName())).isTrue();
    assertThat(canLoadClass(classloader, Test.class.getName())).isFalse();
    assertThat(canLoadClass(classloader, "A")).isFalse();
    assertThat(canLoadResource(classloader, "a.txt")).isFalse();
  }

  @Test
  public void previous_classloader_not_returned_again() throws Exception {
    Map<String, ClassLoader> classloaders1 = sut.newClassloader("example1").build();
    Map<String, ClassLoader> classloaders2 = new ClassloaderBuilder(classloaders1.values())
      .newClassloader("example2").build();

    assertThat(classloaders2).containsOnlyKeys("example2");
  }

  @Test
  public void fail_if_setting_attribute_to_previously_loaded_classloader() throws Exception {
    Map<String, ClassLoader> classloaders1 = sut.newClassloader("example1").build();
    ClassloaderBuilder builder = new ClassloaderBuilder(classloaders1.values())
      .newClassloader("example2");

    try {
      builder.setMask("example1", Mask.ALL);
      fail();
    } catch (IllegalStateException e) {
      // ok
    }
  }

  /**
   * Classloader based on another one (the junit env in this example). No parent-child hierarchy.
   */
  @Test
  public void base_classloader() throws Exception {
    //
    Map<String, ClassLoader> classloaders = sut.newClassloader("example", getClass().getClassLoader()).build();

    assertThat(classloaders).hasSize(1);
    ClassLoader classloader = classloaders.get("example");
    assertThat(canLoadClass(classloader, HashMap.class.getName())).isTrue();
    assertThat(canLoadClass(classloader, Test.class.getName())).isTrue();
    assertThat(canLoadClass(classloader, "A")).isFalse();
    assertThat(canLoadResource(classloader, "a.txt")).isFalse();
  }

  @Test
  public void classloader_constituents() throws Exception {
    Map<String, ClassLoader> classloaders = sut
      .newClassloader("the-cl")
      .addURL("the-cl", new File("tester/a.jar").toURL())
      .addURL("the-cl", new File("tester/b.jar").toURL())
      .build();

    assertThat(classloaders).hasSize(1);
    ClassLoader self = classloaders.get("the-cl");
    assertThat(canLoadClass(self, "A")).isTrue();
    assertThat(canLoadResource(self, "a.txt")).isTrue();
    assertThat(canLoadClass(self, "B")).isTrue();
    assertThat(canLoadResource(self, "b.txt")).isTrue();
    assertThat(canLoadClass(self, "C")).isFalse();
    assertThat(canLoadResource(self, "c.txt")).isFalse();
  }

  /**
   * Parent -> child -> grand-child classloaders. Default order strategy is parent-first
   */
  @Test
  public void parent_child_relation() throws Exception {
    // parent contains class A -> access to only A
    // child contains class B -> access to A and B
    // grand-child contains class C -> access to A, B and C
    Map<String, ClassLoader> classloaders = sut
      .newClassloader("the-parent")
      .addURL("the-parent", new File("tester/a.jar").toURL())

      // order of declaration is not important -> declare grand-child before child
      .newClassloader("the-grand-child")
      .addURL("the-grand-child", new File("tester/c.jar").toURL())
      .setParent("the-grand-child", "the-child", Mask.ALL)

      .newClassloader("the-child")
      .addURL("the-child", new File("tester/b.jar").toURL())
      .setParent("the-child", "the-parent", Mask.ALL)

      .build();

    assertThat(classloaders).hasSize(3);

    ClassLoader parent = classloaders.get("the-parent");
    assertThat(canLoadClass(parent, "A")).isTrue();
    assertThat(canLoadClass(parent, "B")).isFalse();
    assertThat(canLoadClass(parent, "C")).isFalse();
    assertThat(canLoadResource(parent, "a.txt")).isTrue();
    assertThat(canLoadResource(parent, "b.txt")).isFalse();
    assertThat(canLoadResource(parent, "c.txt")).isFalse();

    ClassLoader child = classloaders.get("the-child");
    assertThat(canLoadClass(child, "A")).isTrue();
    assertThat(canLoadClass(child, "B")).isTrue();
    assertThat(canLoadClass(child, "C")).isFalse();
    assertThat(canLoadResource(child, "a.txt")).isTrue();
    assertThat(canLoadResource(child, "b.txt")).isTrue();
    assertThat(canLoadResource(child, "c.txt")).isFalse();

    ClassLoader grandChild = classloaders.get("the-grand-child");
    assertThat(canLoadClass(grandChild, "A")).isTrue();
    assertThat(canLoadClass(grandChild, "B")).isTrue();
    assertThat(canLoadClass(grandChild, "C")).isTrue();
    assertThat(canLoadResource(grandChild, "a.txt")).isTrue();
    assertThat(canLoadResource(grandChild, "b.txt")).isTrue();
    assertThat(canLoadResource(grandChild, "c.txt")).isTrue();
  }

  /**
   * Parent classloader can be created outside {@link ClassloaderBuilder}.
   * Default ordering strategy is parent-first.
   */
  @Test
  public void existing_parent() throws Exception {
    // parent contains JUnit
    // child contains class A -> access to A and JUnit
    ClassLoader parent = getClass().getClassLoader();
    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("the-child")
      .addURL("the-child", new File("tester/a.jar").toURL())
      .setParent("the-child", parent, Mask.ALL)
      .build();

    assertThat(newClassloaders).hasSize(1);
    assertThat(canLoadClass(parent, Test.class.getName())).isTrue();
    assertThat(canLoadClass(parent, "A")).isFalse();
    ClassLoader child = newClassloaders.get("the-child");
    assertThat(canLoadClass(child, Test.class.getName())).isTrue();
    assertThat(canLoadClass(child, "A")).isTrue();
  }

  @Test
  public void parent_first_ordering() throws Exception {
    // parent contains version 1 of A
    // child contains version 2 of A
    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("the-parent")
      .addURL("the-parent", new File("tester/a.jar").toURL())

      .newClassloader("the-child")
      .addURL("the-child", new File("tester/a_v2.jar").toURL())
      .setParent("the-child", "the-parent", Mask.ALL)
      .build();

    ClassLoader parent = newClassloaders.get("the-parent");
    assertThat(canLoadMethod(parent, "A", "version1")).isTrue();
    assertThat(canLoadMethod(parent, "A", "version2")).isFalse();
    assertThat(IOUtils.toString(parent.getResource("a.txt"))).startsWith("version 1 of a.txt");

    ClassLoader child = newClassloaders.get("the-child");
    assertThat(canLoadMethod(child, "A", "version1")).isTrue();
    assertThat(canLoadMethod(child, "A", "version2")).isFalse();
    assertThat(IOUtils.toString(child.getResource("a.txt"))).startsWith("version 1 of a.txt");
  }

  /**
   * - parent contains B and version 1 of A
   * - child contains version 2 of A -> sees B and version 2 of A
   */
  @Test
  public void self_first_ordering() throws Exception {

    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("the-parent")
      .addURL("the-parent", new File("tester/a.jar").toURL())
      .addURL("the-parent", new File("tester/b.jar").toURL())

      .newClassloader("the-child")
      .addURL("the-child", new File("tester/a_v2.jar").toURL())
      .setParent("the-child", "the-parent", Mask.ALL)
      .setLoadingOrder("the-child", ClassloaderBuilder.LoadingOrder.SELF_FIRST)
      .build();

    ClassLoader parent = newClassloaders.get("the-parent");
    assertThat(canLoadMethod(parent, "A", "version1")).isTrue();
    assertThat(canLoadMethod(parent, "A", "version2")).isFalse();
    assertThat(IOUtils.toString(parent.getResource("a.txt"))).startsWith("version 1 of a.txt");

    ClassLoader child = newClassloaders.get("the-child");
    assertThat(canLoadClass(child, "B")).isTrue();
    assertThat(canLoadMethod(child, "A", "version1")).isFalse();
    assertThat(canLoadMethod(child, "A", "version2")).isTrue();
    assertThat(IOUtils.toString(child.getResource("a.txt"))).startsWith("version 2 of a.txt");
    assertThat(Collections.list(child.getResources("b.txt"))).hasSize(1);
    ArrayList<URL> resources = Collections.list(child.getResources("a.txt"));
    assertThat(resources).hasSize(2);
    assertThat(IOUtils.toString(resources.get(0))).startsWith("version 2 of a.txt");
    assertThat(IOUtils.toString(resources.get(1))).startsWith("version 1 of a.txt");
  }

  /**
   * Prevent a classloader from loading some resources that are available in its own constituents.
   */
  @Test
  public void self_mask() throws Exception {
    Map<String, ClassLoader> classloaders = sut
      .newClassloader("the-cl")
      .addURL("the-cl", new File("tester/a.jar").toURL())
      .addURL("the-cl", new File("tester/b.jar").toURL())
      .setMask("the-cl", Mask.builder().exclude("A.class", "a.txt").build())
      .build();

    ClassLoader cl = classloaders.get("the-cl");
    assertThat(canLoadClass(cl, "A")).isFalse();
    assertThat(canLoadClass(cl, "B")).isTrue();
    assertThat(canLoadResource(cl, "a.txt")).isFalse();
    assertThat(canLoadResource(cl, "b.txt")).isTrue();
  }

  /**
   * Partial inheritance of parent classloader
   */
  @Test
  public void parent_mask() throws Exception {
    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("the-parent")
      .addURL("the-parent", new File("tester/a.jar").toURL())
      .addURL("the-parent", new File("tester/b.jar").toURL())

      .newClassloader("the-child")
      .addURL("the-child", new File("tester/c.jar").toURL())
      .setParent("the-child", "the-parent", Mask.builder().exclude("A.class", "a.txt").build())
      .build();

    ClassLoader parent = newClassloaders.get("the-parent");
    assertThat(canLoadClass(parent, "A")).isTrue();
    assertThat(canLoadClass(parent, "B")).isTrue();
    assertThat(canLoadClass(parent, "C")).isFalse();
    assertThat(canLoadResource(parent, "a.txt")).isTrue();
    assertThat(canLoadResource(parent, "b.txt")).isTrue();
    assertThat(canLoadResource(parent, "c.txt")).isFalse();

    ClassLoader child = newClassloaders.get("the-child");
    assertThat(canLoadClass(child, "A")).isFalse();
    assertThat(canLoadClass(child, "B")).isTrue();
    assertThat(canLoadClass(child, "C")).isTrue();
    assertThat(canLoadResource(child, "a.txt")).isFalse();
    assertThat(canLoadResource(child, "b.txt")).isTrue();
    assertThat(canLoadResource(child, "c.txt")).isTrue();
  }

  /**
   * Parent classloader contains A and B, but exports only B to its children
   */
  @Test
  public void export_mask() throws Exception {
    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("the-parent")
      .addURL("the-parent", new File("tester/a.jar").toURL())
      .addURL("the-parent", new File("tester/b.jar").toURL())
      .setExportMask("the-parent", Mask.builder().exclude("A.class", "a.txt").build())

      .newClassloader("the-child")
      .setParent("the-child", "the-parent", Mask.ALL)
      .build();

    ClassLoader parent = newClassloaders.get("the-parent");
    assertThat(canLoadClass(parent, "A")).isTrue();
    assertThat(canLoadClass(parent, "B")).isTrue();
    assertThat(canLoadResource(parent, "a.txt")).isTrue();
    assertThat(canLoadResource(parent, "b.txt")).isTrue();

    ClassLoader child = newClassloaders.get("the-child");
    assertThat(canLoadClass(child, "A")).isFalse();
    assertThat(canLoadClass(child, "B")).isTrue();
    assertThat(canLoadResource(child, "a.txt")).isFalse();
    assertThat(canLoadResource(child, "b.txt")).isTrue();
  }

  /**
   * Parent classloader contains A, B and C, but exports only B and C to its children.
   * On the other side child classloader excludes B from its parent, so it benefits
   * only from C
   */
  @Test
  public void mix_of_import_and_export_masks() throws Exception {
    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("the-parent")
      .addURL("the-parent", new File("tester/a.jar").toURL())
      .addURL("the-parent", new File("tester/b.jar").toURL())
      .addURL("the-parent", new File("tester/c.jar").toURL())
      .setExportMask("the-parent", Mask.builder().exclude("A.class", "a.txt").build())

      .newClassloader("the-child")
      .setParent("the-child", "the-parent", Mask.builder().exclude("B.class", "b.txt").build())
      .build();

    ClassLoader parent = newClassloaders.get("the-parent");
    assertThat(canLoadClass(parent, "A")).isTrue();
    assertThat(canLoadClass(parent, "B")).isTrue();
    assertThat(canLoadClass(parent, "C")).isTrue();
    assertThat(canLoadResource(parent, "a.txt")).isTrue();
    assertThat(canLoadResource(parent, "b.txt")).isTrue();
    assertThat(canLoadResource(parent, "c.txt")).isTrue();

    ClassLoader child = newClassloaders.get("the-child");
    assertThat(canLoadClass(child, "A")).isFalse();
    assertThat(canLoadClass(child, "B")).isFalse();
    assertThat(canLoadClass(child, "C")).isTrue();
    assertThat(canLoadResource(child, "a.txt")).isFalse();
    assertThat(canLoadResource(child, "b.txt")).isFalse();
    assertThat(canLoadResource(child, "c.txt")).isTrue();
  }

  @Test
  public void fail_to_create_the_same_classloader_twice() throws Exception {
    sut.newClassloader("the-cl");
    try {
      sut.newClassloader("the-cl");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("The classloader 'the-cl' already exists. Can not create it twice.");
    }
  }

  @Test
  public void fail_to_create_the_same_previous_classloader_twice() throws Exception {
    Map<String, ClassLoader> classloaders1 = sut.newClassloader("the-cl").build();
    ClassloaderBuilder classloaderBuilder = new ClassloaderBuilder(classloaders1.values());
    try {
      classloaderBuilder.newClassloader("the-cl");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("The classloader 'the-cl' already exists in the list of previously created classloaders. " +
        "Can not create it twice.");
    }
  }

  @Test
  public void fail_if_missing_declaration() throws Exception {
    sut.newClassloader("the-cl");
    sut.setParent("the-cl", "missing", Mask.ALL);
    try {
      sut.build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("The classloader 'missing' does not exist");
    }
  }

  @Test
  public void sibling() throws Exception {
    // sibling1 contains A
    // sibling2 contains B
    // child contains C -> see A, B and C
    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("sib1")
      .addURL("sib1", new File("tester/a.jar").toURL())

      .newClassloader("sib2")
      .addURL("sib2", new File("tester/b.jar").toURL())

      .newClassloader("the-child")
      .addURL("the-child", new File("tester/c.jar").toURL())
      .addSibling("the-child", "sib1", Mask.ALL)
      .addSibling("the-child", "sib2", Mask.ALL)
      .build();

    ClassLoader sib1 = newClassloaders.get("sib1");
    assertThat(canLoadClass(sib1, "A")).isTrue();
    assertThat(canLoadClass(sib1, "B")).isFalse();
    assertThat(canLoadClass(sib1, "C")).isFalse();
    assertThat(canLoadResource(sib1, "a.txt")).isTrue();
    assertThat(canLoadResource(sib1, "b.txt")).isFalse();
    assertThat(canLoadResource(sib1, "c.txt")).isFalse();

    ClassLoader sib2 = newClassloaders.get("sib2");
    assertThat(canLoadClass(sib2, "A")).isFalse();
    assertThat(canLoadClass(sib2, "B")).isTrue();
    assertThat(canLoadClass(sib2, "C")).isFalse();
    assertThat(canLoadResource(sib2, "a.txt")).isFalse();
    assertThat(canLoadResource(sib2, "b.txt")).isTrue();
    assertThat(canLoadResource(sib2, "c.txt")).isFalse();

    ClassLoader child = newClassloaders.get("the-child");
    assertThat(canLoadClass(child, "A")).isTrue();
    assertThat(canLoadClass(child, "B")).isTrue();
    assertThat(canLoadClass(child, "C")).isTrue();
    assertThat(canLoadResource(child, "a.txt")).isTrue();
    assertThat(canLoadResource(child, "b.txt")).isTrue();
    assertThat(canLoadResource(child, "c.txt")).isTrue();
  }

  /**
   * Sibling classloader can be created outside {@link ClassloaderBuilder}.
   */
  @Test
  public void existing_sibling() throws Exception {
    // sibling1 contains JUnit
    // child contains A -> see JUnit and A
    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("the-child")
      .addURL("the-child", new File("tester/a.jar").toURL())
      .addSibling("the-child", getClass().getClassLoader(), Mask.ALL)
      .build();

    ClassLoader child = newClassloaders.get("the-child");
    assertThat(canLoadClass(child, Test.class.getName())).isTrue();
    assertThat(canLoadClass(child, "A")).isTrue();
  }

  /**
   * - sibling contains A and B
   * - child contains C and excludes A from sibling -> sees only B and C
   */
  @Test
  public void sibling_mask() throws Exception {
    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("sib1")
      .addURL("sib1", new File("tester/a.jar").toURL())
      .addURL("sib1", new File("tester/b.jar").toURL())

      .newClassloader("the-child")
      .addURL("the-child", new File("tester/c.jar").toURL())
      .addSibling("the-child", "sib1", Mask.builder().exclude("A.class", "a.txt").build())
      .build();

    ClassLoader sib1 = newClassloaders.get("sib1");
    assertThat(canLoadClass(sib1, "A")).isTrue();
    assertThat(canLoadClass(sib1, "B")).isTrue();
    assertThat(canLoadResource(sib1, "a.txt")).isTrue();
    assertThat(canLoadResource(sib1, "b.txt")).isTrue();

    ClassLoader child = newClassloaders.get("the-child");
    assertThat(canLoadClass(child, "A")).isFalse();
    assertThat(canLoadClass(child, "B")).isTrue();
    assertThat(canLoadClass(child, "C")).isTrue();
    assertThat(canLoadResource(child, "a.txt")).isFalse();
    assertThat(canLoadResource(child, "b.txt")).isTrue();
    assertThat(canLoadResource(child, "c.txt")).isTrue();
    assertThat(Collections.list(child.getResources("a.txt"))).isEmpty();
    assertThat(Collections.list(child.getResources("b.txt"))).hasSize(1);
    assertThat(Collections.list(child.getResources("c.txt"))).hasSize(1);
  }

  /**
   * - sibling contains A and B but exports only B
   * - child contains C -> sees only B and C
   */
  @Test
  public void sibling_export_mask() throws Exception {
    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("sib1")
      .addURL("sib1", new File("tester/a.jar").toURL())
      .addURL("sib1", new File("tester/b.jar").toURL())
      .setExportMask("sib1", Mask.builder().include("B.class", "b.txt").build())

      .newClassloader("the-child")
      .addURL("the-child", new File("tester/c.jar").toURL())
      .addSibling("the-child", "sib1", Mask.ALL)
      .build();

    ClassLoader sib1 = newClassloaders.get("sib1");
    assertThat(canLoadClass(sib1, "A")).isTrue();
    assertThat(canLoadClass(sib1, "B")).isTrue();
    assertThat(canLoadResource(sib1, "a.txt")).isTrue();
    assertThat(canLoadResource(sib1, "b.txt")).isTrue();

    ClassLoader child = newClassloaders.get("the-child");
    assertThat(canLoadClass(child, "A")).isFalse();
    assertThat(canLoadClass(child, "B")).isTrue();
    assertThat(canLoadClass(child, "C")).isTrue();
    assertThat(canLoadResource(child, "a.txt")).isFalse();
    assertThat(canLoadResource(child, "b.txt")).isTrue();
    assertThat(canLoadResource(child, "c.txt")).isTrue();
    assertThat(Collections.list(child.getResources("a.txt"))).isEmpty();
    assertThat(Collections.list(child.getResources("b.txt"))).hasSize(1);
    assertThat(Collections.list(child.getResources("c.txt"))).hasSize(1);
  }

  /**
   * Sibling classloader is loaded previously self:
   * - sibling has version 1 of A
   * - self has version 2 of A -> sees version 1
   */
  @Test
  public void sibling_prevails_over_self() throws Exception {
    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("sib")
      .addURL("sib", new File("tester/a.jar").toURL())

      .newClassloader("self")
      .addURL("self", new File("tester/a_v2.jar").toURL())
      .addSibling("self", "sib", Mask.ALL)
      .build();

    ClassLoader sib = newClassloaders.get("sib");
    assertThat(canLoadMethod(sib, "A", "version1")).isTrue();
    assertThat(canLoadMethod(sib, "A", "version2")).isFalse();
    assertThat(IOUtils.toString(sib.getResource("a.txt"))).startsWith("version 1 of a.txt");

    ClassLoader self = newClassloaders.get("self");
    assertThat(canLoadMethod(self, "A", "version1")).isTrue();
    assertThat(canLoadMethod(self, "A", "version2")).isFalse();
    assertThat(IOUtils.toString(self.getResource("a.txt"))).startsWith("version 1 of a.txt");
  }

  /**
   * Sibling classloader is always loaded previously self, even if self-first strategy:
   * - sibling has version 1 of A
   * - self has version 2 of A -> sees version 1
   */
  @Test
  public void sibling_prevails_over_self_even_if_self_first() throws Exception {
    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("sib")
      .addURL("sib", new File("tester/a.jar").toURL())

      .newClassloader("self")
      .addURL("self", new File("tester/a_v2.jar").toURL())
      .addSibling("self", "sib", Mask.ALL)
      .setLoadingOrder("self", ClassloaderBuilder.LoadingOrder.SELF_FIRST)
      .build();

    ClassLoader sib = newClassloaders.get("sib");
    assertThat(canLoadMethod(sib, "A", "version1")).isTrue();
    assertThat(canLoadMethod(sib, "A", "version2")).isFalse();
    assertThat(IOUtils.toString(sib.getResource("a.txt"))).startsWith("version 1 of a.txt");

    ClassLoader self = newClassloaders.get("self");
    assertThat(canLoadMethod(self, "A", "version1")).isTrue();
    assertThat(canLoadMethod(self, "A", "version2")).isFalse();
    assertThat(IOUtils.toString(self.getResource("a.txt"))).startsWith("version 1 of a.txt");
  }

  /**
   * https://github.com/SonarSource/sonar-classloader/issues/1
   */
  @Test
  public void cycle_of_siblings() throws Exception {
    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("a")
      .addURL("a", new File("tester/a.jar").toURL())

      .newClassloader("b")
      .addURL("b", new File("tester/b.jar").toURL())
      .addSibling("a", "b", Mask.builder().include("B.class", "b.txt").build())
      .addSibling("b", "a", Mask.builder().include("A.class", "a.txt").build())
      .build();

    ClassLoader a = newClassloaders.get("a");
    assertThat(canLoadClass(a, "A")).isTrue();
    assertThat(canLoadClass(a, "B")).isTrue();
    assertThat(IOUtils.toString(a.getResource("a.txt"))).isNotEmpty();
    assertThat(IOUtils.toString(a.getResource("b.txt"))).isNotEmpty();

    ClassLoader b = newClassloaders.get("b");
    assertThat(canLoadClass(b, "A")).isTrue();
    assertThat(canLoadClass(b, "B")).isTrue();
    assertThat(IOUtils.toString(b.getResource("a.txt"))).isNotEmpty();
    assertThat(IOUtils.toString(b.getResource("b.txt"))).isNotEmpty();
  }

  @Test
  public void getResources_from_parent_and_siblings() throws Exception {
    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("the-parent")
      .addURL("the-parent", new File("tester/a.jar").toURL())

      .newClassloader("the-sib")
      .addURL("the-sib", new File("tester/b.jar").toURL())

      .newClassloader("the-child")
      .addURL("the-child", new File("tester/c.jar").toURL())
      .setParent("the-child", "the-parent", Mask.ALL)
      .addSibling("the-child", "the-sib", Mask.ALL)
      .build();

    ClassLoader parent = newClassloaders.get("the-parent");
    assertThat(Collections.list(parent.getResources("a.txt"))).hasSize(1);
    assertThat(Collections.list(parent.getResources("b.txt"))).isEmpty();
    assertThat(Collections.list(parent.getResources("c.txt"))).isEmpty();

    ClassLoader child = newClassloaders.get("the-child");
    assertThat(Collections.list(child.getResources("a.txt"))).hasSize(1);
    assertThat(Collections.list(child.getResources("b.txt"))).hasSize(1);
    assertThat(Collections.list(child.getResources("c.txt"))).hasSize(1);
  }

  @Test
  public void getResources_from_previously_loaded_parent() throws Exception {
    Map<String, ClassLoader> classloaders1 = sut
      .newClassloader("the-parent")
      .addURL("the-parent", new File("tester/a.jar").toURL())
      .build();


    Map<String, ClassLoader> classloaders2 = new ClassloaderBuilder(classloaders1.values())
      .newClassloader("the-child")
      .addURL("the-child", new File("tester/b.jar").toURL())
      .setParent("the-child", "the-parent", Mask.ALL)
      .build();

    ClassLoader parent = classloaders1.get("the-parent");
    assertThat(Collections.list(parent.getResources("a.txt"))).hasSize(1);
    assertThat(Collections.list(parent.getResources("b.txt"))).isEmpty();

    ClassLoader child = classloaders2.get("the-child");
    assertThat(Collections.list(child.getResources("a.txt"))).hasSize(1);
    assertThat(Collections.list(child.getResources("b.txt"))).hasSize(1);
  }

  @Test
  public void getResources_from_previously_loaded_sibling_based_on_export_mask() throws Exception {
    Map<String, ClassLoader> classloaders1 = sut
      .newClassloader("the-sib")
      .addURL("the-sib", new File("tester/a.jar").toURL())
      .setExportMask("the-sib", Mask.builder().include("A.java").build())
      .build();

    Map<String, ClassLoader> classloaders2 = new ClassloaderBuilder(classloaders1.values())
      .newClassloader("the-child")
      .addURL("the-child", new File("tester/b.jar").toURL())
      .addSibling("the-child", "the-sib", Mask.ALL)
      .build();

    ClassLoader parent = classloaders1.get("the-sib");
    assertThat(Collections.list(parent.getResources("a.txt"))).hasSize(1);
    assertThat(Collections.list(parent.getResources("A.java"))).hasSize(1);
    assertThat(Collections.list(parent.getResources("b.txt"))).isEmpty();

    ClassLoader child = classloaders2.get("the-child");
    assertThat(Collections.list(child.getResources("a.txt"))).isEmpty();
    assertThat(Collections.list(parent.getResources("A.java"))).hasSize(1);
    assertThat(Collections.list(child.getResources("b.txt"))).hasSize(1);
  }

  @Test
  public void getResources_from_previously_loaded_sibling() throws Exception {
    Map<String, ClassLoader> classloaders1 = sut
      .newClassloader("the-sib")
      .addURL("the-sib", new File("tester/a.jar").toURL())
      .build();

    Map<String, ClassLoader> classloaders2 = new ClassloaderBuilder(classloaders1.values())
      .newClassloader("the-child")
      .addURL("the-child", new File("tester/b.jar").toURL())
      .addSibling("the-child", "the-sib", Mask.ALL)
      .build();

    ClassLoader parent = classloaders1.get("the-sib");
    assertThat(Collections.list(parent.getResources("a.txt"))).hasSize(1);
    assertThat(Collections.list(parent.getResources("b.txt"))).isEmpty();

    ClassLoader child = classloaders2.get("the-child");
    assertThat(Collections.list(child.getResources("a.txt"))).hasSize(1);
    assertThat(Collections.list(child.getResources("b.txt"))).hasSize(1);
  }

  @Test
  public void getResources_multiple_versions_with_parent_first_strategy() throws Exception {
    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("the-parent")
      .addURL("the-parent", new File("tester/a.jar").toURL())

      .newClassloader("the-child")
      .addURL("the-child", new File("tester/a_v2.jar").toURL())
      .setParent("the-child", "the-parent", Mask.ALL)
      .build();

    ClassLoader parent = newClassloaders.get("the-parent");
    assertThat(Collections.list(parent.getResources("a.txt"))).hasSize(1);

    ClassLoader child = newClassloaders.get("the-child");
    List<URL> childResources = Collections.list(child.getResources("a.txt"));
    assertThat(childResources).hasSize(2);
    assertThat(IOUtils.toString(childResources.get(0))).startsWith("version 1 of a.txt");
    assertThat(IOUtils.toString(childResources.get(1))).startsWith("version 2 of a.txt");
  }

  @Test
  public void resource_not_found_in_parent_first_strategy() throws Exception {
    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("the-parent")
      .addURL("the-parent", new File("tester/a.jar").toURL())

      .newClassloader("the-child")
      .addURL("the-child", new File("tester/a_v2.jar").toURL())
      .setParent("the-child", "the-parent", Mask.ALL)
      .build();

    ClassLoader parent = newClassloaders.get("the-child");
    assertThat(parent.getResource("missing")).isNull();
    try {
      parent.loadClass("missing");
      fail();
    } catch (ClassNotFoundException e) {
      // ok
    }
  }

  @Test
  public void resource_not_found_in_self_first_strategy() throws Exception {
    Map<String, ClassLoader> newClassloaders = sut
      .newClassloader("the-parent")
      .addURL("the-parent", new File("tester/a.jar").toURL())

      .newClassloader("the-child")
      .addURL("the-child", new File("tester/a_v2.jar").toURL())
      .setParent("the-child", "the-parent", Mask.ALL)
      .setLoadingOrder("the-child", ClassloaderBuilder.LoadingOrder.SELF_FIRST)
      .build();

    ClassLoader parent = newClassloaders.get("the-child");
    assertThat(parent.getResource("missing")).isNull();
    try {
      parent.loadClass("missing");
      fail();
    } catch (ClassNotFoundException e) {
      // ok
    }
  }

  private boolean canLoadClass(ClassLoader classloader, String classname) {
    try {
      classloader.loadClass(classname);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private boolean canLoadMethod(ClassLoader classloader, String classname, String methodName) {
    try {
      Class clazz = classloader.loadClass(classname);
      return clazz.getMethod(methodName) != null;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean canLoadResource(ClassLoader classloader, String name) {
    return classloader.getResource(name) != null;
  }
}
