/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import RenderOptions from '../components/RenderOptions';
import SentenceWithHighlights from '../components/SentenceWithHighlights';
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

const BUILDTOOL_COMPONENT_MAP: {
  [x in BuildTools]: React.ComponentType<{ component: T.Component }>;
} = {
  [BuildTools.Maven]: Maven,
  [BuildTools.Gradle]: Gradle,
  [BuildTools.DotNet]: DotNet,
  [BuildTools.Other]: Other
};

export default function JenkinsfileStep(props: JenkinsfileStepProps) {
  const { component, open } = props;
  const [buildTool, setBuildTool] = React.useState<BuildTools | undefined>(undefined);
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
                onCheck={value => setBuildTool(value as BuildTools)}
                optionLabelKey="onboarding.build"
                options={Object.values(BuildTools)}
              />
            </li>
            {buildTool !== undefined &&
              React.createElement(BUILDTOOL_COMPONENT_MAP[buildTool], { component })}
          </ol>
          {buildTool !== undefined && (
            <>
              <hr className="huge-spacer-top huge-spacer-bottom" />
              <div className="abs-width-600">
                <p className="big-spacer-bottom">
                  <SentenceWithHighlights
                    highlightKeys={['all_set']}
                    translationKey="onboarding.tutorial.with.jenkins.all_set"
                  />
                </p>
                <div className="display-flex-row big-spacer-bottom">
                  <div>
                    <img
                      alt="" // Should be ignored by screen readers
                      className="big-spacer-right"
                      width={30}
                      src={`${getBaseUrl()}/images/tutorials/commit.svg`}
                    />
                  </div>
                  <div>
                    <p className="little-spacer-bottom">
                      <strong>{translate('onboarding.tutorial.with.jenkins.commit')}</strong>
                    </p>
                    <p>{translate('onboarding.tutorial.with.jenkins.commit.why')}</p>
                  </div>
                </div>
                <div className="display-flex-row huge-spacer-bottom">
                  <div>
                    <img
                      alt="" // Should be ignored by screen readers
                      className="big-spacer-right"
                      width={30}
                      src={`${getBaseUrl()}/images/tutorials/refresh.svg`}
                    />
                  </div>
                  <div>
                    <p className="little-spacer-bottom">
                      <strong>{translate('onboarding.tutorial.with.jenkins.refresh')}</strong>
                    </p>
                    <p>{translate('onboarding.tutorial.with.jenkins.refresh.why')}</p>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
      )}
      stepNumber={3}
      stepTitle={translate('onboarding.tutorial.with.jenkins.jenkinsfile.title')}
    />
  );
}
