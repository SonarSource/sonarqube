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
import { AlmBindingDefinition, AlmKeys, ProjectAlmBindingResponse } from '../../types/alm-settings';
import GitLabCITutorial from './gitlabci/GitLabCITutorial';
import JenkinsTutorial from './jenkins/JenkinsTutorial';
import ManualTutorial from './manual/ManualTutorial';
import { TutorialModes } from './types';

export interface TutorialSelectionRendererProps {
  almBinding?: AlmBindingDefinition;
  component: T.Component;
  currentUser: T.LoggedInUser;
  loading: boolean;
  onSelectTutorial: (mode: TutorialModes) => void;
  projectBinding?: ProjectAlmBindingResponse;
  selectedTutorial?: TutorialModes;
}

export default function TutorialSelectionRenderer(props: TutorialSelectionRendererProps) {
  const { almBinding, component, currentUser, loading, projectBinding, selectedTutorial } = props;

  if (loading) {
    return <i className="spinner" />;
  }

  const jenkinsAvailable =
    projectBinding && [AlmKeys.Bitbucket, AlmKeys.GitHub].includes(projectBinding.alm);

  return (
    <>
      {selectedTutorial === undefined && (
        <div className="tutorial-selection">
          <header className="spacer-top spacer-bottom padded">
            <h1 className="text-center big-spacer-bottom">
              {translate('onboarding.tutorial.choose_method')}
            </h1>
          </header>

          <div className="display-flex-justify-center">
            {projectBinding?.alm === AlmKeys.GitLab && (
              <button
                className="button button-huge display-flex-column spacer-left spacer-right tutorial-mode-gitlab"
                onClick={() => props.onSelectTutorial(TutorialModes.GitLabCI)}
                type="button">
                <img
                  alt="" // Should be ignored by screen readers
                  height={80}
                  src={`${getBaseUrl()}/images/alm/gitlab.svg`}
                />
                <div className="medium big-spacer-top">
                  {translate('onboarding.tutorial.choose_method.gitlab_ci')}
                </div>
              </button>
            )}

            {jenkinsAvailable && (
              <button
                className="button button-huge display-flex-column spacer-left spacer-right tutorial-mode-jenkins"
                onClick={() => props.onSelectTutorial(TutorialModes.Jenkins)}
                type="button">
                <img
                  alt="" // Should be ignored by screen readers
                  height={80}
                  src={`${getBaseUrl()}/images/tutorials/jenkins.svg`}
                />
                <div className="medium big-spacer-top">
                  {translate('onboarding.tutorial.choose_method.jenkins')}
                </div>
              </button>
            )}

            <button
              className="button button-huge display-flex-column spacer-left spacer-right tutorial-mode-manual"
              onClick={() => props.onSelectTutorial(TutorialModes.Manual)}
              type="button">
              <img
                alt="" // Should be ignored by screen readers
                height={80}
                src={`${getBaseUrl()}/images/sonarcloud/analysis/manual.svg`}
              />
              <div className="medium big-spacer-top">
                {translate('onboarding.tutorial.choose_method.manual')}
              </div>
            </button>
          </div>
        </div>
      )}

      {selectedTutorial === TutorialModes.Manual && (
        <ManualTutorial component={component} currentUser={currentUser} />
      )}

      {selectedTutorial === TutorialModes.Jenkins && projectBinding !== undefined && (
        <JenkinsTutorial
          almBinding={almBinding}
          component={component}
          projectBinding={projectBinding}
        />
      )}

      {selectedTutorial === TutorialModes.GitLabCI && projectBinding !== undefined && (
        <GitLabCITutorial
          component={component}
          currentUser={currentUser}
          projectBinding={projectBinding}
        />
      )}
    </>
  );
}
