/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import { AppState, Component } from '../../../types/types';
import { BuildTools } from '../types';
import CFamily from './commands/CFamily';
import DotNet from './commands/DotNet';
import Gradle from './commands/Gradle';
import JavaMaven from './commands/JavaMaven';
import Others from './commands/Others';

export interface AnalysisCommandProps {
  appState: AppState;
  buildTool: BuildTools;
  component: Component;
  onDone: () => void;
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
      return (
        <JavaMaven branchesEnabled={branchesEnabled} component={component} onDone={props.onDone} />
      );
    case BuildTools.Gradle:
      return (
        <Gradle branchesEnabled={branchesEnabled} component={component} onDone={props.onDone} />
      );
    case BuildTools.DotNet:
      return (
        <DotNet branchesEnabled={branchesEnabled} component={component} onDone={props.onDone} />
      );
    case BuildTools.CFamily:
      return (
        <CFamily branchesEnabled={branchesEnabled} component={component} onDone={props.onDone} />
      );
    case BuildTools.Other:
      return (
        <Others branchesEnabled={branchesEnabled} component={component} onDone={props.onDone} />
      );
  }
  return null;
}

export default withAppStateContext(AnalysisCommand);
