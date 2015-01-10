/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.server.debt;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.SonarPlugin;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DebtModelPluginRepositoryTest {

  private static final String TEST_XML_PREFIX_PATH = "org/sonar/server/debt/DebtModelPluginRepositoryTest/";

  private DebtModelPluginRepository modelFinder;

  @Test
  public void test_component_initialization() throws Exception {
    // we do have the "csharp-model.xml" file in src/test/resources
    PluginMetadata csharpPluginMetadata = mock(PluginMetadata.class);
    when(csharpPluginMetadata.getKey()).thenReturn("csharp");

    // but we don' have the "php-model.xml" one
    PluginMetadata phpPluginMetadata = mock(PluginMetadata.class);
    when(phpPluginMetadata.getKey()).thenReturn("php");

    PluginRepository repository = mock(PluginRepository.class);
    when(repository.getMetadata()).thenReturn(Lists.newArrayList(csharpPluginMetadata, phpPluginMetadata));
    FakePlugin fakePlugin = new FakePlugin();
    when(repository.getPlugin(anyString())).thenReturn(fakePlugin);
    modelFinder = new DebtModelPluginRepository(repository, TEST_XML_PREFIX_PATH);

    // when
    modelFinder.start();

    // assert
    Collection<String> contributingPluginList = modelFinder.getContributingPluginList();
    assertThat(contributingPluginList.size()).isEqualTo(2);
    assertThat(contributingPluginList).containsOnly("technical-debt", "csharp");
  }

  @Test
  public void contributing_plugin_list() throws Exception {
    initModel();
    Collection<String> contributingPluginList = modelFinder.getContributingPluginList();
    assertThat(contributingPluginList.size()).isEqualTo(2);
    assertThat(contributingPluginList).contains("csharp", "java");
  }

  @Test
  public void get_content_for_xml_file() throws Exception {
    initModel();
    Reader xmlFileReader = null;
    try {
      xmlFileReader = modelFinder.createReaderForXMLFile("csharp");
      assertNotNull(xmlFileReader);
      List<String> lines = IOUtils.readLines(xmlFileReader);
      assertThat(lines.size()).isEqualTo(25);
      assertThat(lines.get(0)).isEqualTo("<sqale>");
    } catch (Exception e) {
      fail("Should be able to read the XML file.");
    } finally {
      IOUtils.closeQuietly(xmlFileReader);
    }
  }

  @Test
  public void return_xml_file_path_for_plugin() throws Exception {
    initModel();
    assertThat(modelFinder.getXMLFilePath("foo")).isEqualTo(TEST_XML_PREFIX_PATH + "foo-model.xml");
  }

  @Test
  public void contain_default_model() throws Exception {
    modelFinder = new DebtModelPluginRepository(mock(PluginRepository.class));
    modelFinder.start();
    assertThat(modelFinder.getContributingPluginKeyToClassLoader().keySet()).containsOnly("technical-debt");
  }

  private void initModel() throws MalformedURLException {
    Map<String, ClassLoader> contributingPluginKeyToClassLoader = Maps.newHashMap();
    contributingPluginKeyToClassLoader.put("csharp", newClassLoader());
    contributingPluginKeyToClassLoader.put("java", newClassLoader());
    modelFinder = new DebtModelPluginRepository(contributingPluginKeyToClassLoader, TEST_XML_PREFIX_PATH);
  }

  private ClassLoader newClassLoader() throws MalformedURLException {
    ClassLoader loader = mock(ClassLoader.class);
    when(loader.getResourceAsStream(anyString())).thenAnswer(new Answer<InputStream>() {
      public InputStream answer(InvocationOnMock invocation) throws Throwable {
        return new FileInputStream(Resources.getResource((String) invocation.getArguments()[0]).getPath());
      }
    });
    return loader;
  }

  class FakePlugin extends SonarPlugin {
    public List getExtensions() {
      return null;
    }
  }

}
