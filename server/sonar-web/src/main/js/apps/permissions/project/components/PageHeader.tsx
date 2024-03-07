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
import { ButtonPrimary, FlagMessage, Title } from 'design-system';
import * as React from 'react';
import GitHubSynchronisationWarning from '../../../../app/components/GitHubSynchronisationWarning';
import { translate } from '../../../../helpers/l10n';
import { getBaseUrl } from '../../../../helpers/system';
import { useGithubProvisioningEnabledQuery } from '../../../../queries/identity-provider/github';
import { isApplication, isPortfolioLike, isProject } from '../../../../types/component';
import { Component } from '../../../../types/types';
import ApplyTemplate from './ApplyTemplate';

interface Props {
  component: Component;
  isGitHubProject?: boolean;
  loadHolders: () => void;
}

export default function PageHeader(props: Props) {
  const [applyTemplateModal, setApplyTemplateModal] = React.useState(false);
  const { data: githubProvisioningStatus } = useGithubProvisioningEnabledQuery();

  const { component, isGitHubProject } = props;
  const { configuration } = component;
  const provisionedByGitHub = isGitHubProject && !!githubProvisioningStatus;
  const canApplyPermissionTemplate =
    configuration?.canApplyPermissionTemplate && !provisionedByGitHub;

  const handleApplyTemplate = () => {
    setApplyTemplateModal(true);
  };

  const handleApplyTemplateClose = () => {
    setApplyTemplateModal(false);
  };

  let description = translate('roles.page.description2');
  if (isPortfolioLike(component.qualifier)) {
    description = translate('roles.page.description_portfolio');
  } else if (isApplication(component.qualifier)) {
    description = translate('roles.page.description_application');
  }

  const visibilityDescription =
    isProject(component.qualifier) && component.visibility
      ? translate('visibility', component.visibility, 'description', component.qualifier)
      : undefined;

  return (
    <header className="sw-mb-2 sw-flex sw-items-center sw-justify-between">
      <div>
        <Title>
          {translate('permissions.page')}
          {provisionedByGitHub && (
            <img
              alt="github"
              className="sw-mx-2 sw-align-baseline"
              aria-label={translate('project_permission.github_managed')}
              height={16}
              src={`${getBaseUrl()}/images/alm/github.svg`}
            />
          )}
        </Title>

        <div>
          <p>{description}</p>
          {visibilityDescription && <p>{visibilityDescription}</p>}
          {provisionedByGitHub && (
            <>
              <p>{translate('roles.page.description.github')}</p>
              <div className="sw-mt-2">
                <GitHubSynchronisationWarning short />
              </div>
            </>
          )}
          {githubProvisioningStatus && !isGitHubProject && (
            <FlagMessage variant="warning" className="sw-mt-2">
              {translate('project_permission.local_project_with_github_provisioning')}
            </FlagMessage>
          )}
        </div>
      </div>
      {canApplyPermissionTemplate && (
        <div>
          <ButtonPrimary className="js-apply-template" onClick={handleApplyTemplate}>
            {translate('projects_role.apply_template')}
          </ButtonPrimary>

          {applyTemplateModal && (
            <ApplyTemplate
              onApply={props.loadHolders}
              onClose={handleApplyTemplateClose}
              project={component}
            />
          )}
        </div>
      )}
    </header>
  );
}
