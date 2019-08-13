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
package org.sonar.server.ui;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.api.Startable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.page.Context;
import org.sonar.api.web.page.Page;
import org.sonar.api.web.page.Page.Qualifier;
import org.sonar.api.web.page.Page.Scope;
import org.sonar.api.web.page.PageDefinition;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.extension.CoreExtensionRepository;
import org.sonar.server.ui.page.CorePageDefinition;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.web.page.Page.Scope.COMPONENT;
import static org.sonar.api.web.page.Page.Scope.GLOBAL;
import static org.sonar.api.web.page.Page.Scope.ORGANIZATION;
import static org.sonar.core.util.stream.MoreCollectors.toList;

@ServerSide
public class PageRepository implements Startable {
  private final PluginRepository pluginRepository;
  private final CoreExtensionRepository coreExtensionRepository;
  private final List<PageDefinition> definitions;
  private final List<CorePageDefinition> corePageDefinitions;
  private List<Page> pages;

  /**
   * Used by Pico when there is no {@link PageDefinition}.
   */
  public PageRepository(PluginRepository pluginRepository, CoreExtensionRepository coreExtensionRepository) {
    this.pluginRepository = pluginRepository;
    this.coreExtensionRepository = coreExtensionRepository;
    // in case there's no page definition
    this.definitions = emptyList();
    this.corePageDefinitions = emptyList();
  }

  /**
   * Used by Pico when there is only {@link PageDefinition} provided both by Plugin(s).
   */
  public PageRepository(PluginRepository pluginRepository, CoreExtensionRepository coreExtensionRepository,
    PageDefinition[] pageDefinitions) {
    this.pluginRepository = pluginRepository;
    this.coreExtensionRepository = coreExtensionRepository;
    this.definitions = copyOf(pageDefinitions);
    this.corePageDefinitions = emptyList();
  }

  /**
   * Used by Pico when there is only {@link PageDefinition} provided both by Core Extension(s).
   */
  public PageRepository(PluginRepository pluginRepository, CoreExtensionRepository coreExtensionRepository,
    CorePageDefinition[] corePageDefinitions) {
    this.pluginRepository = pluginRepository;
    this.coreExtensionRepository = coreExtensionRepository;
    this.definitions = emptyList();
    this.corePageDefinitions = ImmutableList.copyOf(corePageDefinitions);
  }

  /**
   * Used by Pico when there is {@link PageDefinition} provided both by Core Extension(s) and Plugin(s).
   */
  public PageRepository(PluginRepository pluginRepository, CoreExtensionRepository coreExtensionRepository,
    PageDefinition[] pageDefinitions, CorePageDefinition[] corePageDefinitions) {
    this.pluginRepository = pluginRepository;
    this.coreExtensionRepository = coreExtensionRepository;
    this.definitions = copyOf(pageDefinitions);
    this.corePageDefinitions = ImmutableList.copyOf(corePageDefinitions);
  }

  @Override
  public void start() {
    Context context = new Context();
    definitions.forEach(definition -> definition.define(context));
    Context coreContext = new Context();
    corePageDefinitions.stream()
      .map(CorePageDefinition::getPageDefinition)
      .forEach(definition -> definition.define(coreContext));
    pages = Stream.concat(
      context.getPages().stream().peek(this::checkPluginExists),
      coreContext.getPages().stream().peek(this::checkCoreExtensionExists))
      .sorted(comparing(Page::getKey))
      .collect(toList());
  }

  @Override
  public void stop() {
    // nothing to do
  }

  public List<Page> getGlobalPages(boolean isAdmin) {
    return getPages(GLOBAL, isAdmin, null);
  }

  public List<Page> getOrganizationPages(boolean isAdmin) {
    return getPages(ORGANIZATION, isAdmin, null);
  }

  public List<Page> getComponentPages(boolean isAdmin, String qualifierKey) {
    Qualifier qualifier = Qualifier.fromKey(qualifierKey);
    return qualifier == null ? emptyList() : getPages(COMPONENT, isAdmin, qualifier);
  }

  private List<Page> getPages(Scope scope, boolean isAdmin, @Nullable Qualifier qualifier) {
    return getAllPages().stream()
      .filter(p -> p.getScope().equals(scope))
      .filter(p -> p.isAdmin() == isAdmin)
      .filter(p -> !COMPONENT.equals(p.getScope()) || p.getComponentQualifiers().contains(qualifier))
      .collect(toList());
  }

  @VisibleForTesting
  List<Page> getAllPages() {
    return requireNonNull(pages, "Pages haven't been initialized yet");
  }

  private void checkPluginExists(Page page) {
    String pluginKey = page.getPluginKey();
    checkState(pluginRepository.hasPlugin(pluginKey),
      "Page '%s' references plugin '%s' that does not exist", page.getName(), pluginKey);
  }

  private void checkCoreExtensionExists(Page page) {
    String coreExtensionName = page.getPluginKey();
    checkState(coreExtensionRepository.isInstalled(coreExtensionName),
      "Page '%s' references Core Extension '%s' which is not installed", page.getName(), coreExtensionName);
  }

}
