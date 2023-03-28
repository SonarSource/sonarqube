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
import { useEffect, useState } from 'react';
import { Outlet, useParams } from 'react-router-dom';
import { OrganizationContextProps } from "../OrganizationContext";
import { Organization } from "../../../types/types";
import { getOrganization, getOrganizationNavigation } from "../../../api/organizations";
import { throwGlobalError } from "../../../helpers/error";
import { Helmet } from "react-helmet-async";
import Suggestions from "../../../components/embed-docs-modal/Suggestions";
import OrganizationNavigation from "../navigation/OrganizationNavigation";
import { Alert } from "../../../components/ui/Alert";
import withCurrentUserContext from "../../../app/components/current-user/withCurrentUserContext";
import { Location, withRouter } from "../../../components/hoc/withRouter";
import './OrganizationApp.css';

interface OrganizationAppProps {
  userOrganizations: Organization[];
  location: Location;
}

const OrganizationApp: React.FC<OrganizationAppProps> = ({ userOrganizations, location }) => {

  const { organizationKey } = useParams();
  const [organization, setOrganization] = useState<Organization>();

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

  return (
      <div>
        <Helmet defaultTitle={organization.name} titleTemplate={'%s - ' + organization.name}/>
        <Suggestions suggestions="organization_space"/>
        <OrganizationNavigation
            location={location}
            organization={organization}
            userOrganizations={userOrganizations}
        />
        {
          organization.notifications ? organization.notifications.map((notification, index) => (
                  <Alert key={index} variant={notification.type} display="banner"
                         className={'top-fixed alert-' + notification.type}>
                    <div className="alert-inner-content">
                      <span
                          dangerouslySetInnerHTML={{
                            __html: notification.message
                          }}
                      />
                    </div>
                  </Alert>
              )
          ) : null
        }
        {renderChildren(organization)}
      </div>
  );
}

export default withRouter(withCurrentUserContext(OrganizationApp));
