/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { Link } from 'react-router';
import { sortBy } from 'lodash';
import Select from '../../../components/controls/Select';
import { Organization } from '../../../app/types';
import { translate } from '../../../helpers/l10n';
import { sanitizeAlmId } from '../../../helpers/almIntegrations';
import { getBaseUrl } from '../../../helpers/urls';

interface Props {
  autoImport?: boolean;
  onChange: (organization: Organization) => void;
  organization: string;
  organizations: Organization[];
}

export default function OrganizationSelect({
  autoImport,
  onChange,
  organization,
  organizations
}: Props) {
  return (
    <div className="form-field spacer-bottom">
      <label htmlFor="select-organization">
        {translate('onboarding.create_project.organization')}
        <em className="mandatory">*</em>
      </label>
      <Select
        autoFocus={true}
        className="input-super-large"
        clearable={false}
        id="select-organization"
        labelKey="name"
        onChange={onChange}
        optionRenderer={optionRenderer}
        options={sortBy(organizations, o => o.name.toLowerCase())}
        required={true}
        value={organization}
        valueKey="key"
        valueRenderer={optionRenderer}
      />
      <Link className="big-spacer-left js-new-org" to="/create-organization">
        {autoImport
          ? translate('onboarding.create_project.import_new_org')
          : translate('onboarding.create_project.create_new_org')}
      </Link>
    </div>
  );
}

export function optionRenderer(organization: Organization) {
  return (
    <span>
      {organization.alm && (
        <img
          alt={organization.alm.key}
          className="spacer-right"
          height={14}
          src={`${getBaseUrl()}/images/sonarcloud/${sanitizeAlmId(organization.alm.key)}.svg`}
        />
      )}
      {organization.name}
      <span className="note little-spacer-left">{organization.key}</span>
    </span>
  );
}
