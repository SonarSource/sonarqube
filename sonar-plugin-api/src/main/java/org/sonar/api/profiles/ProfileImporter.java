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
package org.sonar.api.profiles;

import java.io.Reader;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.ValidationMessages;

/**
 * Create a quality profile from an external rules file.
 *
 * @since 2.3
 */
@ServerSide
@ExtensionPoint
public abstract class ProfileImporter {

  private String[] supportedLanguages = new String[0];
  private String importerKey;
  private String importerName;

  protected ProfileImporter(String key, String name) {
    this.importerKey = key;
    this.importerName = name;
  }

  /**
   * Import the profile from a reader.
   *
   * {@link ValidationMessages#warnings} can be used to return some warnings to the user, for instance when some rules doesn't exist.
   * {@link ValidationMessages#errors} can be used when an unrecoverable error is generating during import. No quality profile will be created.
   */
  public abstract RulesProfile importProfile(Reader reader, ValidationMessages messages);

  public String getKey() {
    return importerKey;
  }

  public final ProfileImporter setKey(String s) {
    this.importerKey = s;
    return this;
  }

  public final String getName() {
    return importerName;
  }

  public final ProfileImporter setName(String s) {
    this.importerName = s;
    return this;
  }

  /**
   * Set the list of languages supported
   * An empty value means that it will be available for every languages.
   */
  protected final ProfileImporter setSupportedLanguages(String... languages) {
    supportedLanguages = (languages != null) ? languages : new String[0];
    return this;
  }

  /**
   * @return if empty, then any languages are supported.
   */
  public String[] getSupportedLanguages() {
    return supportedLanguages;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProfileImporter that = (ProfileImporter) o;
    return !((importerKey != null) ? !importerKey.equals(that.importerKey) : (that.importerKey != null));
  }

  @Override
  public final int hashCode() {
    return (importerKey != null) ? importerKey.hashCode() : 0;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
      .append("key", importerKey)
      .append("name", importerName)
      .append("languages", supportedLanguages)
      .toString();
  }
}
