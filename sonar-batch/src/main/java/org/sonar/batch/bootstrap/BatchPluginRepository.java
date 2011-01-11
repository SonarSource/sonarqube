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
package org.sonar.batch.bootstrap;

import com.google.common.collect.Lists;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.Plugin;
import org.sonar.api.batch.AbstractCoverageExtension;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.platform.Environment;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.SonarException;
import org.sonar.core.classloaders.ClassLoadersCollection;
import org.sonar.core.plugin.AbstractPluginRepository;
import org.sonar.core.plugin.JpaPlugin;
import org.sonar.core.plugin.JpaPluginDao;
import org.sonar.core.plugin.JpaPluginFile;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class BatchPluginRepository extends AbstractPluginRepository {

  private static final Logger LOG = LoggerFactory.getLogger(BatchPluginRepository.class);

  private JpaPluginDao dao;

  private ClassLoadersCollection classLoaders;
  private ExtensionDownloader extensionDownloader;
  private Environment environment;

  public BatchPluginRepository(JpaPluginDao dao, ExtensionDownloader extensionDownloader, Environment environment) {
    this.dao = dao;
    this.extensionDownloader = extensionDownloader;
    this.environment = environment;
  }

  /**
   * for unit tests only
   */
  BatchPluginRepository() {

  }

  public void start() {
    classLoaders = new ClassLoadersCollection(Thread.currentThread().getContextClassLoader());
    for (JpaPlugin pluginMetadata : dao.getPlugins()) {
      String key = pluginMetadata.getKey();
      List<URL> urls = Lists.newArrayList();
      for (JpaPluginFile pluginFile : pluginMetadata.getFiles()) {
        File file = extensionDownloader.downloadExtension(pluginFile);
        try {
          urls.add(file.toURI().toURL());

        } catch (MalformedURLException e) {
          throw new SonarException("Can not get the URL of: " + file, e);
        }
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Classloader of plugin " + key + ":");
        for (URL url : urls) {
          LOG.debug("   -> " + url);
        }
      }
      classLoaders.createClassLoader(key, urls, pluginMetadata.isUseChildFirstClassLoader() == Boolean.TRUE);
    }
    classLoaders.done();
  }

  public void registerPlugins(MutablePicoContainer pico) {
    for (JpaPlugin pluginMetadata : dao.getPlugins()) {
      try {
        Class claz = classLoaders.get(pluginMetadata.getKey()).loadClass(pluginMetadata.getPluginClass());
        Plugin plugin = (Plugin) claz.newInstance();
        registerPlugin(pico, plugin, pluginMetadata.getKey());

      } catch (Exception e) {
        throw new SonarException("Fail to load extensions from plugin " + pluginMetadata.getKey(), e);
      }
    }
    invokeExtensionProviders(pico);
  }

  @Override
  protected boolean shouldRegisterExtension(PicoContainer container, String pluginKey, Object extension) {
    boolean ok = isType(extension, BatchExtension.class);
    if (ok && !isSupportsEnvironment(extension)) {
      ok = false;
      LOG.debug("The following extension is ignored: " + extension + " due to execution environment.");
    }
    if (ok && isType(extension, AbstractCoverageExtension.class)) {
      ok = shouldRegisterCoverageExtension(pluginKey, container.getComponent(Project.class), container.getComponent(Configuration.class));
      if (!ok) {
        LOG.debug("The following extension is ignored: " + extension + ". See the parameter " + AbstractCoverageExtension.PARAM_PLUGIN);
      }
    }
    return ok;
  }

  private boolean isSupportsEnvironment(Object extension) {
    Class clazz = (extension instanceof Class ? (Class) extension : extension.getClass());
    SupportedEnvironment env = AnnotationUtils.getClassAnnotation(clazz, SupportedEnvironment.class);
    if (env == null) {
      return true;
    }
    for (String supported : env.value()) {
      if (StringUtils.equalsIgnoreCase(environment.toString(), supported)) {
        return true;
      }
    }
    return false;
  }

  boolean shouldRegisterCoverageExtension(String pluginKey, Project project, Configuration conf) {
    boolean ok = true;
    if (StringUtils.equals(project.getLanguageKey(), Java.KEY)) {
      String[] selectedPluginKeys = conf.getStringArray(AbstractCoverageExtension.PARAM_PLUGIN);
      if (ArrayUtils.isEmpty(selectedPluginKeys)) {
        selectedPluginKeys = new String[] { AbstractCoverageExtension.DEFAULT_PLUGIN };
      }
      ok = ArrayUtils.contains(selectedPluginKeys, pluginKey);
    }
    return ok;
  }
}
