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
package org.sonar.batch.bootstrap;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

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
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.core.classloaders.ClassLoadersCollection;
import org.sonar.core.plugin.AbstractPluginRepository;
import org.sonar.core.plugin.JpaPlugin;
import org.sonar.core.plugin.JpaPluginDao;
import org.sonar.core.plugin.JpaPluginFile;

public class BatchPluginRepository extends AbstractPluginRepository {

  private static final Logger LOG = LoggerFactory.getLogger(BatchPluginRepository.class);

  private JpaPluginDao dao;

  private ClassLoadersCollection classLoaders;
  private ExtensionDownloader extensionDownloader;
  private EnvironmentInformation environment;
  private List<JpaPlugin> register;

  public BatchPluginRepository(JpaPluginDao dao, ExtensionDownloader extensionDownloader, EnvironmentInformation environment) {
    this.dao = dao;
    this.extensionDownloader = extensionDownloader;
    this.environment = environment;
    LOG.info("Execution environment: {} {}", environment.getKey(), environment.getVersion());
  }

  /**
   * for unit tests only
   */
  BatchPluginRepository() {
  }

  private List<URL> download(JpaPlugin pluginMetadata) {
    List<URL> urls = Lists.newArrayList();
    for (JpaPluginFile pluginFile : pluginMetadata.getFiles()) {
      File file = extensionDownloader.downloadExtension(pluginFile);
      try {
        urls.add(file.toURI().toURL());

      } catch (MalformedURLException e) {
        throw new SonarException("Can not get the URL of: " + file, e);
      }
    }
    return urls;
  }

  public void start() {
    register = Lists.newArrayList();
    classLoaders = new ClassLoadersCollection(Thread.currentThread().getContextClassLoader());

    List<JpaPlugin> jpaPlugins = dao.getPlugins();

    for (JpaPlugin pluginMetadata : jpaPlugins) {
      if (StringUtils.isEmpty(pluginMetadata.getBasePlugin())) {
        String key = pluginMetadata.getKey();
        List<URL> urls = download(pluginMetadata);
        classLoaders.createClassLoader(key, urls, pluginMetadata.isUseChildFirstClassLoader() == Boolean.TRUE);
        register.add(pluginMetadata);
      }
    }

    // Extend plugins by other plugins
    for (JpaPlugin pluginMetadata : jpaPlugins) {
      String pluginKey = pluginMetadata.getKey();
      String basePluginKey = pluginMetadata.getBasePlugin();
      if (StringUtils.isNotEmpty(basePluginKey)) {
        if (classLoaders.get(basePluginKey) != null) {
          LOG.debug("Plugin {} extends {}", pluginKey, basePluginKey);
          List<URL> urls = download(pluginMetadata);
          classLoaders.extend(basePluginKey, pluginKey, urls);
          register.add(pluginMetadata);
        } else {
          // Ignored, because base plugin doesn't exists
          LOG.warn("Plugin {} extends nonexistent plugin {}", pluginKey, basePluginKey);
        }
      }
    }

    classLoaders.done();
  }

  public void registerPlugins(MutablePicoContainer pico) {
    for (JpaPlugin pluginMetadata : register) {
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
    if (ok && isType(extension, CoverageExtension.class)) {
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
      if (StringUtils.equalsIgnoreCase(environment.getKey(), supported)) {
        return true;
      }
    }
    return false;
  }

  boolean shouldRegisterCoverageExtension(String pluginKey, Project project, Configuration conf) {
    if (!project.getAnalysisType().isDynamic(true)) {
      // not dynamic and not reuse reports
      return false;
    }
    if (StringUtils.equals(project.getLanguageKey(), Java.KEY)) {
      String[] selectedPluginKeys = conf.getStringArray(AbstractCoverageExtension.PARAM_PLUGIN);
      if (ArrayUtils.isEmpty(selectedPluginKeys)) {
        selectedPluginKeys = new String[] { AbstractCoverageExtension.DEFAULT_PLUGIN };
      }
      return ArrayUtils.contains(selectedPluginKeys, pluginKey);
    }
    return true;
  }
}
