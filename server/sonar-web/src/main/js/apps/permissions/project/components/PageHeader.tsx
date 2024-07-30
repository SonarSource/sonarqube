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

import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { FlagMessage, Title } from 'design-system';
import * as React from 'react';
import { Image } from '~sonar-aligned/components/common/Image';
import { isPortfolioLike } from '~sonar-aligned/helpers/component';
import GitHubSynchronisationWarning from '../../../../app/components/GitHubSynchronisationWarning';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { isDefined } from '../../../../helpers/types';
import {
  useIsGitHubProjectQuery,
  useIsGitLabProjectQuery,
} from '../../../../queries/devops-integration';
import { useGithubProvisioningEnabledQuery } from '../../../../queries/identity-provider/github';
import { useGilabProvisioningEnabledQuery } from '../../../../queries/identity-provider/gitlab';
import { isApplication, isProject } from '../../../../types/component';
import { Component } from '../../../../types/types';
import ApplyTemplate from './ApplyTemplate';

interface Props {
  component: Component;
  loadHolders: () => void;
}

export default function PageHeader(props: Readonly<Props>) {
  const { component, loadHolders } = props;
  const { configuration, key, qualifier, visibility } = component;
  const [applyTemplateModal, setApplyTemplateModal] = React.useState(false);
  const { data: isGitHubProject } = useIsGitHubProjectQuery(key);
  const { data: isGitLabProject } = useIsGitLabProjectQuery(key);
  const { data: githubProvisioningStatus } = useGithubProvisioningEnabledQuery();
  const { data: gitlabProvisioningStatus } = useGilabProvisioningEnabledQuery();

  const provisionedByGitHub = isGitHubProject && !!githubProvisioningStatus;
  const provisionedByGitLab = isGitLabProject && !!gitlabProvisioningStatus;
  const provisioned = provisionedByGitHub || provisionedByGitLab;
  const canApplyPermissionTemplate = configuration?.canApplyPermissionTemplate && !provisioned;

  const handleApplyTemplate = () => {
    setApplyTemplateModal(true);
  };

  const handleApplyTemplateClose = () => {
    setApplyTemplateModal(false);
  };

  let description = translate('roles.page.description2');
  if (isPortfolioLike(qualifier)) {
    description = translate('roles.page.description_portfolio');
  } else if (isApplication(qualifier)) {
    description = translate('roles.page.description_application');
  }

  const visibilityDescription =
    isProject(qualifier) && visibility
      ? translate('visibility', visibility, 'description', qualifier)
      : undefined;

  return (
    <header className="sw-mb-2 sw-flex sw-items-center sw-justify-between">
      <div>
        <Title>
          {translate('permissions.page')}
          {provisioned && (
            <Image
              alt={provisionedByGitHub ? 'github' : 'gitlab'}
              className="sw-mx-2 sw-align-baseline"
              aria-label={translateWithParameters(
                'project_permission.managed',
                provisionedByGitHub ? translate('alm.github') : translate('alm.gitlab'),
              )}
              height={16}
              src={`/images/alm/${provisionedByGitHub ? 'github' : 'gitlab'}.svg`}
            />
          )}
        </Title>

        <div>
          <p>{description}</p>
          {isDefined(visibilityDescription) && <p>{visibilityDescription}</p>}
          {provisioned && (
            <>
              <p>
                {provisionedByGitHub
                  ? translate('roles.page.description.github')
                  : translate('roles.page.description.gitlab')}
              </p>
              <div className="sw-mt-2">
                {provisionedByGitHub && <GitHubSynchronisationWarning short />}
              </div>
            </>
          )}
          {githubProvisioningStatus && !isGitHubProject && (
            <FlagMessage variant="warning" className="sw-mt-2">
              {translate('project_permission.local_project_with_github_provisioning')}
            </FlagMessage>
          )}
          {gitlabProvisioningStatus && !isGitLabProject && (
            <FlagMessage variant="warning" className="sw-mt-2">
              {translate('project_permission.local_project_with_gitlab_provisioning')}
            </FlagMessage>
          )}
        </div>
      </div>
      {canApplyPermissionTemplate && (
        <div>
          <Button
            className="js-apply-template"
            onClick={handleApplyTemplate}
            variety={ButtonVariety.Primary}
          >
            {translate('projects_role.apply_template')}
          </Button>

          {applyTemplateModal && (
            <ApplyTemplate
              onApply={loadHolders}
              onClose={handleApplyTemplateClose}
              project={component}
            />
          )}
        </div>
      )}
    </header>
  );
}
