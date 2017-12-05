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
package org.sonar.server.qualitygate;

import org.sonar.core.platform.Module;
import org.sonar.server.qualitygate.ws.CopyAction;
import org.sonar.server.qualitygate.ws.CreateAction;
import org.sonar.server.qualitygate.ws.CreateConditionAction;
import org.sonar.server.qualitygate.ws.DeleteConditionAction;
import org.sonar.server.qualitygate.ws.DeselectAction;
import org.sonar.server.qualitygate.ws.DestroyAction;
import org.sonar.server.qualitygate.ws.GetByProjectAction;
import org.sonar.server.qualitygate.ws.ListAction;
import org.sonar.server.qualitygate.ws.ProjectStatusAction;
import org.sonar.server.qualitygate.ws.QualityGatesWs;
import org.sonar.server.qualitygate.ws.QualityGatesWsSupport;
import org.sonar.server.qualitygate.ws.RenameAction;
import org.sonar.server.qualitygate.ws.SearchAction;
import org.sonar.server.qualitygate.ws.SelectAction;
import org.sonar.server.qualitygate.ws.SetAsDefaultAction;
import org.sonar.server.qualitygate.ws.ShowAction;
import org.sonar.server.qualitygate.ws.UpdateConditionAction;

public class QualityGateModule extends Module {
  @Override
  protected void configureModule() {
    add(
      QualityGateUpdater.class,
      QualityGateConditionsUpdater.class,
      QualityGateFinder.class,
      QualityGateEvaluatorImpl.class,
      // WS
      QualityGatesWsSupport.class,
      QualityGatesWs.class,
      ListAction.class,
      SearchAction.class,
      ShowAction.class,
      CreateAction.class,
      RenameAction.class,
      CopyAction.class,
      DestroyAction.class,
      SetAsDefaultAction.class,
      SelectAction.class,
      DeselectAction.class,
      CreateConditionAction.class,
      DeleteConditionAction.class,
      UpdateConditionAction.class,
      ProjectStatusAction.class,
      GetByProjectAction.class);
  }
}
