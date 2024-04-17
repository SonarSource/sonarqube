/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { rawSizes } from '../../../app/theme';
import ContextNavBar from "../../../components/ui/ContextNavBar";
import { Organization, Notification } from "../../../types/types";
import { getRawNotificationsForOrganization } from '../../../../js/api/codescan';
import { sanitizeUserInput } from '../../../helpers/sanitize';

interface Props {
  location: { pathname: string };
  organization: Organization;
  userOrganizations: Organization[];
}

export function OrganizationNavigation({ location, organization, userOrganizations }: Props) {
  const [notifications, setNotifications] = React.useState<Notification[]>([]);
  const { contextNavHeightRaw, contextNavHeightWithError } = rawSizes;
  const [height, setHeight] = React.useState(contextNavHeightRaw);
  const orgAlertHeight = 30; // refer .org-alert-warning height in NavBarTabs

  React.useEffect(() => {
    const fetchNotifications = async () => {
        const notifications = await getRawNotificationsForOrganization(organization.kee);
        if (notifications.length > 0) {
          const errorNotifications = notifications.filter(notification => notification.type == 'ERROR');
          if (errorNotifications.length > 0) {
            setNotifications(errorNotifications);
            setHeight(contextNavHeightWithError + orgAlertHeight * (errorNotifications.length - 1));
          } else {
            setNotifications(notifications);
            setHeight(contextNavHeightWithError + orgAlertHeight * (notifications.length - 1));
          }
        } else {
          setNotifications([]);
          setHeight(contextNavHeightRaw);
        }
    };
    fetchNotifications();
  },[organization, userOrganizations]);

  return (
        <>
        <ContextNavBar height={height} id="context-navigation">
          <div className="navbar-context-justified">
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
          { notifications.map((notification, key) => (
              <div className={"org-alert-" + notification.type.toLowerCase()} key={key}>
                <div className='org-alert-inner'>
                  <div className='icon'>
                    {notification.type === 'ERROR' ? 'x' : '!'}
                  </div>
                  <div className='msg' dangerouslySetInnerHTML={{ __html: sanitizeUserInput(notification.message) }} />
                </div>
              </div>
            ))
          }
        </ContextNavBar>
      </>
    );
}

export default OrganizationNavigation;
