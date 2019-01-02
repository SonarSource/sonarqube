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
package org.sonar.api.web.page;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.web.page.Page.Scope.COMPONENT;
import static org.sonar.api.web.page.Page.Scope.GLOBAL;

/**
 * @see PageDefinition
 * @since 6.3
 */
public final class Page {
  private final String key;
  private final String name;
  private final boolean isAdmin;
  private final Scope scope;
  private final Set<Qualifier> qualifiers;

  private Page(Builder builder) {
    this.key = builder.key;
    this.name = builder.name;
    this.qualifiers = Stream.of(builder.qualifiers).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
    this.isAdmin = builder.isAdmin;
    this.scope = builder.scope;
  }

  public static Builder builder(String key) {
    return new Builder(key);
  }

  public String getKey() {
    return key;
  }

  public String getPluginKey() {
    return key.substring(0, key.indexOf('/'));
  }

  public String getName() {
    return name;
  }

  public Set<Qualifier> getComponentQualifiers() {
    return qualifiers;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public Scope getScope() {
    return scope;
  }

  public enum Scope {
    GLOBAL, ORGANIZATION, COMPONENT
  }

  public enum Qualifier {
    PROJECT(org.sonar.api.resources.Qualifiers.PROJECT),
    MODULE(org.sonar.api.resources.Qualifiers.MODULE),
    APP(org.sonar.api.resources.Qualifiers.APP),
    VIEW(org.sonar.api.resources.Qualifiers.VIEW),
    SUB_VIEW(org.sonar.api.resources.Qualifiers.SUBVIEW);

    private final String key;

    Qualifier(String key) {
      this.key = key;
    }

    @CheckForNull
    public static Qualifier fromKey(String key) {
      return Arrays.stream(values())
        .filter(qualifier -> qualifier.key.equals(key))
        .findAny()
        .orElse(null);
    }

    public String getKey() {
      return key;
    }
  }

  public static class Builder {
    private final String key;
    private String name;
    private boolean isAdmin = false;
    private Scope scope = GLOBAL;
    private Qualifier[] qualifiers = new Qualifier[] {};

    /**
     * @param key It must respect the format plugin_key/page_identifier. Example: <code>my_plugin/my_page</code>
     */
    private Builder(String key) {
      requireNonNull(key, "Key must not be null");
      if (key.split("/").length != 2) {
        throw new IllegalArgumentException("Page key [" + key + "] is not valid. It must contain a single slash, for example my_plugin/my_page.");
      }
      this.key = requireNonNull(key, "Key must not be null");
    }

    /**
     * Page name displayed in the UI. Mandatory.
     */
    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    /**
     * if set to true, display the page in the administration section, depending on the scope
     */
    public Builder setAdmin(boolean admin) {
      this.isAdmin = admin;
      return this;
    }

    /**
     * Define where the page is displayed, either in the global menu or in a component page
     * @param scope - default is GLOBAL
     */
    public Builder setScope(Scope scope) {
      this.scope = requireNonNull(scope, "Scope must not be null");
      return this;
    }

    /**
     * Define the components where the page is displayed. If set, {@link #setScope(Scope)} must be set to COMPONENT
     * @see Qualifier
     */
    public Builder setComponentQualifiers(Qualifier... qualifiers) {
      this.qualifiers = requireNonNull(qualifiers);
      return this;
    }

    public Page build() {
      if (key == null || key.isEmpty()) {
        throw new IllegalArgumentException("Key must not be empty");
      }
      if (name == null || name.isEmpty()) {
        throw new IllegalArgumentException("Name must be defined and not empty");
      }
      if (qualifiers.length > 0 && !COMPONENT.equals(scope)) {
        throw new IllegalArgumentException(format("The scope must be '%s' when component qualifiers are provided", COMPONENT));
      }
      if (qualifiers.length == 0 && COMPONENT.equals(scope)) {
        qualifiers = Qualifier.values();
      }

      return new Page(this);
    }
  }
}
