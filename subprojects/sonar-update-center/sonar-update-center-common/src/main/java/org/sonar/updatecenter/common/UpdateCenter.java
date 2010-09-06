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
package org.sonar.updatecenter.common;

import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public final class UpdateCenter {

  private Sonar sonar = new Sonar();
  private Set<Plugin> plugins = new HashSet<Plugin>();
  private Date date;

  public UpdateCenter() {
    this(new Date());
  }

  public UpdateCenter(Date date) {
    this.date = date;
  }

  public Set<Plugin> getPlugins() {
    return plugins;
  }

  public Plugin getPlugin(String key) {
    for (Plugin plugin : plugins) {
      if (StringUtils.equals(key, plugin.getKey())) {
        return plugin;
      }
    }
    return null;
  }

  public UpdateCenter setPlugins(Collection<Plugin> plugins) {
    this.plugins.clear();
    this.plugins.addAll(plugins);
    return this;
  }

  public UpdateCenter addPlugin(Plugin plugin) {
    this.plugins.add(plugin);
    return this;
  }

  public Sonar getSonar() {
    return sonar;
  }

  public UpdateCenter setSonar(Sonar sonar) {
    this.sonar = sonar;
    return this;
  }

  public Date getDate() {
    return date;
  }

  public UpdateCenter setDate(Date date) {
    this.date = date;
    return this;
  }
}
