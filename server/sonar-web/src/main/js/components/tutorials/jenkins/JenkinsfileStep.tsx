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
import { translate } from 'sonar-ui-common/helpers/l10n';
import AllSet from '../components/AllSet';
import RenderOptions from '../components/RenderOptions';
import Step from '../components/Step';
import { BuildTools } from '../types';
import DotNet from './buildtool-steps/DotNet';
import Gradle from './buildtool-steps/Gradle';
import Maven from './buildtool-steps/Maven';
import Other from './buildtool-steps/Other';

export interface JenkinsfileStepProps {
  component: T.Component;
  open: boolean;
}

// To remove when CFamily is includ in this tutorial
type BuildToolsWithoutCFamily = Exclude<BuildTools, BuildTools.CFamily>;

const BUILDTOOL_COMPONENT_MAP: {
  [x in BuildToolsWithoutCFamily]: React.ComponentType<{ component: T.Component }>;
} = {
  [BuildTools.Maven]: Maven,
  [BuildTools.Gradle]: Gradle,
  [BuildTools.DotNet]: DotNet,
  [BuildTools.Other]: Other
};

export default function JenkinsfileStep(props: JenkinsfileStepProps) {
  const { component, open } = props;
  const [buildTool, setBuildTool] = React.useState<BuildToolsWithoutCFamily | undefined>(undefined);
  return (
    <Step
      finished={false}
      open={open}
      renderForm={() => (
        <div className="boxed-group-inner">
          <ol className="list-styled">
            <li>
              {translate('onboarding.build')}
              <RenderOptions
                checked={buildTool}
                name="buildtool"
                onCheck={value => setBuildTool(value as BuildToolsWithoutCFamily)}
                optionLabelKey="onboarding.build"
                options={Object.keys(BUILDTOOL_COMPONENT_MAP)}
              />
            </li>
            {buildTool !== undefined &&
              React.createElement(BUILDTOOL_COMPONENT_MAP[buildTool], { component })}
          </ol>
          {buildTool !== undefined && (
            <>
              <hr className="huge-spacer-top huge-spacer-bottom" />
              <AllSet />
            </>
          )}
        </div>
      )}
      stepNumber={3}
      stepTitle={translate('onboarding.tutorial.with.jenkins.jenkinsfile.title')}
    />
  );
}
