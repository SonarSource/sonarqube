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
import ChooseRemoteOrganizationStep from './ChooseRemoteOrganizationStep';
import OrganizationDetailsStep from './OrganizationDetailsStep';
import {
  AlmApplication,
  AlmOrganization,
  OrganizationBase,
  Organization
} from '../../../app/types';
import { getBaseUrl } from '../../../helpers/urls';
import { translate } from '../../../helpers/l10n';
import { sanitizeAlmId } from '../../../helpers/almIntegrations';

interface Props {
  almApplication: AlmApplication;
  almInstallId?: string;
  almOrganization?: AlmOrganization;
  createOrganization: (
    organization: OrganizationBase & { installId?: string }
  ) => Promise<Organization>;
  onOrgCreated: (organization: string) => void;
}

export default class AutoOrganizationCreate extends React.PureComponent<Props> {
  handleCreateOrganization = (organization: Required<OrganizationBase>) => {
    if (organization) {
      return this.props
        .createOrganization({
          avatar: organization.avatar,
          description: organization.description,
          installId: this.props.almInstallId,
          key: organization.key,
          name: organization.name || organization.key,
          url: organization.url
        })
        .then(({ key }) => this.props.onOrgCreated(key));
    } else {
      return Promise.reject();
    }
  };

  render() {
    const { almApplication, almInstallId, almOrganization } = this.props;
    if (almInstallId && almOrganization) {
      return (
        <OrganizationDetailsStep
          description={
            <p className="huge-spacer-bottom">
              <FormattedMessage
                defaultMessage={translate('onboarding.create_organization.import_organization_x')}
                id="onboarding.create_organization.import_organization_x"
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
          }
          finished={false}
          onContinue={this.handleCreateOrganization}
          onOpen={() => {}}
          open={true}
          organization={almOrganization}
          submitText={translate('my_account.create_organization')}
        />
      );
    }
    return (
      <ChooseRemoteOrganizationStep
        almApplication={this.props.almApplication}
        almInstallId={almInstallId}
      />
    );
  }
}
