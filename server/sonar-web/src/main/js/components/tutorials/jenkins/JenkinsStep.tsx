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
import { FlagMessage, NumberedList, NumberedListItem, TutorialStep } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import { withCLanguageFeature } from '../../hoc/withCLanguageFeature';
import RenderOptions from '../components/RenderOptions';
import { BuildTools } from '../types';
import CFamilly from './buildtool-steps/CFamilly';
import DotNet from './buildtool-steps/DotNet';
import Gradle from './buildtool-steps/Gradle';
import Maven from './buildtool-steps/Maven';
import Other from './buildtool-steps/Other';

const BUILD_TOOLS_WITH_NO_ADDITIONAL_OPTIONS = [
  BuildTools.Maven,
  BuildTools.Gradle,
  BuildTools.Other,
];

const BUILDTOOL_COMPONENT_MAP: {
  [x in BuildTools]: React.ComponentType<
    React.PropsWithChildren<React.PropsWithChildren<LanguageProps>>
  >;
} = {
  [BuildTools.Maven]: Maven,
  [BuildTools.Gradle]: Gradle,
  [BuildTools.DotNet]: DotNet,
  [BuildTools.CFamily]: CFamilly,
  [BuildTools.Other]: Other,
};

export interface JenkinsfileStepProps {
  baseUrl: string;
  component: Component;
  hasCLanguageFeature: boolean;
  onDone: (done: boolean) => void;
}

export interface LanguageProps {
  onDone: (done: boolean) => void;
  component: Component;
  baseUrl: string;
}

export function JenkinsfileStep(props: JenkinsfileStepProps) {
  const { component, hasCLanguageFeature, baseUrl, onDone } = props;

  const [buildTool, setBuildTool] = React.useState<BuildTools>();

  const buildToolOrder = Object.keys(BUILDTOOL_COMPONENT_MAP);
  if (!hasCLanguageFeature) {
    buildToolOrder.splice(buildToolOrder.indexOf(BuildTools.CFamily), 1);
  }

  const BuildToolComponent = buildTool ? BUILDTOOL_COMPONENT_MAP[buildTool] : undefined;

  React.useEffect(() => {
    if (buildTool && BUILD_TOOLS_WITH_NO_ADDITIONAL_OPTIONS.includes(buildTool)) {
      onDone(true);
    }
  }, [buildTool, onDone]);

  return (
    <TutorialStep title={translate('onboarding.tutorial.with.jenkins.jenkinsfile.title')}>
      <NumberedList>
        <NumberedListItem>
          {translate('onboarding.build')}
          <RenderOptions
            label={translate('onboarding.build')}
            checked={buildTool}
            onCheck={(value) => setBuildTool(value as BuildTools)}
            optionLabelKey="onboarding.build"
            options={buildToolOrder}
          />
          {buildTool === BuildTools.CFamily && (
            <FlagMessage variant="info" className="sw-mt-2 sw-w-abs-600">
              {translate('onboarding.tutorial.with.jenkins.jenkinsfile.cfamilly.agent_setup')}
            </FlagMessage>
          )}
        </NumberedListItem>
        {BuildToolComponent !== undefined && (
          <BuildToolComponent component={component} baseUrl={baseUrl} onDone={props.onDone} />
        )}
      </NumberedList>
    </TutorialStep>
  );
}

export default withCLanguageFeature(JenkinsfileStep);
