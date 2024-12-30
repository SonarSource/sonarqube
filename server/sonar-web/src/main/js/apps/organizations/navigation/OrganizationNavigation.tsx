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
import OrganizationNavigationHeader from './OrganizationNavigationHeader';
import OrganizationNavigationMenu from './OrganizationNavigationMenu';
import OrganizationNavigationMeta from './OrganizationNavigationMeta';
import { Organization } from "../../../types/types";
import { getRawNotificationsForOrganization } from '../../../api/codescan';
import { addGlobalErrorMessage, addGlobalWarningMessage, TopBar } from "~design-system";

interface Props {
  location: { pathname: string };
  organization: Organization;
  userOrganizations: Organization[];
}

export function OrganizationNavigation({ location, organization, userOrganizations }: Props) {
  React.useEffect(() => {
    const fetchNotifications = async () => {
      const notifications = await getRawNotificationsForOrganization(organization.kee);
      if (notifications.length) {
        notifications.forEach((notification) => {
          if (notification.type == 'ERROR') {
            addGlobalErrorMessage(notification.message);
          } else {
            addGlobalWarningMessage(notification.message);
          }
        });
      }
    };
    fetchNotifications();
  }, [organization, userOrganizations]);

  return (
    <TopBar id="context-navigation">
      <div className="sw-flex sw-justify-between">
        <OrganizationNavigationHeader
          organization={organization}
          organizations={userOrganizations}
        />
        <OrganizationNavigationMeta
          organization={organization}
        />
      </div>
      <OrganizationNavigationMenu
        location={location}
        organization={organization}
      />
    </TopBar>
  );
}

export default OrganizationNavigation;
