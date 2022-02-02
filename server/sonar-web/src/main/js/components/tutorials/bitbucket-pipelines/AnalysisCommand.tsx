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
import { Dictionary } from 'lodash';
import * as React from 'react';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import { AppState, Component } from '../../../types/types';
import { CompilationInfo } from '../components/CompilationInfo';
import CreateYmlFile from '../components/CreateYmlFile';
import { BuildTools } from '../types';
import cFamilyExample from './commands/CFamily';
import dotNetExample from './commands/DotNet';
import gradleExample from './commands/Gradle';
import mavenExample from './commands/Maven';
import othersExample from './commands/Others';
import { PreambuleYaml } from './PreambuleYaml';

export interface AnalysisCommandProps {
  appState: AppState;
  buildTool: BuildTools;
  component: Component;
}

const YamlTemplate: Dictionary<(branchesEnabled?: boolean, projectKey?: string) => string> = {
  [BuildTools.Gradle]: gradleExample,
  [BuildTools.Maven]: mavenExample,
  [BuildTools.DotNet]: dotNetExample,
  [BuildTools.CFamily]: cFamilyExample,
  [BuildTools.Other]: othersExample
};

export function AnalysisCommand(props: AnalysisCommandProps) {
  const {
    buildTool,
    component,
    appState: { branchesEnabled }
  } = props;

  if (!buildTool) {
    return null;
  }

  const yamlTemplate = YamlTemplate[buildTool](branchesEnabled, component.key);

  return (
    <>
      <PreambuleYaml buildTool={buildTool} component={component} />
      <CreateYmlFile yamlFileName="bitbucket-pipelines.yml" yamlTemplate={yamlTemplate} />
      {buildTool === BuildTools.CFamily && <CompilationInfo className="abs-width-800" />}
    </>
  );
}

export default withAppStateContext(AnalysisCommand);
