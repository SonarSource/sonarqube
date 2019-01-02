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
package org.sonar.core.extension;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.ComponentContainer;

import static java.util.Objects.requireNonNull;

public abstract class CoreExtensionsInstaller {
  private static final Logger LOG = Loggers.get(CoreExtensionsInstaller.class);

  private final SonarRuntime sonarRuntime;
  private final CoreExtensionRepository coreExtensionRepository;
  private final Class<? extends Annotation> supportedAnnotationType;

  public static Predicate<Object> noExtensionFilter() {
    return t -> true;
  }

  public static Predicate<Object> noAdditionalSideFilter() {
    return t -> true;
  }

  protected CoreExtensionsInstaller(SonarRuntime sonarRuntime, CoreExtensionRepository coreExtensionRepository,
    Class<? extends Annotation> supportedAnnotationType) {
    this.sonarRuntime = sonarRuntime;
    this.coreExtensionRepository = coreExtensionRepository;
    this.supportedAnnotationType = supportedAnnotationType;
  }

  /**
   * @param container the container into which extensions will be installed
   * @param extensionFilter filters extensions added to {@link CoreExtension.Context}. When it returns false, the
   *                        extension is ignored as if it had never been added to the context.
   * @param additionalSideFilter applied on top of filtering on {@link #supportedAnnotationType} to decide whether
   *                             extension should be added to container as an object or only as a PropertyDefinition.
   */
  public void install(ComponentContainer container, Predicate<Object> extensionFilter, Predicate<Object> additionalSideFilter) {
    coreExtensionRepository.loadedCoreExtensions()
      .forEach(coreExtension -> install(container, extensionFilter, additionalSideFilter, coreExtension));
  }

  private void install(ComponentContainer container, Predicate<Object> extensionFilter, Predicate<Object> additionalSideFilter, CoreExtension coreExtension) {
    String coreExtensionName = coreExtension.getName();
    try {
      List<Object> providerKeys = addDeclaredExtensions(container, extensionFilter, additionalSideFilter, coreExtension);
      addProvidedExtensions(container, additionalSideFilter, coreExtensionName, providerKeys);

      LOG.debug("Installed core extension: " + coreExtensionName);
      coreExtensionRepository.installed(coreExtension);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load core extension " + coreExtensionName, e);
    }
  }

  private List<Object> addDeclaredExtensions(ComponentContainer container, Predicate<Object> extensionFilter,
    Predicate<Object> additionalSideFilter, CoreExtension coreExtension) {
    ContextImpl context = new ContextImpl(container, extensionFilter, additionalSideFilter, coreExtension.getName());
    coreExtension.load(context);
    return context.getProviders();
  }

  private void addProvidedExtensions(ComponentContainer container, Predicate<Object> additionalSideFilter,
    String extensionCategory, List<Object> providerKeys) {
    providerKeys.stream()
      .map(providerKey -> (ExtensionProvider) container.getComponentByKey(providerKey))
      .forEach(provider -> addFromProvider(container, additionalSideFilter, extensionCategory, provider));
  }

  private void addFromProvider(ComponentContainer container, Predicate<Object> additionalSideFilter,
    String extensionCategory, ExtensionProvider provider) {
    Object obj = provider.provide();
    if (obj != null) {
      if (obj instanceof Iterable) {
        for (Object ext : (Iterable) obj) {
          addSupportedExtension(container, additionalSideFilter, extensionCategory, ext);
        }
      } else {
        addSupportedExtension(container, additionalSideFilter, extensionCategory, obj);
      }
    }
  }

  private <T> boolean addSupportedExtension(ComponentContainer container, Predicate<Object> additionalSideFilter,
    String extensionCategory, T component) {
    if (hasSupportedAnnotation(component) && additionalSideFilter.test(component)) {
      container.addExtension(extensionCategory, component);
      return true;
    }
    return false;
  }

  private <T> boolean hasSupportedAnnotation(T component) {
    return AnnotationUtils.getAnnotation(component, supportedAnnotationType) != null;
  }

  private class ContextImpl implements CoreExtension.Context {
    private final ComponentContainer container;
    private final Predicate<Object> extensionFilter;
    private final Predicate<Object> additionalSideFilter;
    private final String extensionCategory;
    private final List<Object> providers = new ArrayList<>();

    public ContextImpl(ComponentContainer container, Predicate<Object> extensionFilter,
      Predicate<Object> additionalSideFilter, String extensionCategory) {
      this.container = container;
      this.extensionFilter = extensionFilter;
      this.additionalSideFilter = additionalSideFilter;
      this.extensionCategory = extensionCategory;
    }

    @Override
    public SonarRuntime getRuntime() {
      return sonarRuntime;
    }

    @Override
    public Configuration getBootConfiguration() {
      return Optional.ofNullable(container.getComponentByType(Configuration.class))
        .orElseGet(() -> new MapSettings().asConfig());
    }

    @Override
    public CoreExtension.Context addExtension(Object component) {
      requireNonNull(component, "component can't be null");
      if (!extensionFilter.test(component)) {
        return this;
      }

      if (!addSupportedExtension(container, additionalSideFilter, extensionCategory, component)) {
        container.declareExtension(extensionCategory, component);
      } else if (ExtensionProviderSupport.isExtensionProvider(component)) {
        providers.add(component);
      }
      return this;
    }

    @Override
    public final CoreExtension.Context addExtensions(Object component, Object... otherComponents) {
      addExtension(component);
      Arrays.stream(otherComponents).forEach(this::addExtension);
      return this;
    }

    @Override
    public <T> CoreExtension.Context addExtensions(Collection<T> components) {
      requireNonNull(components, "components can't be null");
      components.forEach(this::addExtension);
      return this;
    }

    public List<Object> getProviders() {
      return providers;
    }
  }
}
