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
import { Alert } from '../../../components/ui/Alert';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import { withCLanguageFeature } from '../../hoc/withCLanguageFeature';
import RenderOptions from '../components/RenderOptions';
import Step from '../components/Step';
import { BuildTools } from '../types';
import CFamilly from './buildtool-steps/CFamilly';
import DotNet from './buildtool-steps/DotNet';
import Gradle from './buildtool-steps/Gradle';
import Maven from './buildtool-steps/Maven';
import Other from './buildtool-steps/Other';

export interface JenkinsfileStepProps {
  baseUrl: string;
  component: Component;
  hasCLanguageFeature: boolean;
  finished: boolean;
  onDone: () => void;
  onOpen: () => void;
  open: boolean;
}

export interface LanguageProps {
  onDone: () => void;
  component: Component;
  baseUrl: string;
}

const BUILDTOOL_COMPONENT_MAP: {
  [x in BuildTools]: React.ComponentType<LanguageProps>;
} = {
  [BuildTools.Maven]: Maven,
  [BuildTools.Gradle]: Gradle,
  [BuildTools.DotNet]: DotNet,
  [BuildTools.CFamily]: CFamilly,
  [BuildTools.Other]: Other,
};

export function JenkinsfileStep(props: JenkinsfileStepProps) {
  const { component, hasCLanguageFeature, baseUrl, finished, open } = props;
  const [buildTool, setBuildTool] = React.useState<BuildTools>();
  const buildToolOrder = Object.keys(BUILDTOOL_COMPONENT_MAP);
  if (!hasCLanguageFeature) {
    buildToolOrder.splice(buildToolOrder.indexOf(BuildTools.CFamily), 1);
  }
  return (
    <Step
      finished={finished}
      onOpen={props.onOpen}
      open={open}
      renderForm={() => (
        <div className="boxed-group-inner">
          <ol className="list-styled">
            <li>
              {translate('onboarding.build')}
              <RenderOptions
                label={translate('onboarding.build')}
                checked={buildTool}
                onCheck={(value) => setBuildTool(value as BuildTools)}
                optionLabelKey="onboarding.build"
                options={buildToolOrder}
              />
              {buildTool === BuildTools.CFamily && (
                <Alert variant="info" className="spacer-top abs-width-600">
                  {translate('onboarding.tutorial.with.jenkins.jenkinsfile.cfamilly.agent_setup')}
                </Alert>
              )}
            </li>
            {buildTool !== undefined &&
              React.createElement(BUILDTOOL_COMPONENT_MAP[buildTool], {
                component,
                baseUrl,
                onDone: props.onDone,
              })}
          </ol>
        </div>
      )}
      stepNumber={3}
      stepTitle={translate('onboarding.tutorial.with.jenkins.jenkinsfile.title')}
    />
  );
}

export default withCLanguageFeature(JenkinsfileStep);
