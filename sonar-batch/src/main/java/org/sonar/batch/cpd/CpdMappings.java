/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.cpd;

import org.sonar.api.BatchSide;
import org.sonar.api.batch.CpdMapping;

import javax.annotation.CheckForNull;

@BatchSide
public class CpdMappings {

  private final CpdMapping[] mappings;

  public CpdMappings(CpdMapping[] mappings) {
    this.mappings = mappings;
  }

  public CpdMappings() {
    this(new CpdMapping[0]);
  }

  @CheckForNull
  public CpdMapping getMapping(String language) {
    if (mappings != null) {
      for (CpdMapping cpdMapping : mappings) {
        if (cpdMapping.getLanguage().getKey().equals(language)) {
          return cpdMapping;
        }
      }
    }
    return null;
  }

}
