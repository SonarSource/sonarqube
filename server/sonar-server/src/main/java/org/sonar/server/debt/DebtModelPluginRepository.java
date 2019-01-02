/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.picocontainer.Startable;
import org.sonar.api.Plugin;
import org.sonar.api.server.ServerSide;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

import static com.google.common.collect.Lists.newArrayList;

/**
 * <p>This class is used to find which technical debt model XML files exist in the Sonar instance.</p>
 * <p>
 * Those XML files are provided by language plugins that embed their own contribution to the definition of the Technical debt model.
 * They must be located in the classpath of those language plugins, more specifically in the "com.sonar.sqale" package, and
 * they must be named "<pluginKey>-model.xml".
 * </p>
 */
@ServerSide
public class DebtModelPluginRepository implements Startable {

  public static final String DEFAULT_MODEL = "technical-debt";

  private static final String XML_FILE_SUFFIX = "-model.xml";
  private static final String XML_FILE_PREFIX = "com/sonar/sqale/";

  private String xmlFilePrefix;

  private PluginRepository pluginRepository;
  private Map<String, ClassLoader> contributingPluginKeyToClassLoader;

  public DebtModelPluginRepository(PluginRepository pluginRepository) {
    this.pluginRepository = pluginRepository;
    this.xmlFilePrefix = XML_FILE_PREFIX;
  }

  @VisibleForTesting
  DebtModelPluginRepository(PluginRepository pluginRepository, String xmlFilePrefix) {
    this.pluginRepository = pluginRepository;
    this.xmlFilePrefix = xmlFilePrefix;
  }

  @VisibleForTesting
  DebtModelPluginRepository(Map<String, ClassLoader> contributingPluginKeyToClassLoader, String xmlFilePrefix) {
    this.contributingPluginKeyToClassLoader = contributingPluginKeyToClassLoader;
    this.xmlFilePrefix = xmlFilePrefix;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    findAvailableXMLFiles();
  }

  private void findAvailableXMLFiles() {
    if (contributingPluginKeyToClassLoader == null) {
      contributingPluginKeyToClassLoader = Maps.newTreeMap();
      // Add default model
      contributingPluginKeyToClassLoader.put(DEFAULT_MODEL, getClass().getClassLoader());
      for (PluginInfo pluginInfo : pluginRepository.getPluginInfos()) {
        String pluginKey = pluginInfo.getKey();
        Plugin plugin = pluginRepository.getPluginInstance(pluginKey);
        ClassLoader classLoader = plugin.getClass().getClassLoader();
        if (classLoader.getResource(getXMLFilePath(pluginKey)) != null) {
          contributingPluginKeyToClassLoader.put(pluginKey, classLoader);
        }
      }
    }
    contributingPluginKeyToClassLoader = Collections.unmodifiableMap(contributingPluginKeyToClassLoader);
  }

  @VisibleForTesting
  String getXMLFilePath(String model) {
    return xmlFilePrefix + model + XML_FILE_SUFFIX;
  }

  /**
   * Returns the list of plugins that can contribute to the technical debt model.
   *
   * @return the list of plugin keys
   */
  public Collection<String> getContributingPluginList() {
    return newArrayList(contributingPluginKeyToClassLoader.keySet());
  }

  /**
   * Creates a new {@link java.io.Reader} for the XML file that contains the model contributed by the given plugin.
   *
   * @param pluginKey the key of the plugin that contributes the XML file
   * @return the reader, that must be closed once its use is finished.
   */
  public Reader createReaderForXMLFile(String pluginKey) {
    ClassLoader classLoader = contributingPluginKeyToClassLoader.get(pluginKey);
    String xmlFilePath = getXMLFilePath(pluginKey);
    return new InputStreamReader(classLoader.getResourceAsStream(xmlFilePath), StandardCharsets.UTF_8);
  }

  @VisibleForTesting
  Map<String, ClassLoader> getContributingPluginKeyToClassLoader() {
    return contributingPluginKeyToClassLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    // Nothing to do
  }

}
