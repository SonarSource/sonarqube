/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import IdentityProviderLink from 'sonar-ui-common/components/controls/IdentityProviderLink';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { save } from 'sonar-ui-common/helpers/storage';
import {
  ORGANIZATION_IMPORT_BINDING_IN_PROGRESS_TIMESTAMP,
  ORGANIZATION_IMPORT_REDIRECT_TO_PROJECT_TIMESTAMP
} from '../organization/utils';
import OrganizationInput from './OrganizationInput';
import RemoteRepositories from './RemoteRepositories';

interface Props {
  almApplication: T.AlmApplication;
  boundOrganizations: T.Organization[];
  onOrganizationUpgrade: () => void;
  onProjectCreate: (projectKeys: string[], organization: string) => void;
  organization?: string;
}

interface State {
  selectedOrganization: string;
}

export default class AutoProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = { selectedOrganization: this.getInitialSelectedOrganization(props) };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
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

  handleOrganizationSelect = ({ key }: T.Organization) => {
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
            backgroundColor={almApplication.backgroundColor}
            className="display-inline-block"
            iconPath={almApplication.iconPath}
            name={almApplication.name}
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
    const organization = boundOrganizations.find(o => o.key === selectedOrganization);

    return (
      <>
        <OrganizationInput
          autoImport={true}
          onChange={this.handleOrganizationSelect}
          organization={selectedOrganization}
          organizations={this.props.boundOrganizations}
        />
        {organization && (
          <RemoteRepositories
            almApplication={almApplication}
            onOrganizationUpgrade={this.props.onOrganizationUpgrade}
            onProjectCreate={onProjectCreate}
            organization={organization}
          />
        )}
      </>
    );
  }
}
