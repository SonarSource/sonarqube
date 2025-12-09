/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.plugins;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import org.sonar.api.Plugin;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;
import org.sonar.api.internal.PluginContextImpl;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.core.platform.ExtensionContainer;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Loads the plugins server extensions and injects them to DI container
 */
public abstract class ServerExtensionInstaller {
  private final Configuration configuration;
  private final SonarRuntime sonarRuntime;
  private final PluginRepository pluginRepository;
  private final Set<Class<? extends Annotation>> supportedAnnotationTypes;

  protected ServerExtensionInstaller(Configuration configuration, SonarRuntime sonarRuntime, PluginRepository pluginRepository,
    Collection<Class<? extends Annotation>> supportedAnnotationTypes) {
    requireNonNull(supportedAnnotationTypes, "At least one supported annotation type must be specified");
    this.configuration = configuration;
    this.sonarRuntime = sonarRuntime;
    this.pluginRepository = pluginRepository;
    this.supportedAnnotationTypes = ImmutableSet.copyOf(supportedAnnotationTypes);
  }

  public void installExtensions(ExtensionContainer container) {
    ListMultimap<PluginInfo, Object> installedExtensionsByPlugin = ArrayListMultimap.create();
    for (PluginInfo pluginInfo : pluginRepository.getPluginInfos()) {
      try {
        String pluginKey = pluginInfo.getKey();
        Plugin plugin = pluginRepository.getPluginInstance(pluginKey);
        container.addExtension(pluginInfo, plugin);

        Plugin.Context context = new PluginContextImpl.Builder()
          .setSonarRuntime(sonarRuntime)
          .setBootConfiguration(configuration)
          .build();
        plugin.define(context);
        for (Object extension : context.getExtensions()) {
          if (installExtension(container, pluginInfo, extension) != null) {
            installedExtensionsByPlugin.put(pluginInfo, extension);
          } else {
            container.declareExtension(pluginInfo, extension);
          }
        }
      } catch (Throwable e) {
        // catch Throwable because we want to catch Error too (IncompatibleClassChangeError, ...)
        throw new IllegalStateException(format("Fail to load plugin %s [%s]", pluginInfo.getName(), pluginInfo.getKey()), e);
      }
    }
  }

  private Object installExtension(ExtensionContainer container, PluginInfo pluginInfo, Object extension) {
    for (Class<? extends Annotation> supportedAnnotationType : supportedAnnotationTypes) {
      if (AnnotationUtils.getAnnotation(extension, supportedAnnotationType) != null) {
        container.addExtension(pluginInfo, extension);
        return extension;
      }
    }
    return null;
  }

}
