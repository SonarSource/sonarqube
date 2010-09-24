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
package org.sonar.plugins.core.sensors;

import org.apache.commons.io.FileUtils;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.sonar.api.batch.ResourceCreationLock;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class JavaSourceImporterTest {

  @Test
  public void shouldCreateResource() throws IOException {
    JavaSourceImporter importer = new JavaSourceImporter(mock(ResourceCreationLock.class));
    Resource clazz = importer.createResource(new File(newDir("source1"), "/MyClass.java"), Arrays.asList(newDir("source1")), false);
    assertThat(clazz, is(JavaFile.class));
    assertThat(clazz.getKey(), is(JavaPackage.DEFAULT_PACKAGE_NAME + ".MyClass"));
    assertThat(clazz.getName(), is("MyClass"));
  }

  @Test
  public void shouldCreateTestResource() throws IOException {
    JavaSourceImporter importer = new JavaSourceImporter(mock(ResourceCreationLock.class));
    Resource resource = importer.createResource(new File(newDir("tests"), "/MyClassTest.java"), Arrays.asList(newDir("tests")), true);
    assertThat(resource, is(JavaFile.class));
    assertThat(ResourceUtils.isUnitTestClass(resource), is(true));
  }

  @Test
  public void doNotSaveInnerClasses() throws IOException {
    // example : https://svn.apache.org/repos/asf/geronimo/server/trunk/plugins/corba/geronimo-corba/src/test/java/org/apache/geronimo/corba/compiler/other/Generic$Interface.java
    JavaSourceImporter importer = new JavaSourceImporter(mock(ResourceCreationLock.class));
    Resource resource = importer.createResource(new File(newDir("tests"), "/Generic$Interface.java"), Arrays.asList(newDir("tests")), true);
    assertThat(resource, nullValue());
  }

  private File newDir(String relativePath) throws IOException {
    File target = new File("target", relativePath);
    FileUtils.forceMkdir(target);
    return target;
  }
}