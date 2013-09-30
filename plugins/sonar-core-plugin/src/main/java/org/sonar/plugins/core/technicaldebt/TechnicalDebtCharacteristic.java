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
package org.sonar.plugins.core.technicaldebt;

import com.google.common.collect.Lists;

import javax.annotation.Nullable;

import java.util.List;

public final class TechnicalDebtCharacteristic implements Characteristicable {

  private String key;
  private org.sonar.api.qualitymodel.Characteristic characteristic;
  private TechnicalDebtCharacteristic parent = null;
  private List<TechnicalDebtCharacteristic> subCharacteristics = Lists.newArrayList();
  private List<TechnicalDebtRequirement> requirements = Lists.newArrayList();

  public TechnicalDebtCharacteristic(org.sonar.api.qualitymodel.Characteristic c) {
    this(c, null);
  }

  public TechnicalDebtCharacteristic(org.sonar.api.qualitymodel.Characteristic c, @Nullable TechnicalDebtCharacteristic parent) {
    this.characteristic = c;
    this.key = c.getKey();
    this.parent = parent;
    for (org.sonar.api.qualitymodel.Characteristic subc : c.getChildren()) {
      if (subc.getEnabled()) {
        if (subc.getRule() != null) {
          requirements.add(new TechnicalDebtRequirement(subc, this));
        } else {
          subCharacteristics.add(new TechnicalDebtCharacteristic(subc, this));
        }
      }
    }
  }

  public String getKey() {
    return key;
  }

  public List<TechnicalDebtCharacteristic> getSubCharacteristics() {
    return subCharacteristics;
  }

  public TechnicalDebtCharacteristic getParent() {
    return parent;
  }

  public List<TechnicalDebtRequirement> getRequirements() {
    return requirements;
  }

  public org.sonar.api.qualitymodel.Characteristic toCharacteristic() {
    return characteristic;
  }
}
