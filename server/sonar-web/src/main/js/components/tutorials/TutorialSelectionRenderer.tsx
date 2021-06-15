/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { AlmKeys, AlmSettingsInstance, ProjectAlmBindingResponse } from '../../types/alm-settings';
import AzurePipelinesTutorial from './azure-pipelines/AzurePipelinesTutorial';
import BitbucketPipelinesTutorial from './bitbucket-pipelines/BitbucketPipelinesTutorial';
import GitHubActionTutorial from './github-action/GitHubActionTutorial';
import GitLabCITutorial from './gitlabci/GitLabCITutorial';
import JenkinsTutorial from './jenkins/JenkinsTutorial';
import ManualTutorial from './manual/ManualTutorial';
import { TutorialModes } from './types';

export interface TutorialSelectionRendererProps {
  almBinding?: AlmSettingsInstance;
  baseUrl: string;
  component: T.Component;
  currentUser: T.LoggedInUser;
  loading: boolean;
  onSelectTutorial: (mode: TutorialModes) => void;
  projectBinding?: ProjectAlmBindingResponse;
  selectedTutorial?: TutorialModes;
}

const DEFAULT_ICON_SIZE = 80;
const GH_ACTION_ICON_SIZE = 64;

function renderButton(
  mode: TutorialModes,
  onSelectTutorial: (mode: TutorialModes) => void,
  icon: React.ReactNode
) {
  return (
    <button
      className={`button button-huge display-flex-column spacer-left spacer-right huge-spacer-bottom tutorial-mode-${mode}`}
      // Currently, OtherCI is the same tutorial as Manual. We might update it to its own stand-alone
      // tutorial in the future.
      onClick={() => onSelectTutorial(mode === TutorialModes.OtherCI ? TutorialModes.Manual : mode)}
      type="button">
      {icon}
      <div className="medium big-spacer-top">
        {translate('onboarding.tutorial.choose_method', mode)}
      </div>
    </button>
  );
}

export default function TutorialSelectionRenderer(props: TutorialSelectionRendererProps) {
  const {
    almBinding,
    baseUrl,
    component,
    currentUser,
    loading,
    projectBinding,
    selectedTutorial
  } = props;

  if (loading) {
    return <i className="spinner" />;
  }

  let showGitHubActions = true;
  let showGitLabCICD = true;
  let showBitbucketPipelines = true;
  let showAzurePipelines = true;
  let showJenkins = true;

  if (projectBinding !== undefined) {
    showGitHubActions = projectBinding.alm === AlmKeys.GitHub;
    showGitLabCICD = projectBinding.alm === AlmKeys.GitLab;
    showBitbucketPipelines = projectBinding?.alm === AlmKeys.BitbucketCloud;
    showAzurePipelines = [AlmKeys.Azure, AlmKeys.GitHub].includes(projectBinding.alm);
    showJenkins = [
      AlmKeys.BitbucketCloud,
      AlmKeys.BitbucketServer,
      AlmKeys.GitHub,
      AlmKeys.GitLab
    ].includes(projectBinding.alm);
  }

  return (
    <>
      {selectedTutorial === undefined && (
        <div className="tutorial-selection">
          <header className="spacer-top spacer-bottom padded">
            <h1 className="text-center big-spacer-bottom">
              {translate('onboarding.tutorial.choose_method')}
            </h1>
          </header>

          <div className="display-flex-justify-center display-flex-wrap">
            {renderButton(
              TutorialModes.Manual,
              props.onSelectTutorial,
              <img
                alt="" // Should be ignored by screen readers
                height={DEFAULT_ICON_SIZE}
                src={`${getBaseUrl()}/images/tutorials/manual.svg`}
              />
            )}

            {showAzurePipelines &&
              renderButton(
                TutorialModes.AzurePipelines,
                props.onSelectTutorial,
                <img
                  alt="" // Should be ignored by screen readers
                  height={DEFAULT_ICON_SIZE}
                  src={`${getBaseUrl()}/images/tutorials/azure-pipelines.svg`}
                />
              )}

            {showBitbucketPipelines &&
              renderButton(
                TutorialModes.BitbucketPipelines,
                props.onSelectTutorial,
                <img
                  alt="" // Should be ignored by screen readers
                  height={DEFAULT_ICON_SIZE}
                  src={`${getBaseUrl()}/images/alm/bitbucket.svg`}
                />
              )}

            {showGitHubActions &&
              renderButton(
                TutorialModes.GitHubActions,
                props.onSelectTutorial,
                <img
                  alt="" // Should be ignored by screen readers
                  height={GH_ACTION_ICON_SIZE}
                  className="spacer-bottom spacer-top"
                  src={`${getBaseUrl()}/images/tutorials/github-actions.svg`}
                />
              )}

            {showGitLabCICD &&
              renderButton(
                TutorialModes.GitLabCI,
                props.onSelectTutorial,
                <img
                  alt="" // Should be ignored by screen readers
                  height={DEFAULT_ICON_SIZE}
                  src={`${getBaseUrl()}/images/alm/gitlab.svg`}
                />
              )}

            {showJenkins &&
              renderButton(
                TutorialModes.Jenkins,
                props.onSelectTutorial,
                <img
                  alt="" // Should be ignored by screen readers
                  height={DEFAULT_ICON_SIZE}
                  src={`${getBaseUrl()}/images/tutorials/jenkins.svg`}
                />
              )}

            {renderButton(
              TutorialModes.OtherCI,
              props.onSelectTutorial,
              <span
                aria-disabled={true}
                className="display-flex-center gigantic"
                style={{ height: DEFAULT_ICON_SIZE }}>
                &hellip;
              </span>
            )}
          </div>
        </div>
      )}

      {selectedTutorial === TutorialModes.Manual && (
        <ManualTutorial component={component} currentUser={currentUser} />
      )}

      {selectedTutorial === TutorialModes.BitbucketPipelines && (
        <BitbucketPipelinesTutorial
          almBinding={almBinding}
          baseUrl={baseUrl}
          component={component}
          currentUser={currentUser}
          projectBinding={projectBinding}
        />
      )}

      {selectedTutorial === TutorialModes.GitHubActions && (
        <GitHubActionTutorial
          almBinding={almBinding}
          baseUrl={baseUrl}
          component={component}
          currentUser={currentUser}
          projectBinding={projectBinding}
        />
      )}

      {selectedTutorial === TutorialModes.Jenkins && (
        <JenkinsTutorial
          almBinding={almBinding}
          component={component}
          projectBinding={projectBinding}
        />
      )}

      {selectedTutorial === TutorialModes.GitLabCI && (
        <GitLabCITutorial baseUrl={baseUrl} component={component} currentUser={currentUser} />
      )}

      {selectedTutorial === TutorialModes.AzurePipelines && (
        <AzurePipelinesTutorial baseUrl={baseUrl} component={component} currentUser={currentUser} />
      )}
    </>
  );
}
