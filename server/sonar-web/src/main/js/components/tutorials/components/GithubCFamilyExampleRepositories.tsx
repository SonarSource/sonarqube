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
import classNames from 'classnames';
import React from 'react';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import Link from '../../common/Link';
import { OSs, TutorialModes } from '../types';
import './GithubCFamilyExampleRepositories.css';

export interface GithubCFamilyExampleRepositoriesProps {
  className?: string;
  os?: OSs;
  ci?: TutorialModes;
}

const OS_SEARCH_MAP = {
  [OSs.Linux]: 'linux',
  [OSs.Windows]: 'windows',
  [OSs.MacOS]: 'macos',
};

const CI_SEARCH_MAP = {
  [TutorialModes.Jenkins]: 'jenkins',
  [TutorialModes.AzurePipelines]: 'azure',
  [TutorialModes.GitHubActions]: 'gh-actions',
  [TutorialModes.GitLabCI]: 'gitlab',
  [TutorialModes.BitbucketPipelines]: 'bitbucket',
  [TutorialModes.Local]: 'otherci',
  [TutorialModes.OtherCI]: 'otherci',
};

export default function GithubCFamilyExampleRepositories(
  props: GithubCFamilyExampleRepositoriesProps
) {
  const { className, os, ci } = props;
  const queryParams = ['sq', os ? OS_SEARCH_MAP[os] : undefined, ci ? CI_SEARCH_MAP[ci] : undefined]
    .filter((s) => !!s)
    .join('+');
  const link = `https://github.com/orgs/sonarsource-cfamily-examples/repositories?q=${queryParams}`;

  return (
    <div
      className={classNames(
        'github-cfamily-example-repositories-box big-padded boxed-group',
        className
      )}
    >
      <div className="display-flex-center">
        <img
          alt="" // Should be ignored by screen readers
          className="spacer-right"
          height={20}
          src={`${getBaseUrl()}/images/alm/github.svg`}
        />
        <Link className="spacer-left big" target="_blank" to={link}>
          sonarsource-cfamily-examples
        </Link>
      </div>
      <p className="spacer-top">
        {translate('onboarding.tutorial.cfamily.examples_repositories_description')}
      </p>
    </div>
  );
}
