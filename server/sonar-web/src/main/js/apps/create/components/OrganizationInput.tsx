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
import { translate } from "../../../helpers/l10n";
import OrganizationSelect from './OrganizationSelect';
import { Organization } from "../../../types/types";
import { NavLink } from "react-router-dom";
import { AppState } from '../../../../js/types/appstate';
import withAppStateContext from '../../../../js/app/components/app-state/withAppStateContext';
import { GlobalSettingKeys } from '../../../../js/types/settings';


interface Props {
  onChange: (organization: Organization) => void;
  appState: AppState;
  organization?: Organization;
  organizations: Organization[];
}
function OrganizationInput(props: Props) {

  const { appState: { settings, canAdmin, canCustomerAdmin }, onChange, organization, organizations } = props;
  const anyoneCanCreate = settings[GlobalSettingKeys.OrganizationsAnyoneCanCreate] === 'true';
  const canCreateOrganizations = (anyoneCanCreate || canAdmin || canCustomerAdmin);

  return (
      <div className="form-field spacer-bottom">
        <label htmlFor="select-organization">
          <span className="text-middle">
            <strong>{translate('onboarding.create_project.organization')}</strong>
            <em className="mandatory">*</em>
          </span>
        </label>
        <OrganizationSelect
            onChange={onChange}
            organization={organization}
            organizations={organizations}
        />
        {canCreateOrganizations && (
          <NavLink className="big-spacer-left" to="/organizations/create">
            {translate('onboarding.create_project.create_new_org')}
          </NavLink>
         )}
      </div>
  );
}

export default withAppStateContext(OrganizationInput);
