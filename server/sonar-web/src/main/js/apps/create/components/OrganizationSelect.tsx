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
import { sortBy } from 'lodash';
import { translate } from "../../../helpers/l10n";
import Select from "../../../components/controls/Select";
import { Organization } from "../../../types/types";
import { components, OptionProps, SingleValueProps } from "react-select";

interface Props {
  onChange: (organization: Organization) => void;
  organization?: Organization;
  organizations: Organization[];
}

const optionRenderer = (props: OptionProps<Organization, false>) => {
  return <components.Option {...props}>{renderValue(props.data)}</components.Option>;
}

const singleValueRenderer = (props: SingleValueProps<Organization>) => (
    <components.SingleValue {...props}>{renderValue(props.data)}</components.SingleValue>
);

function renderValue(organization: Organization) {
  return (
      <div className="display-flex-space-between">
        <span className="text-ellipsis flex-1">
          {organization.name}
          <span className="note sw-ml-2">{organization.kee}</span>
        </span>
      </div>
  );
}

export default function OrganizationSelect({ onChange, organization, organizations }: Props) {
  return (
      <Select
          autoFocus={!organization}
          className="input-super-large"
          isClearable={false}
          id="select-organization"
          onChange={onChange}
          options={sortBy(organizations, o => o.name.toLowerCase())}
          getOptionLabel={(org) => org.name}
          getOptionValue={(org) => org.kee}
          isSearchable={true}
          components={{
            Option: optionRenderer,
            SingleValue: singleValueRenderer,
          }}
          placeholder={translate('onboarding.import_organization.choose_organization')}
          value={organization}
      />
  );
}
