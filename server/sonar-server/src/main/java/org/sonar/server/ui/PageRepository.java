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
package org.sonar.server.ui;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.Startable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.page.Context;
import org.sonar.api.web.page.Page;
import org.sonar.api.web.page.Page.Qualifier;
import org.sonar.api.web.page.Page.Scope;
import org.sonar.api.web.page.PageDefinition;
import org.sonar.core.platform.PluginRepository;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.web.page.Page.Scope.COMPONENT;
import static org.sonar.api.web.page.Page.Scope.GLOBAL;
import static org.sonar.api.web.page.Page.Scope.ORGANIZATION;
import static org.sonar.core.util.stream.MoreCollectors.toList;

@ServerSide
public class PageRepository implements Startable {
  private static final Splitter PAGE_KEY_SPLITTER = Splitter.on("/").omitEmptyStrings();

  private final PluginRepository pluginRepository;
  private final List<PageDefinition> definitions;
  private List<Page> pages;

  public PageRepository(PluginRepository pluginRepository) {
    this.pluginRepository = pluginRepository;
    // in case there's no page definition
    this.definitions = Collections.emptyList();
  }

  public PageRepository(PluginRepository pluginRepository, PageDefinition[] pageDefinitions) {
    this.pluginRepository = pluginRepository;
    definitions = ImmutableList.copyOf(pageDefinitions);
  }

  private static Consumer<Page> checkWellFormed() {
    return page -> {
      boolean isWellFormed = PAGE_KEY_SPLITTER.splitToList(page.getKey()).size() == 2;
      checkState(isWellFormed, "Page '%s' with key '%s' does not respect the format plugin_key/extension_point_key (ex: governance/project_dump)",
        page.getName(), page.getKey());
    };
  }

  @Override
  public void start() {
    Context context = new Context();
    definitions.forEach(definition -> definition.define(context));
    pages = context.getPages().stream()
      .peek(checkWellFormed())
      .peek(checkPluginExists())
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

  private Consumer<Page> checkPluginExists() {
    return page -> {
      String plugin = PAGE_KEY_SPLITTER.splitToList(page.getKey()).get(0);
      boolean pluginExists = pluginRepository.hasPlugin(plugin);
      checkState(pluginExists, "Page '%s' references plugin '%s' that does not exist", page.getName(), plugin);
    };
  }

}
