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
import { ButtonPrimary, FishVisual, Highlight } from 'design-system';
import * as React from 'react';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import { translate } from '../../../helpers/l10n';
import { hasGlobalPermission } from '../../../helpers/users';
import { Permissions } from '../../../types/permissions';
import { CurrentUser, isLoggedIn } from '../../../types/users';

export interface EmptyInstanceProps {
  currentUser: CurrentUser;
  router: Router;
}

export function EmptyInstance(props: EmptyInstanceProps) {
  const { currentUser, router } = props;
  const showNewProjectButton =
    isLoggedIn(currentUser) && hasGlobalPermission(currentUser, Permissions.ProjectCreation);

  return (
    <div className="sw-text-center sw-py-8">
      <FishVisual />
      <Highlight as="h3" className="sw-body-md-highlight sw-mt-6">
        {showNewProjectButton
          ? translate('projects.no_projects.empty_instance.new_project')
          : translate('projects.no_projects.empty_instance')}
      </Highlight>
      {showNewProjectButton && (
        <div>
          <p className="sw-mt-2 sw-body-sm">
            {translate('projects.no_projects.empty_instance.how_to_add_projects')}
          </p>
          <ButtonPrimary
            className="sw-mt-6"
            onClick={() => {
              router.push('/projects/create');
            }}
          >
            {translate('my_account.create_new.TRK')}
          </ButtonPrimary>
        </div>
      )}
    </div>
  );
}

export default withRouter(EmptyInstance);
