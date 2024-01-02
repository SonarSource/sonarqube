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
import { getComponentNavigation } from '../../../api/navigation';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import CreateApplicationForm from '../../../app/components/extensions/CreateApplicationForm';
import { Button } from '../../../components/controls/buttons';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import { translate } from '../../../helpers/l10n';
import { getComponentAdminUrl, getComponentOverviewUrl } from '../../../helpers/urls';
import { hasGlobalPermission } from '../../../helpers/users';
import { AppState } from '../../../types/appstate';
import { ComponentQualifier } from '../../../types/component';
import { Permissions } from '../../../types/permissions';
import { LoggedInUser } from '../../../types/users';

export interface ApplicationCreationProps {
  appState: AppState;
  className?: string;
  currentUser: LoggedInUser;
  router: Router;
}

export function ApplicationCreation(props: ApplicationCreationProps) {
  const { appState, className, currentUser, router } = props;

  const [showForm, setShowForm] = React.useState(false);

  const canCreateApplication =
    appState.qualifiers.includes(ComponentQualifier.Application) &&
    hasGlobalPermission(currentUser, Permissions.ApplicationCreation);

  if (!canCreateApplication) {
    return null;
  }

  const handleComponentCreate = ({
    key,
    qualifier,
  }: {
    key: string;
    qualifier: ComponentQualifier;
  }) => {
    return getComponentNavigation({ component: key }).then(({ configuration }) => {
      if (configuration && configuration.showSettings) {
        router.push(getComponentAdminUrl(key, qualifier));
      } else {
        router.push(getComponentOverviewUrl(key, qualifier));
      }
      setShowForm(false);
    });
  };

  return (
    <div className={className}>
      <Button className="button-primary" onClick={() => setShowForm(true)}>
        {translate('projects.create_application')}
      </Button>

      {showForm && (
        <CreateApplicationForm
          onClose={() => setShowForm(false)}
          onCreate={handleComponentCreate}
        />
      )}
    </div>
  );
}

export default withCurrentUserContext(withRouter(withAppStateContext(ApplicationCreation)));
