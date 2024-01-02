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
import EllipsisIcon from '../../components/icons/EllipsisIcon';
import { translate } from '../../helpers/l10n';
import { getBaseUrl } from '../../helpers/system';
import { AlmKeys, AlmSettingsInstance, ProjectAlmBindingResponse } from '../../types/alm-settings';
import { Component } from '../../types/types';
import { LoggedInUser } from '../../types/users';
import { Alert } from '../ui/Alert';
import AzurePipelinesTutorial from './azure-pipelines/AzurePipelinesTutorial';
import BitbucketPipelinesTutorial from './bitbucket-pipelines/BitbucketPipelinesTutorial';
import GitHubActionTutorial from './github-action/GitHubActionTutorial';
import GitLabCITutorial from './gitlabci/GitLabCITutorial';
import JenkinsTutorial from './jenkins/JenkinsTutorial';
import OtherTutorial from './other/OtherTutorial';
import { TutorialModes } from './types';

export interface TutorialSelectionRendererProps {
  almBinding?: AlmSettingsInstance;
  baseUrl: string;
  component: Component;
  currentUser: LoggedInUser;
  currentUserCanScanProject: boolean;
  loading: boolean;
  mainBranchName: string;
  onSelectTutorial: (mode: TutorialModes) => void;
  projectBinding?: ProjectAlmBindingResponse;
  selectedTutorial?: TutorialModes;
  willRefreshAutomatically?: boolean;
}

const DEFAULT_ICON_SIZE = 60;
const GH_ACTION_ICON_SIZE = 46;

function renderButton(
  mode: TutorialModes,
  onSelectTutorial: (mode: TutorialModes) => void,
  icon: React.ReactNode
) {
  return (
    <button
      className={`button button-huge display-flex-column big-spacer-right big-spacer-bottom tutorial-mode-${mode}`}
      // Currently, OtherCI is the same tutorial as Manual. We might update it to its own stand-alone
      // tutorial in the future.
      onClick={() => onSelectTutorial(mode)}
      type="button"
    >
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
    currentUserCanScanProject,
    loading,
    mainBranchName,
    projectBinding,
    selectedTutorial,
    willRefreshAutomatically,
  } = props;

  if (loading) {
    return <i aria-label={translate('loading')} className="spinner" />;
  }

  if (!currentUserCanScanProject) {
    return <Alert variant="warning">{translate('onboarding.tutorial.no_scan_rights')}</Alert>;
  }

  let showGitHubActions = true;
  let showGitLabCICD = true;
  let showBitbucketPipelines = true;
  let showAzurePipelines = true;
  let showJenkins = true;

  if (projectBinding !== undefined) {
    showGitHubActions = projectBinding.alm === AlmKeys.GitHub;
    showGitLabCICD = projectBinding.alm === AlmKeys.GitLab;
    showBitbucketPipelines = projectBinding.alm === AlmKeys.BitbucketCloud;
    showAzurePipelines = [AlmKeys.Azure, AlmKeys.GitHub].includes(projectBinding.alm);
    showJenkins = [
      AlmKeys.BitbucketCloud,
      AlmKeys.BitbucketServer,
      AlmKeys.GitHub,
      AlmKeys.GitLab,
    ].includes(projectBinding.alm);
  }

  return (
    <>
      {selectedTutorial === undefined && (
        <>
          <h2 className="spacer-top huge-spacer-bottom">
            {translate('onboarding.tutorial.choose_method')}
          </h2>

          <div className="tutorial-selection">
            <p className="big-spacer-bottom">
              {translate('onboarding.tutorial.choose_method.devops_platform.description')}
            </p>
            <div className="display-flex-start display-flex-wrap">
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

              {renderButton(
                TutorialModes.OtherCI,
                props.onSelectTutorial,
                <EllipsisIcon size={DEFAULT_ICON_SIZE} />
              )}
            </div>

            <p className="big-spacer-bottom spacer-top">
              {translate('onboarding.tutorial.choose_method.local.description')}
            </p>
            <div>
              {renderButton(
                TutorialModes.Local,
                props.onSelectTutorial,
                <img
                  alt="" // Should be ignored by screen readers
                  height={DEFAULT_ICON_SIZE}
                  src={`${getBaseUrl()}/images/tutorials/manual.svg`}
                />
              )}
            </div>
          </div>
        </>
      )}

      {selectedTutorial === TutorialModes.Local && (
        <OtherTutorial
          component={component}
          baseUrl={baseUrl}
          isLocal={true}
          currentUser={currentUser}
        />
      )}

      {selectedTutorial === TutorialModes.OtherCI && (
        <OtherTutorial component={component} baseUrl={baseUrl} currentUser={currentUser} />
      )}

      {selectedTutorial === TutorialModes.BitbucketPipelines && (
        <BitbucketPipelinesTutorial
          almBinding={almBinding}
          baseUrl={baseUrl}
          component={component}
          currentUser={currentUser}
          mainBranchName={mainBranchName}
          projectBinding={projectBinding}
          willRefreshAutomatically={willRefreshAutomatically}
        />
      )}

      {selectedTutorial === TutorialModes.GitHubActions && (
        <GitHubActionTutorial
          almBinding={almBinding}
          baseUrl={baseUrl}
          component={component}
          currentUser={currentUser}
          mainBranchName={mainBranchName}
          projectBinding={projectBinding}
          willRefreshAutomatically={willRefreshAutomatically}
        />
      )}

      {selectedTutorial === TutorialModes.Jenkins && (
        <JenkinsTutorial
          almBinding={almBinding}
          baseUrl={baseUrl}
          component={component}
          projectBinding={projectBinding}
          willRefreshAutomatically={willRefreshAutomatically}
        />
      )}

      {selectedTutorial === TutorialModes.GitLabCI && (
        <GitLabCITutorial
          baseUrl={baseUrl}
          component={component}
          currentUser={currentUser}
          mainBranchName={mainBranchName}
          willRefreshAutomatically={willRefreshAutomatically}
        />
      )}

      {selectedTutorial === TutorialModes.AzurePipelines && (
        <AzurePipelinesTutorial
          alm={projectBinding?.alm}
          baseUrl={baseUrl}
          component={component}
          currentUser={currentUser}
          willRefreshAutomatically={willRefreshAutomatically}
        />
      )}
    </>
  );
}
