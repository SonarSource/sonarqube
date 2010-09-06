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
package org.sonar.core.plugin;

import org.sonar.api.database.BaseIdentifiable;

import javax.persistence.*;

/**
 * @since 2.2
 */
@Entity
@Table(name = "plugin_files")
public class JpaPluginFile extends BaseIdentifiable {

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "plugin_id")
  private JpaPlugin plugin;

  @Column(name = "filename", updatable = true, nullable = false, length = 100)
  private String filename;

  public JpaPluginFile() {
  }

  public JpaPluginFile(JpaPlugin plugin, String filename) {
    this.plugin = plugin;
    this.filename = filename;
  }

  public JpaPlugin getPlugin() {
    return plugin;
  }

  public String getPluginKey() {
    return plugin.getKey();
  }

  public void setPlugin(JpaPlugin plugin) {
    this.plugin = plugin;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getPath() {
    return new StringBuilder()
        .append(plugin.getKey())
        .append("/")
        .append(filename).toString();
  }
}
