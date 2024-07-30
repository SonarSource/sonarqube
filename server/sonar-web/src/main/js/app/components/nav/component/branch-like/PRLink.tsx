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
import React from 'react';
import { Image } from '~sonar-aligned/components/common/Image';
import { isPullRequest } from '~sonar-aligned/helpers/branch-like';
import { translate, translateWithParameters } from '../../../../../helpers/l10n';
import { isDefined } from '../../../../../helpers/types';
import { AlmKeys } from '../../../../../types/alm-settings';
import { BranchLike } from '../../../../../types/branch-like';
import { Component } from '../../../../../types/types';

function getPRUrlAlmKey(url = '') {
  const lowerCaseUrl = url.toLowerCase();

  if (lowerCaseUrl.includes(AlmKeys.GitHub)) {
    return AlmKeys.GitHub;
  } else if (lowerCaseUrl.includes(AlmKeys.GitLab)) {
    return AlmKeys.GitLab;
  } else if (lowerCaseUrl.includes(AlmKeys.BitbucketServer)) {
    return AlmKeys.BitbucketServer;
  } else if (
    lowerCaseUrl.includes(AlmKeys.Azure) ||
    lowerCaseUrl.includes('microsoft') ||
    lowerCaseUrl.includes('visualstudio')
  ) {
    return AlmKeys.Azure;
  }

  return undefined;
}

export default function PRLink({
  currentBranchLike,
  component,
}: Readonly<{
  component: Component;
  currentBranchLike: BranchLike;
}>) {
  if (!isPullRequest(currentBranchLike)) {
    return null;
  }

  const almKey =
    component.alm?.key ||
    (isPullRequest(currentBranchLike) && getPRUrlAlmKey(currentBranchLike.url)) ||
    '';

  return (
    <>
      {isDefined(currentBranchLike.url) && (
        <LinkStandalone
          iconLeft={
            almKey !== '' && (
              <Image
                alt={almKey}
                height={16}
                src={`/images/alm/${almKey}.svg`}
                title={translateWithParameters('branches.see_the_pr_on_x', translate(almKey))}
              />
            )
          }
          key={currentBranchLike.key}
          to={currentBranchLike.url}
        >
          {almKey === '' && translate('branches.see_the_pr')}
        </LinkStandalone>
      )}
    </>
  );
}
