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
import { useEffect, useState } from 'react';
import { Outlet, useParams } from 'react-router-dom';
import { OrganizationContextProps } from "../OrganizationContext";
import { Organization } from "../../../types/types";
import { getOrganization, getOrganizationNavigation } from "../../../api/organizations";
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { Helmet } from "react-helmet-async";
import Suggestions from "../../../components/embed-docs-modal/Suggestions";
import OrganizationNavigation from "../navigation/OrganizationNavigation";
import withCurrentUserContext from "../../../app/components/current-user/withCurrentUserContext";
import { Location } from '~sonar-aligned/types/router';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import './OrganizationApp.css';
import { hasGlobalPermission } from "../../../helpers/users";
import { Permissions } from "../../../types/permissions";
import { CurrentUser } from "../../../types/users";
import NotFound from "../../../app/components/NotFound";
import { FlagMessage } from "~design-system";
import { createPortal } from "react-dom";

interface OrganizationAppProps {
  currentUser: CurrentUser;
  userOrganizations: Organization[];
  location: Location;
}

const OrganizationApp: React.FC<OrganizationAppProps> = ({ currentUser, userOrganizations, location }) => {

  const { organizationKey } = useParams();
  const portalAnchor = React.useRef<Element | null>(null);
  const [organization, setOrganization] = useState<Organization>();

  // Set portal anchor on mount
  useEffect(() => {
    portalAnchor.current = document.querySelector('#component-nav-portal');
  }, []);

  useEffect(() => {
    if (organizationKey != null) {
      Promise.all([getOrganization(organizationKey), getOrganizationNavigation(organizationKey)]).then(
        ([organization, navigation]) => {
          if (organization) {
            const organizationWithPermissions = { ...organization, ...navigation };
            setOrganization(organizationWithPermissions);
          }
        }
      ).catch(throwGlobalError);
    }
  }, [organizationKey]);

  const renderChildren = (organization: Organization) => {
    const context: OrganizationContextProps = {
      organization,
    };

    return <Outlet context={context}/>;
  }

  if (!organization) {
    return null;
  }

  const isMember = userOrganizations.find(o => o.kee == organization.kee);
  const isSysAdmin = hasGlobalPermission(currentUser, Permissions.Admin);
  if (!isMember && !isSysAdmin) {
    return <NotFound withContainer={false}/>;
  }

  return (
    <div>
      <Helmet defaultTitle={organization.name} titleTemplate={'%s - ' + organization.name}/>
      <Suggestions suggestions="organization_space"/>

      {organization && portalAnchor.current &&
        /* Use a portal to fix positioning until we can fully review the layout */
        createPortal(
          <OrganizationNavigation
            location={location}
            organization={organization}
            userOrganizations={userOrganizations}
          />,
          portalAnchor.current,
        )
      }

      {
        organization.notifications ? organization.notifications.map((notification, index) => (
            <FlagMessage
              variant={notification.type}
              display="banner"
              className={'top-fixed alert-' + notification.type}
            >
              <div className="alert-inner-content">
                  <span
                    dangerouslySetInnerHTML={{
                      __html: notification.message
                    }}
                  />
              </div>
            </FlagMessage>
          )
        ) : null
      }
      <main className="sw-my-6">
        {renderChildren(organization)}
      </main>
    </div>
  );
}

export default withRouter(withCurrentUserContext(OrganizationApp));
