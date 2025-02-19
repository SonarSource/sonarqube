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
import { Organization, Notification } from "../../../types/types";
import { getRawNotificationsForOrganization } from '../../../api/codescan';
import { Banner, SafeHTMLInjection, TopBar } from "~design-system";
import './OrganizationNavigation.css';

interface Props {
  location: { pathname: string };
  organization: Organization;
  userOrganizations: Organization[];
}

export function OrganizationNavigation({ location, organization, userOrganizations }: Props) {
  const [notifications, setNotifications] = React.useState<Notification[]>([]);

  React.useEffect(() => {
    const fetchNotifications = async () => {
      const notifications = await getRawNotificationsForOrganization(organization.kee);
      if (notifications.length >= 0) {
        setNotifications(notifications);
      }
    };
    fetchNotifications();
  }, [organization, userOrganizations]);

  function getNotificationVariant(
    notification: Notification,
  ): 'error' | 'warning' | 'success' | 'info' {
    const notificationType = notification.type.toLowerCase();

    if (
      notificationType === 'error' ||
      notificationType === 'warning' ||
      notificationType === 'success' ||
      notificationType === 'info'
    ) {
      return notificationType as 'error' | 'warning' | 'success' | 'info';
    }

    throw new Error(`Invalid notification type: ${notification.type}`);
  }

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
      {notifications.map((notification, key) => {
        return (
          <div key={key} className="org-nav-banner-element">
            <Banner
              className="org-nav-banner"
              key={key}
              variant={getNotificationVariant(notification)}
            >
              <SafeHTMLInjection htmlAsString={notification.message} />
            </Banner>
          </div>
        );
      })}
    </TopBar>
  );
}

export default OrganizationNavigation;
