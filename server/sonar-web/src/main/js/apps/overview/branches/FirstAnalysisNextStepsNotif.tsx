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
import { Link } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { queryToSearchString } from '~sonar-aligned/helpers/urls';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import DismissableAlert from '../../../components/ui/DismissableAlert';
import { translate } from '../../../helpers/l10n';
import { useProjectBindingQuery } from '../../../queries/devops-integration';
import { Component } from '../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../types/users';
import { PULL_REQUEST_DECORATION_BINDING_CATEGORY } from '../../settings/constants';

export interface FirstAnalysisNextStepsNotifProps {
  branchesEnabled?: boolean;
  component: Component;
  currentUser: CurrentUser;
  detectedCIOnLastAnalysis?: boolean;
}

export function FirstAnalysisNextStepsNotif(props: FirstAnalysisNextStepsNotifProps) {
  const { component, currentUser, branchesEnabled, detectedCIOnLastAnalysis } = props;

  const { data: projectBinding, isLoading } = useProjectBindingQuery(component.key);

  if (!isLoggedIn(currentUser) || component.qualifier !== ComponentQualifier.Project || isLoading) {
    return null;
  }

  const showConfigurePullRequestDecoNotif = branchesEnabled && projectBinding == null;
  const showConfigureCINotif =
    detectedCIOnLastAnalysis !== undefined ? !detectedCIOnLastAnalysis : false;

  if (!showConfigureCINotif && !showConfigurePullRequestDecoNotif) {
    return null;
  }

  const showOnlyConfigureCI = showConfigureCINotif && !showConfigurePullRequestDecoNotif;
  const showOnlyConfigurePR = showConfigurePullRequestDecoNotif && !showConfigureCINotif;
  const showBoth = showConfigureCINotif && showConfigurePullRequestDecoNotif;
  const isProjectAdmin = component.configuration?.showSettings;
  const tutorialsLink = (
    <Link
      to={{
        pathname: '/tutorials',
        search: queryToSearchString({ id: component.key }),
      }}
    >
      {translate('overview.project.next_steps.links.set_up_ci')}
    </Link>
  );
  const projectSettingsLink = (
    <Link
      to={{
        pathname: '/project/settings',
        search: queryToSearchString({
          id: component.key,
          category: PULL_REQUEST_DECORATION_BINDING_CATEGORY,
        }),
      }}
    >
      {translate('overview.project.next_steps.links.project_settings')}
    </Link>
  );

  return (
    <DismissableAlert alertKey={`config_ci_pr_deco.${component.key}`} variant="info">
      <div>
        {showOnlyConfigureCI && (
          <FormattedMessage
            defaultMessage={translate('overview.project.next_steps.set_up_ci')}
            id="overview.project.next_steps.set_up_ci"
            values={{
              link: tutorialsLink,
            }}
          />
        )}

        {showOnlyConfigurePR &&
          (isProjectAdmin ? (
            <FormattedMessage
              defaultMessage={translate('overview.project.next_steps.set_up_pr_deco.admin')}
              id="overview.project.next_steps.set_up_pr_deco.admin"
              values={{
                link_project_settings: projectSettingsLink,
              }}
            />
          ) : (
            translate('overview.project.next_steps.set_up_pr_deco')
          ))}

        {showBoth &&
          (isProjectAdmin ? (
            <FormattedMessage
              defaultMessage={translate('overview.project.next_steps.set_up_pr_deco_and_ci.admin')}
              id="overview.project.next_steps.set_up_pr_deco_and_ci.admin"
              values={{
                link_ci: tutorialsLink,
                link_project_settings: projectSettingsLink,
              }}
            />
          ) : (
            <FormattedMessage
              defaultMessage={translate('overview.project.next_steps.set_up_pr_deco_and_ci')}
              id="overview.project.next_steps.set_up_pr_deco_and_ci"
              values={{ link_ci: tutorialsLink }}
            />
          ))}
      </div>
    </DismissableAlert>
  );
}

export default withCurrentUserContext(FirstAnalysisNextStepsNotif);
