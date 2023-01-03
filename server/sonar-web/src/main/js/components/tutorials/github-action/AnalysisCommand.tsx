/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import * as React from 'react';
import { withAppState } from '../../hoc/withAppState';
import { BuildTools } from '../types';
import DotNet from './commands/DotNet';
import Gradle from './commands/Gradle';
import JavaMaven from './commands/JavaMaven';
import Others from './commands/Others';

export interface AnalysisCommandProps {
  appState: T.AppState;
  buildTool?: BuildTools;
  component: T.Component;
}

export function AnalysisCommand(props: AnalysisCommandProps) {
  const {
    buildTool,
    component,
    appState: { branchesEnabled }
  } = props;

  if (!buildTool) {
    return null;
  }

  switch (buildTool) {
    case BuildTools.Maven:
      return <JavaMaven branchesEnabled={branchesEnabled} component={component} />;
    case BuildTools.Gradle:
      return <Gradle branchesEnabled={branchesEnabled} component={component} />;
    case BuildTools.DotNet:
      return <DotNet branchesEnabled={branchesEnabled} component={component} />;
    case BuildTools.Other:
      return <Others branchesEnabled={branchesEnabled} component={component} />;
  }
  return null;
}

export default withAppState(AnalysisCommand);
