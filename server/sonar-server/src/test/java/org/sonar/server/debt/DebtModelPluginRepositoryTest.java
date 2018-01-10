/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.debt;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.SonarPlugin;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DebtModelPluginRepositoryTest {

  private static final String TEST_XML_PREFIX_PATH = "org/sonar/server/debt/DebtModelPluginRepositoryTest/";

  DebtModelPluginRepository underTest;

  @Test
  public void test_component_initialization() {
    // we do have the "csharp-model.xml" file in src/test/resources
    PluginInfo csharpPluginMetadata = new PluginInfo("csharp");

    // but we don' have the "php-model.xml" one
    PluginInfo phpPluginMetadata = new PluginInfo("php");

    PluginRepository repository = mock(PluginRepository.class);
    when(repository.getPluginInfos()).thenReturn(Lists.newArrayList(csharpPluginMetadata, phpPluginMetadata));
    FakePlugin fakePlugin = new FakePlugin();
    when(repository.getPluginInstance(anyString())).thenReturn(fakePlugin);
    underTest = new DebtModelPluginRepository(repository, TEST_XML_PREFIX_PATH);

    // when
    underTest.start();

    // assert
    Collection<String> contributingPluginList = underTest.getContributingPluginList();
    assertThat(contributingPluginList.size()).isEqualTo(2);
    assertThat(contributingPluginList).containsOnly("technical-debt", "csharp");
  }

  @Test
  public void contributing_plugin_list() {
    initModel();
    Collection<String> contributingPluginList = underTest.getContributingPluginList();
    assertThat(contributingPluginList.size()).isEqualTo(2);
    assertThat(contributingPluginList).contains("csharp", "java");
  }

  @Test
  public void get_content_for_xml_file() {
    initModel();
    Reader xmlFileReader = null;
    try {
      xmlFileReader = underTest.createReaderForXMLFile("csharp");
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
  public void return_xml_file_path_for_plugin() {
    initModel();
    assertThat(underTest.getXMLFilePath("foo")).isEqualTo(TEST_XML_PREFIX_PATH + "foo-model.xml");
  }

  @Test
  public void contain_default_model() {
    underTest = new DebtModelPluginRepository(mock(PluginRepository.class));
    underTest.start();
    assertThat(underTest.getContributingPluginKeyToClassLoader().keySet()).containsOnly("technical-debt");
  }

  private void initModel() {
    Map<String, ClassLoader> contributingPluginKeyToClassLoader = Maps.newHashMap();
    contributingPluginKeyToClassLoader.put("csharp", newClassLoader());
    contributingPluginKeyToClassLoader.put("java", newClassLoader());
    underTest = new DebtModelPluginRepository(contributingPluginKeyToClassLoader, TEST_XML_PREFIX_PATH);
  }

  private ClassLoader newClassLoader() {
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
