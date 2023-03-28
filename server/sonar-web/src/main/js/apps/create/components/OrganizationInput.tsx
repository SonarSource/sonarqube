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
import { translate } from "../../../helpers/l10n";
import OrganizationSelect from './OrganizationSelect';
import { Organization } from "../../../types/types";
import { NavLink } from "react-router-dom";

interface Props {
  onChange: (organization: Organization) => void;
  organization?: Organization;
  organizations: Organization[];
}

export default function OrganizationInput({ organization, organizations, onChange }: Props) {

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
        <NavLink className="big-spacer-left" to="/organizations/create">
          {translate('onboarding.create_project.create_new_org')}
        </NavLink>
      </div>
  );
}
