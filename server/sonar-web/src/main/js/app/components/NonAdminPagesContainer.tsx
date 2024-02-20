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
import { CenteredLayout, FlagMessage } from 'design-system';
import * as React from 'react';
import { Outlet } from 'react-router-dom';
import { translate } from '../../helpers/l10n';
import { isApplication } from '../../types/component';
import { ComponentContext } from './componentContext/ComponentContext';

export default function NonAdminPagesContainer() {
  const { component } = React.useContext(ComponentContext);

  /*
   * Catch Applications for which the user does not have access to all child projects
   * and prevent displaying whatever page was requested.
   * This doesn't apply to admin pages (those are not within this container)
   */
  if (component && isApplication(component.qualifier) && !component.canBrowseAllChildProjects) {
    return (
      <CenteredLayout
        className="sw-py-8 sw-body-md sw-flex sw-flex-col sw-items-center"
        id="code-page"
      >
        <FlagMessage className="it__alert-no-access-all-child-project sw-mt-10" variant="error">
          <p>
            {translate('application.cannot_access_all_child_projects1')}
            <br />
            {translate('application.cannot_access_all_child_projects2')}
          </p>
        </FlagMessage>
      </CenteredLayout>
    );
  }

  return <Outlet />;
}
