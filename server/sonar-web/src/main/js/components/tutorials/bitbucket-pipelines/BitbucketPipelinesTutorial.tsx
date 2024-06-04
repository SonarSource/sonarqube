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
import { BasicSeparator, Title, TutorialStep, TutorialStepList } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';
import { Component } from '../../../types/types';
import { LoggedInUser } from '../../../types/users';
import AllSet from '../components/AllSet';
import GithubCFamilyExampleRepositories from '../components/GithubCFamilyExampleRepositories';
import YamlFileStep from '../components/YamlFileStep';
import { TutorialConfig, TutorialModes } from '../types';
import { shouldShowGithubCFamilyExampleRepositories } from '../utils';
import AnalysisCommand from './AnalysisCommand';
import RepositoryVariables from './RepositoryVariables';

export enum Steps {
  REPOSITORY_VARIABLES = 1,
  YAML = 2,
  ALL_SET = 3,
}

export interface BitbucketPipelinesTutorialProps {
  almBinding?: AlmSettingsInstance;
  baseUrl: string;
  component: Component;
  currentUser: LoggedInUser;
  mainBranchName: string;
  willRefreshAutomatically?: boolean;
}

export default function BitbucketPipelinesTutorial(props: BitbucketPipelinesTutorialProps) {
  const { almBinding, baseUrl, currentUser, component, willRefreshAutomatically, mainBranchName } =
    props;

  const [config, setConfig] = React.useState<TutorialConfig>({});
  const [done, setDone] = React.useState(false);

  React.useEffect(() => {
    setDone(Boolean(config.buildTool));
  }, [config.buildTool]);

  return (
    <>
      <Title>{translate('onboarding.tutorial.with.bitbucket_ci.title')}</Title>

      <TutorialStepList className="sw-mb-8">
        <TutorialStep
          title={translate('onboarding.tutorial.with.bitbucket_pipelines.variables.title')}
        >
          <RepositoryVariables
            almBinding={almBinding}
            baseUrl={baseUrl}
            component={component}
            currentUser={currentUser}
          />
        </TutorialStep>
        <TutorialStep title={translate('onboarding.tutorial.with.bitbucket_pipelines.yaml.title')}>
          <YamlFileStep config={config} setConfig={setConfig} ci={TutorialModes.BitbucketPipelines}>
            {(config) => (
              <>
                {shouldShowGithubCFamilyExampleRepositories(config) && (
                  <GithubCFamilyExampleRepositories
                    className="sw-my-4 sw-w-abs-600"
                    ci={TutorialModes.BitbucketPipelines}
                  />
                )}
                <AnalysisCommand
                  config={config}
                  component={component}
                  mainBranchName={mainBranchName}
                />
              </>
            )}
          </YamlFileStep>
        </TutorialStep>

        {done && (
          <>
            <BasicSeparator className="sw-my-10" />
            <AllSet
              alm={AlmKeys.BitbucketCloud}
              willRefreshAutomatically={willRefreshAutomatically}
            />
          </>
        )}
      </TutorialStepList>
    </>
  );
}
