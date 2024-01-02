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
import * as React from 'react';
import withLanguagesContext from '../../../app/components/languages/withLanguagesContext';
import { translate } from '../../../helpers/l10n';
import { Languages } from '../../../types/languages';
import { Component } from '../../../types/types';
import RenderOptions from '../components/RenderOptions';
import { BuildTools } from '../types';
import AnalysisCommand from './commands/AnalysisCommand';

export interface BranchesAnalysisStepProps {
  languages: Languages;
  component: Component;

  onStepValidationChange: (isValid: boolean) => void;
}

const BUILD_TOOLS_ORDERED: Array<BuildTools> = [
  BuildTools.DotNet,
  BuildTools.Maven,
  BuildTools.Gradle,
  BuildTools.CFamily,
  BuildTools.Other,
];

export function BranchAnalysisStepContent(props: BranchesAnalysisStepProps) {
  const { component, onStepValidationChange, languages } = props;

  const [buildTechnology, setBuildTechnology] = React.useState<BuildTools | undefined>();
  const buildToolsList = languages['c']
    ? BUILD_TOOLS_ORDERED
    : BUILD_TOOLS_ORDERED.filter((t) => t !== BuildTools.CFamily);
  return (
    <>
      <span>{translate('onboarding.build')}</span>
      <RenderOptions
        label={translate('onboarding.build')}
        checked={buildTechnology}
        onCheck={(value) => setBuildTechnology(value as BuildTools)}
        optionLabelKey="onboarding.build"
        options={buildToolsList}
      />
      <AnalysisCommand
        onStepValidationChange={onStepValidationChange}
        buildTool={buildTechnology}
        projectKey={component.key}
      />
    </>
  );
}

export default withLanguagesContext(BranchAnalysisStepContent);
