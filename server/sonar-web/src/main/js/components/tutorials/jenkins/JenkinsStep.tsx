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
import { NumberedList, NumberedListItem, TutorialStep } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import { withCLanguageFeature } from '../../hoc/withCLanguageFeature';
import BuildConfigSelection from '../components/BuildConfigSelection';
import { BuildTools, TutorialConfig, TutorialModes } from '../types';
import CFamily from './buildtool-steps/CFamily';
import DotNet from './buildtool-steps/DotNet';
import Gradle from './buildtool-steps/Gradle';
import Maven from './buildtool-steps/Maven';
import Other from './buildtool-steps/Other';

const BUILDTOOL_COMPONENT_MAP: {
  [x in BuildTools]: React.ComponentType<React.PropsWithChildren<LanguageProps>>;
} = {
  [BuildTools.Maven]: Maven,
  [BuildTools.Gradle]: Gradle,
  [BuildTools.DotNet]: DotNet,
  [BuildTools.Cpp]: CFamily,
  [BuildTools.ObjectiveC]: CFamily,
  [BuildTools.Other]: Other,
};

export interface LanguageProps {
  baseUrl: string;
  component: Component;
  config: TutorialConfig;
}

export interface JenkinsfileStepProps {
  baseUrl: string;
  component: Component;
  hasCLanguageFeature: boolean;
  setDone: (done: boolean) => void;
}

export function JenkinsStep(props: Readonly<JenkinsfileStepProps>) {
  const { component, hasCLanguageFeature, baseUrl, setDone } = props;

  const [config, setConfig] = React.useState<TutorialConfig>({});

  React.useEffect(() => {
    setDone(Boolean(config.buildTool));
  }, [config.buildTool, setDone]);

  const BuildToolComponent = config.buildTool && BUILDTOOL_COMPONENT_MAP[config.buildTool];

  return (
    <TutorialStep title={translate('onboarding.tutorial.with.jenkins.jenkinsfile.title')}>
      <NumberedList>
        <NumberedListItem>
          <BuildConfigSelection
            ci={TutorialModes.Jenkins}
            config={config}
            supportCFamily={hasCLanguageFeature}
            onSetConfig={setConfig}
          />
        </NumberedListItem>
        {BuildToolComponent && (
          <BuildToolComponent config={config} component={component} baseUrl={baseUrl} />
        )}
      </NumberedList>
    </TutorialStep>
  );
}

export default withCLanguageFeature(JenkinsStep);
