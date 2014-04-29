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
package org.sonar.server.rule2;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;

import java.util.Date;
import java.util.List;

public class RuleImpl implements Rule {



  @Override
  public RuleKey key() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String language() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String name() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String description() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Severity severity() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String status() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean template() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<String> tags() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<RuleParam> params() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String debtCharacteristicKey() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String debtSubCharacteristicKey() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DebtRemediationFunction debtRemediationFunction() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Date createdAt() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Date updatedAt() {
    // TODO Auto-generated method stub
    return null;
  }

}
