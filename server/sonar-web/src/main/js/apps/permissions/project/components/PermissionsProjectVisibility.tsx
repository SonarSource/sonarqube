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
import VisibilitySelector from '../../../../components/common/VisibilitySelector';
import {
  useIsGitHubProjectQuery,
  useIsGitLabProjectQuery,
} from '../../../../queries/devops-integration';
import { useGithubProvisioningEnabledQuery } from '../../../../queries/identity-provider/github';
import { useGilabProvisioningEnabledQuery } from '../../../../queries/identity-provider/gitlab';
import { Component } from '../../../../types/types';

interface Props {
  component: Component;
  handleVisibilityChange: (visibility: string) => void;
  isLoading: boolean;
}

export default function PermissionsProjectVisibility(props: Readonly<Props>) {
  const { component, handleVisibilityChange, isLoading } = props;
  const canTurnToPrivate = component.configuration?.canUpdateProjectVisibilityToPrivate;

  const { data: isGitHubProject } = useIsGitHubProjectQuery(component.key);
  const { data: isGitLabProject } = useIsGitLabProjectQuery(component.key);
  const { data: gitHubProvisioningStatus, isFetching: isFetchingGitHubProvisioningStatus } =
    useGithubProvisioningEnabledQuery();
  const { data: gitLabProvisioningStatus, isFetching: isFetchingGitLabProvisioningStatus } =
    useGilabProvisioningEnabledQuery();
  const isFetching = isFetchingGitHubProvisioningStatus || isFetchingGitLabProvisioningStatus;
  const isDisabled =
    (isGitHubProject && !!gitHubProvisioningStatus) ||
    (isGitLabProject && !!gitLabProvisioningStatus);

  return (
    <VisibilitySelector
      canTurnToPrivate={canTurnToPrivate}
      className="sw-flex sw-my-4"
      onChange={handleVisibilityChange}
      loading={isLoading || isFetching}
      disabled={isDisabled}
      visibility={component.visibility}
    />
  );
}
