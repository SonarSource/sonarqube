/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.resources;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JavaFileTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testNewClass() {
    JavaFile javaClass = JavaFile.create("src/main/java/org/foo/bar/Hello.java", "org/foo/bar/Hello.java", false);
    assertThat(javaClass.getKey()).isEqualTo("/src/main/java/org/foo/bar/Hello.java");
    assertThat(javaClass.getDeprecatedKey(), is("org.foo.bar.Hello"));
    assertThat(javaClass.getName(), is("Hello"));
    assertThat(javaClass.getLongName(), is("org.foo.bar.Hello"));
    assertThat(javaClass.getParent().getKey(), is("/src/main/java/org/foo/bar"));
    assertThat(javaClass.getParent().getDeprecatedKey(), is("org.foo.bar"));
  }

  @Test
  public void testNewClassByDeprecatedKey() {
    JavaFile javaClass = new JavaFile("org.foo.bar.Hello", false);
    assertThat(javaClass.getDeprecatedKey(), is("org.foo.bar.Hello"));
    assertThat(javaClass.getName(), is("Hello"));
    assertThat(javaClass.getLongName(), is("org.foo.bar.Hello"));
    assertThat(javaClass.getParent().getDeprecatedKey(), is("org.foo.bar"));
  }

  @Test
  public void testNewClassWithExplicitPackage() {
    JavaFile javaClass = new JavaFile("org.foo.bar", "Hello", false);
    assertThat(javaClass.getDeprecatedKey(), is("org.foo.bar.Hello"));
    assertThat(javaClass.getName(), is("Hello"));
    assertThat(javaClass.getLongName(), is("org.foo.bar.Hello"));
    assertThat(javaClass.getParent().getDeprecatedKey(), is("org.foo.bar"));
  }

  @Test
  public void shouldAcceptFilenamesWithDollars() {
    // $ is not used only for inner classes !!!
    JavaFile javaFile = new JavaFile("org.foo.bar", "Hello$Bar");
    assertThat(javaFile.getDeprecatedKey(), is("org.foo.bar.Hello$Bar"));
  }

  @Test
  public void testNewClassWithEmptyPackage() {
    JavaFile javaClass = JavaFile.create("src/main/java/Hello.java", "Hello.java", false);
    assertThat(javaClass.getKey()).isEqualTo("/src/main/java/Hello.java");
    assertThat(javaClass.getDeprecatedKey(), is(JavaPackage.DEFAULT_PACKAGE_NAME + ".Hello"));
    assertThat(javaClass.getName(), is("Hello"));
    assertThat(javaClass.getLongName(), is("Hello"));
    assertThat(javaClass.getParent().getKey()).isEqualTo("/src/main/java");
    assertThat(javaClass.getParent().getDeprecatedKey()).isEqualTo(JavaPackage.DEFAULT_PACKAGE_NAME);
    assertThat(javaClass.getParent().isDefault()).isTrue();
  }

  @Test
  public void testNewClassWithEmptyPackageDeprecatedConstructor() {
    JavaFile javaClass = new JavaFile("", "Hello", false);
    assertThat(javaClass.getDeprecatedKey(), is(JavaPackage.DEFAULT_PACKAGE_NAME + ".Hello"));
    assertThat(javaClass.getName(), is("Hello"));
    assertThat(javaClass.getLongName(), is("Hello"));
    assertThat(javaClass.getParent().isDefault(), is(true));
  }

  @Test
  public void testNewClassWithNullPackageDeprecatedConstructor() {
    JavaFile javaClass = new JavaFile(null, "Hello", false);
    assertThat(javaClass.getDeprecatedKey(), is(JavaPackage.DEFAULT_PACKAGE_NAME + ".Hello"));
    assertThat(javaClass.getName(), is("Hello"));
    assertThat(javaClass.getLongName(), is("Hello"));
    assertThat((javaClass.getParent()).isDefault(), is(true));
  }

  @Test
  public void shouldBeDefaultPackageIfNoPackage() {
    JavaFile javaClass = new JavaFile("Hello", false);
    assertEquals(JavaPackage.DEFAULT_PACKAGE_NAME + ".Hello", javaClass.getDeprecatedKey());
    assertThat(javaClass.getName(), is("Hello"));
    assertThat(javaClass.getLongName(), is("Hello"));
    assertThat(javaClass.getParent().isDefault(), is(true));
  }

  @Test
  public void aClassShouldBeNamedJava() {
    JavaFile javaClass = new JavaFile("org.foo.bar.Java", false);
    assertThat(javaClass.getDeprecatedKey(), is("org.foo.bar.Java"));
    assertThat(javaClass.getLongName(), is("org.foo.bar.Java"));
    assertThat(javaClass.getName(), is("Java"));
    JavaPackage parent = javaClass.getParent();
    assertEquals("org.foo.bar", parent.getDeprecatedKey());
  }

  @Test
  public void shouldTrimClasses() {
    JavaFile clazz = new JavaFile("   org.foo.bar.Hello   ", false);
    assertThat(clazz.getDeprecatedKey(), is("org.foo.bar.Hello"));
    assertThat(clazz.getLongName(), is("org.foo.bar.Hello"));
    assertThat(clazz.getName(), is("Hello"));
    JavaPackage parent = clazz.getParent();
    assertThat(parent.getDeprecatedKey(), is("org.foo.bar"));
  }

  @Test
  public void testEqualsOnClasses() {
    JavaFile class1 = new JavaFile("foo.bar", "Hello", false);
    JavaFile class2 = new JavaFile("foo.bar.Hello", false);
    assertEquals(class1, class2);

    class1 = new JavaFile("NoPackage", false);
    class2 = new JavaFile("NoPackage", false);
    assertEquals(class1, class2);
    assertEquals(class1, class1);
  }

  @Test
  public void oneLevelPackage() {
    JavaFile clazz = new JavaFile("onelevel.MyFile");
    assertEquals("onelevel.MyFile", clazz.getDeprecatedKey());
    assertEquals("onelevel", clazz.getParent().getDeprecatedKey());

    clazz = new JavaFile("onelevel", "MyFile");
    assertEquals("onelevel.MyFile", clazz.getDeprecatedKey());
    assertEquals("onelevel", clazz.getParent().getDeprecatedKey());

    File sourceDir = newDir("sources");
    List<File> sources = Arrays.asList(sourceDir);
    JavaFile javaFile = JavaFile.fromAbsolutePath(absPath(sourceDir, "onelevel/MyFile.java"), sources, false);
    assertEquals("onelevel.MyFile", javaFile.getDeprecatedKey());
    assertEquals("MyFile", javaFile.getName());
    assertEquals("onelevel", javaFile.getParent().getDeprecatedKey());
    assertEquals("onelevel", javaFile.getParent().getName());
    assertThat(javaFile.getParent().isDefault(), is(false));
  }

  @Test
  public void shouldResolveClassFromAbsolutePath() {
    File sources1 = newDir("source1");
    File sources2 = newDir("source2");
    List<File> sources = Arrays.asList(sources1, sources2);
    JavaFile javaFile = JavaFile.fromAbsolutePath(absPath(sources2, "foo/bar/MyFile.java"), sources, false);
    assertThat("foo.bar.MyFile", is(javaFile.getDeprecatedKey()));
    assertThat(javaFile.getLongName(), is("foo.bar.MyFile"));
    assertThat(javaFile.getName(), is("MyFile"));
    assertThat(javaFile.getParent().getDeprecatedKey(), is("foo.bar"));
  }

  @Test
  public void shouldResolveFromAbsolutePathEvenIfDefaultPackage() {
    File source1 = newDir("source1");
    File source2 = newDir("source2");
    List<File> sources = Arrays.asList(source1, source2);

    JavaFile javaClass = JavaFile.fromAbsolutePath(absPath(source1, "MyClass.java"), sources, false);
    assertEquals(JavaPackage.DEFAULT_PACKAGE_NAME + ".MyClass", javaClass.getDeprecatedKey());
    assertEquals("MyClass", javaClass.getName());

    assertThat((javaClass.getParent()).isDefault(), is(true));
  }

  @Test
  public void shouldResolveOnlyJavaFromAbsolutePath() {
    File source1 = newDir("source1");
    List<File> sources = Arrays.asList(source1);
    assertNull(JavaFile.fromAbsolutePath(absPath(source1, "foo/bar/my_file.sql"), sources, false));
  }

  @Test
  public void shouldNotFailWhenResolvingUnknownClassFromAbsolutePath() {
    File source1 = newDir("source1");
    List<File> sources = Arrays.asList(source1);
    assertNull(JavaFile.fromAbsolutePath("/home/other/src/main/java/foo/bar/MyClass.java", sources, false));
  }

  @Test
  public void shouldMatchFilePatterns() {
    JavaFile clazz = JavaFile.create("src/main/java/org/sonar/commons/Foo.java", "org/sonar/commons/Foo.java", false);
    assertTrue(clazz.matchFilePattern("**/commons/**/*.java"));
    assertTrue(clazz.matchFilePattern("/**/commons/**/*.java"));
    assertTrue(clazz.matchFilePattern("/**/commons/**/*.*"));
    assertFalse(clazz.matchFilePattern("/**/sonar/*.java"));
    assertTrue(clazz.matchFilePattern("/src/main/java/org/*/commons/**/*.java"));
    assertTrue(clazz.matchFilePattern("src/main/java/org/sonar/commons/*"));
    assertTrue(clazz.matchFilePattern("src/main/java/org/sonar/**/*.java"));
    assertFalse(clazz.matchFilePattern("src/main/java/org/sonar/*"));
    assertFalse(clazz.matchFilePattern("src/main/java/org/sonar*/*"));
    assertTrue(clazz.matchFilePattern("src/main/java/org/**"));
    assertTrue(clazz.matchFilePattern("*src/main/java/org/sona?/co??ons/**.*"));
    assertFalse(clazz.matchFilePattern("src/main/java/org/sonar/core/**"));
    assertTrue(clazz.matchFilePattern("src/main/java/org/sonar/commons/Foo.java"));
    assertTrue(clazz.matchFilePattern("**/*Foo.java"));
    assertTrue(clazz.matchFilePattern("**/*Foo.*"));
    assertTrue(clazz.matchFilePattern("src/main/java/org/*/*/Foo.java"));
    assertTrue(clazz.matchFilePattern("src/main/java/org/**/**/Foo.java"));
    assertTrue(clazz.matchFilePattern("**/commons/**/*"));
    assertTrue(clazz.matchFilePattern("**/*"));
  }

  // SONAR-4397
  @Test
  public void shouldMatchFilePatternsWhenNoPackage() {
    JavaFile clazz = JavaFile.create("src/main/java/Foo.java", "Foo.java", false);
    assertTrue(clazz.matchFilePattern("**/*Foo.java"));
    assertTrue(clazz.matchFilePattern("**/*Foo.*"));
    assertTrue(clazz.matchFilePattern("**/*"));
    assertTrue(clazz.matchFilePattern("src/main/java/Foo*.*"));
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-1449
   */
  @Test
  public void doNotMatchAPattern() {
    JavaFile file = JavaFile.create("src/main/java/org/sonar/commons/Foo.java", "org/sonar/commons/Foo.java", false);
    assertFalse(file.matchFilePattern("**/*.aj"));
    assertTrue(file.matchFilePattern("**/*.java"));
  }

  @Test
  public void should_exclude_test_files() {
    JavaFile unitTest = JavaFile.create("src/main/java/org/sonar/commons/Foo.java", "org/sonar/commons/Foo.java", true);
    assertTrue(unitTest.matchFilePattern("**/*"));
  }

  private File newDir(String dirName) {
    return tempFolder.newFolder(dirName);
  }

  private String absPath(File dir, String filePath) {
    return new File(dir, filePath).getPath();
  }
}
