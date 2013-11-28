/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.batch.technicaldebt;

import org.picocontainer.injectors.ProviderAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.api.utils.TimeProfiler;

public class TechnicalDebtModelProvider extends ProviderAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(TechnicalDebtModelProvider.class);

  private TechnicalDebtModel model;

  public TechnicalDebtModel provide(TechnicalDebtModelLoader loader) {
    if (model == null) {
      TimeProfiler profiler = new TimeProfiler(LOG).start("Loading technical debt model");
      model = loader.load();
      profiler.stop();
    }
    return model;
  }
}
