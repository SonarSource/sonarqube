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
package org.sonar.plugins.squid;

import java.util.Collection;

import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SquidSearch;
import org.sonar.squid.Squid;
import org.sonar.squid.api.Query;
import org.sonar.squid.api.SourceCode;

public class SquidSearchProxy implements SquidSearch, BatchExtension {

  private Squid target;

  public Squid getTarget() {
    return target;
  }

  public void setTarget(Squid target) {
    this.target = target;
  }

  public Collection<SourceCode> search(Query... query) {
    checkTarget();
    return target.search(query);
  }

  public SourceCode search(String key) {
    checkTarget();
    return target.search(key);
  }

  private void checkTarget() {
    if (target == null) {
      throw new IllegalStateException("Squid service is not initialized");
    }
  }
}
