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

import { LinkStandalone } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { Card, LightLabel } from '~design-system';
import { Image } from '~sonar-aligned/components/common/Image';
import { translate } from '../../../helpers/l10n';
import { OSs, TutorialModes } from '../types';

export interface GithubCFamilyExampleRepositoriesProps {
  ci?: TutorialModes;
  className?: string;
  os?: OSs;
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
  props: Readonly<GithubCFamilyExampleRepositoriesProps>,
) {
  const { className, os, ci } = props;
  const queryParams = ['sq', os ? OS_SEARCH_MAP[os] : undefined, ci ? CI_SEARCH_MAP[ci] : undefined]
    .filter((s) => !!s)
    .join('+');
  const link = `https://github.com/orgs/sonarsource-cfamily-examples/repositories?q=${queryParams}`;

  return (
    <Card className={classNames('sw-p-4 sw-bg-inherit', className)}>
      <div>
        <Image
          alt="" // Should be ignored by screen readers
          className="sw-mr-2"
          height={20}
          src="/images/alm/github.svg"
        />
        <LinkStandalone shouldOpenInNewTab to={link}>
          sonarsource-cfamily-examples
        </LinkStandalone>
      </div>
      <LightLabel as="p" className="sw-mt-4">
        {translate('onboarding.tutorial.cfamily.examples_repositories_description')}
      </LightLabel>
    </Card>
  );
}
