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
import { Select } from '@sonarsource/echoes-react';
import { find, sortBy } from 'lodash';
import * as React from 'react';
import '../../../../js/app/styles/pages/CreateProject.css';
import { translate } from '../../../helpers/l10n';
import { Organization } from '../../../types/types';

interface Props {
  onChange: (organization: Organization) => void;
  organization?: Organization;
  organizations: Organization[];
}

export default function OrganizationSelect({ onChange, organization, organizations }: Props) {
  const options = React.useMemo(
    () =>
      sortBy(organizations, (org) => org.name.toLowerCase()).map((org) => ({
        label: org.name,
        value: org.kee,
      })),
    [organizations],
  );

  return (
    <Select
      autoFocus={!organization}
      onChange={(key) => {
        const org = find(organizations, (org) => org.kee === key);
        if (org) {
          onChange(org);
        }
      }}
      data={options}
      isSearchable
      isNotClearable
      placeholder={translate('onboarding.import_organization.choose_organization')}
      value={organization?.kee}
      className="organization-selectbox"
    />
  );
}
