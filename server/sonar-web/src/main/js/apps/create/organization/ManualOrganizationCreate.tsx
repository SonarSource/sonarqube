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
import OrganizationDetailsForm from './OrganizationDetailsForm';
import { translate } from "../../../helpers/l10n";
import { Organization } from "../../../types/types";
import { createOrganization } from "../../../api/organizations";

interface Props {
  onOrganizationCreate: (organization: Organization) => void;
  className?: string;
}

export default class ManualOrganizationCreate extends React.PureComponent<Props> {

  handleCreateOrganization = (organization: Organization) => {
    return createOrganization({ ...organization, name: organization.name || organization.kee })
        .then((newOrganization) => {
          this.props.onOrganizationCreate(newOrganization);
        });
  };

  render() {
    const { className } = this.props;
    return (
        <div className={className}>
          <OrganizationDetailsForm
              createOrganization={this.handleCreateOrganization}
              submitText={translate('my_account.create_organization')}/>
        </div>
    );
  }
}
