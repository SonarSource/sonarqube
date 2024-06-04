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
import { BasicSeparator, Title, TutorialStepList } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { AlmKeys } from '../../../types/alm-settings';
import { Component } from '../../../types/types';
import { LoggedInUser } from '../../../types/users';
import AllSet from '../components/AllSet';
import EnvironmentVariablesStep from './EnvironmentVariablesStep';
import YmlFileStep from './YmlFileStep';

export enum Steps {
  ENV_VARIABLES,
  YML,
  ALL_SET,
}

export interface GitLabCITutorialProps {
  baseUrl: string;
  component: Component;
  currentUser: LoggedInUser;
  willRefreshAutomatically?: boolean;
}

export default function GitLabCITutorial(props: GitLabCITutorialProps) {
  const { baseUrl, component, currentUser, willRefreshAutomatically } = props;

  const [done, setDone] = React.useState<boolean>(false);

  return (
    <>
      <Title>{translate('onboarding.tutorial.with.gitlab_ci.title')}</Title>

      <TutorialStepList className="sw-mb-8">
        <EnvironmentVariablesStep
          baseUrl={baseUrl}
          component={component}
          currentUser={currentUser}
        />

        <YmlFileStep component={component} setDone={setDone} />
      </TutorialStepList>

      {done && (
        <>
          <BasicSeparator className="sw-my-10" />
          <AllSet alm={AlmKeys.GitLab} willRefreshAutomatically={willRefreshAutomatically} />
        </>
      )}
    </>
  );
}
