/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import withLanguagesContext from '../../../app/components/languages/withLanguagesContext';
import { Languages } from '../../../types/languages';
import { Component } from '../../../types/types';
import BuildConfigSelection from '../components/BuildConfigSelection';
import { TutorialConfig, TutorialModes } from '../types';
import AnalysisCommand from './commands/AnalysisCommand';

export interface BranchesAnalysisStepProps {
  component: Component;
  config: TutorialConfig;
  languages: Languages;
  setConfig: (config: TutorialConfig) => void;
}

export function BranchAnalysisStepContent(props: BranchesAnalysisStepProps) {
  const { config, setConfig, component, languages } = props;

  return (
    <>
      <BuildConfigSelection
        ci={TutorialModes.AzurePipelines}
        config={config}
        supportCFamily={Boolean(languages['c'])}
        onSetConfig={setConfig}
      />

      <AnalysisCommand config={config} projectKey={component.key} projectName={component.name} />
    </>
  );
}

export default withLanguagesContext(BranchAnalysisStepContent);
