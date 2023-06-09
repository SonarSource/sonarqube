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
import { Helmet } from 'react-helmet-async';
import OrganizationsList from './OrganizationsList';
import { Organization } from "../../../types/types";
import { translate } from "../../../helpers/l10n";
import Link from "../../../components/common/Link";
import { AppState } from "../../../types/appstate";
import withAppStateContext from "../../../app/components/app-state/withAppStateContext";
import withCurrentUserContext from "../../../app/components/current-user/withCurrentUserContext";
import { GlobalSettingKeys } from "../../../types/settings";

interface Props {
  appState: AppState;
  userOrganizations: Organization[];
}

function UserOrganizations(props: Props) {

  const { appState: { settings, canAdmin, canCustomerAdmin }, userOrganizations } = props;
  const anyoneCanCreate = settings[GlobalSettingKeys.OrganizationsAnyoneCanCreate] === 'true';
  const canCreateOrganizations = (anyoneCanCreate || canAdmin || canCustomerAdmin);

  return (
      <div className="account-body account-container">
        <Helmet title={translate('my_account.organizations')}/>

        <div className="boxed-group">
          {canCreateOrganizations && (
              <div className="clearfix">
                <div className="boxed-group-actions">
                  <Link className="button" to="/create-organization">
                    {translate('create')}
                  </Link>
                </div>
              </div>
          )}
          <div className="boxed-group-inner">
            <OrganizationsList organizations={userOrganizations}/>
          </div>
        </div>
      </div>
  );
}

export default withAppStateContext(withCurrentUserContext(UserOrganizations));
