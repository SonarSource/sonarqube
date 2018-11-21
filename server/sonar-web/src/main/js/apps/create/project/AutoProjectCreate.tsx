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
import RemoteRepositories from './RemoteRepositories';
import OrganizationInput from './OrganizationInput';
import IdentityProviderLink from '../../../components/ui/IdentityProviderLink';
import { AlmApplication, Organization } from '../../../app/types';
import {
  ORGANIZATION_IMPORT_BINDING_IN_PROGRESS_TIMESTAMP,
  ORGANIZATION_IMPORT_REDIRECT_TO_PROJECT_TIMESTAMP
} from '../organization/utils';
import { translate } from '../../../helpers/l10n';
import { save } from '../../../helpers/storage';

interface Props {
  almApplication: AlmApplication;
  boundOrganizations: Organization[];
  onProjectCreate: (projectKeys: string[], organization: string) => void;
  organization?: string;
}

interface State {
  selectedOrganization: string;
}

export default class AutoProjectCreate extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { selectedOrganization: this.getInitialSelectedOrganization(props) };
  }

  getInitialSelectedOrganization(props: Props) {
    if (props.organization) {
      return props.organization;
    } else if (props.boundOrganizations.length === 1) {
      return props.boundOrganizations[0].key;
    } else {
      return '';
    }
  }

  handleInstallAppClick = () => {
    save(ORGANIZATION_IMPORT_BINDING_IN_PROGRESS_TIMESTAMP, Date.now().toString(10));
    save(ORGANIZATION_IMPORT_REDIRECT_TO_PROJECT_TIMESTAMP, Date.now().toString(10));
  };

  handleOrganizationSelect = ({ key }: Organization) => {
    this.setState({ selectedOrganization: key });
  };

  render() {
    const { almApplication, boundOrganizations, onProjectCreate } = this.props;

    if (boundOrganizations.length === 0) {
      return (
        <>
          <p className="spacer-bottom">
            {translate('onboarding.create_project.install_app_description', almApplication.key)}
          </p>
          <IdentityProviderLink
            className="display-inline-block"
            identityProvider={almApplication}
            onClick={this.handleInstallAppClick}
            small={true}
            url={almApplication.installationUrl}>
            {translate(
              'onboarding.import_organization.choose_organization_button',
              almApplication.key
            )}
          </IdentityProviderLink>
        </>
      );
    }

    const { selectedOrganization } = this.state;
    return (
      <>
        <OrganizationInput
          autoImport={true}
          onChange={this.handleOrganizationSelect}
          organization={selectedOrganization}
          organizations={this.props.boundOrganizations}
        />
        {selectedOrganization && (
          <RemoteRepositories
            almApplication={almApplication}
            onProjectCreate={onProjectCreate}
            organization={selectedOrganization}
          />
        )}
      </>
    );
  }
}
