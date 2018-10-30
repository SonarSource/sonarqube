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
import { FormattedMessage } from 'react-intl';
import AutoOrganizationBind from './AutoOrganizationBind';
import ChooseRemoteOrganizationStep from './ChooseRemoteOrganizationStep';
import OrganizationDetailsForm from './OrganizationDetailsForm';
import OrganizationDetailsStep from './OrganizationDetailsStep';
import RadioToggle from '../../../components/controls/RadioToggle';
import {
  AlmApplication,
  AlmOrganization,
  AlmUnboundApplication,
  Organization,
  OrganizationBase
} from '../../../app/types';
import { bindAlmOrganization } from '../../../api/alm-integration';
import { sanitizeAlmId } from '../../../helpers/almIntegrations';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/urls';

export enum Filters {
  Bind = 'bind',
  Create = 'create'
}

interface Props {
  almApplication: AlmApplication;
  almInstallId?: string;
  almOrganization?: AlmOrganization;
  almUnboundApplications: AlmUnboundApplication[];
  boundOrganization?: OrganizationBase;
  createOrganization: (
    organization: OrganizationBase & { installationId?: string }
  ) => Promise<Organization>;
  onOrgCreated: (organization: string, justCreated?: boolean) => void;
  unboundOrganizations: Organization[];
}

interface State {
  filter?: Filters;
}

export default class AutoOrganizationCreate extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      filter: props.unboundOrganizations.length === 0 ? Filters.Create : undefined
    };
  }

  handleOptionChange = (filter: Filters) => {
    this.setState({ filter });
  };

  handleCreateOrganization = (organization: Required<OrganizationBase>) => {
    return this.props
      .createOrganization({
        avatar: organization.avatar,
        description: organization.description,
        installationId: this.props.almInstallId,
        key: organization.key,
        name: organization.name || organization.key,
        url: organization.url
      })
      .then(({ key }) => this.props.onOrgCreated(key));
  };

  handleBindOrganization = (organization: string) => {
    if (this.props.almInstallId) {
      return bindAlmOrganization({
        organization,
        installationId: this.props.almInstallId
      }).then(() => this.props.onOrgCreated(organization, false));
    }
    return Promise.reject();
  };

  render() {
    const {
      almApplication,
      almInstallId,
      almOrganization,
      boundOrganization,
      unboundOrganizations
    } = this.props;
    if (almInstallId && almOrganization && !boundOrganization) {
      const { filter } = this.state;
      const hasUnboundOrgs = unboundOrganizations.length > 0;
      return (
        <OrganizationDetailsStep
          finished={false}
          onOpen={() => {}}
          open={true}
          organization={almOrganization}>
          <div className="huge-spacer-bottom">
            <p className="big-spacer-bottom">
              <FormattedMessage
                defaultMessage={translate('onboarding.import_organization_x')}
                id="onboarding.import_organization_x"
                values={{
                  avatar: (
                    <img
                      alt={almApplication.name}
                      className="little-spacer-left"
                      src={`${getBaseUrl()}/images/sonarcloud/${sanitizeAlmId(
                        almApplication.key
                      )}.svg`}
                      width={16}
                    />
                  ),
                  name: <strong>{almOrganization.name}</strong>
                }}
              />
            </p>

            {hasUnboundOrgs && (
              <RadioToggle
                name="filter"
                onCheck={this.handleOptionChange}
                options={[
                  {
                    label: translate('onboarding.import_organization.create_new'),
                    value: Filters.Create
                  },
                  {
                    label: translate('onboarding.import_organization.bind_existing'),
                    value: Filters.Bind
                  }
                ]}
                value={filter}
              />
            )}
          </div>

          {filter === Filters.Create && (
            <OrganizationDetailsForm
              onContinue={this.handleCreateOrganization}
              organization={almOrganization}
              submitText={translate('onboarding.import_organization.import')}
            />
          )}
          {filter === Filters.Bind && (
            <AutoOrganizationBind
              onBindOrganization={this.handleBindOrganization}
              unboundOrganizations={unboundOrganizations}
            />
          )}
        </OrganizationDetailsStep>
      );
    }

    return (
      <ChooseRemoteOrganizationStep
        almApplication={this.props.almApplication}
        almInstallId={almInstallId}
        almOrganization={almOrganization}
        almUnboundApplications={this.props.almUnboundApplications}
        boundOrganization={boundOrganization}
      />
    );
  }
}
