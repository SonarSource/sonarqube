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
import OrganizationAvatar from '../../../components/common/OrganizationAvatar';

interface Props {
  almApplication: AlmApplication;
  almInstallId?: string;
  almOrganization?: AlmOrganization;
  createOrganization: (
    organization: OrganizationBase & { installationId?: string }
  ) => Promise<Organization>;
  importPersonalOrg?: Organization;
  onOrgCreated: (organization: string) => void;
  updateOrganization: (
    organization: OrganizationBase & { installationId?: string }
  ) => Promise<Organization>;
}

export default class AutoOrganizationCreate extends React.PureComponent<Props> {
  handleCreateOrganization = (organization: Required<OrganizationBase>) => {
    if (organization) {
      const { importPersonalOrg } = this.props;
      let promise: Promise<Organization>;
      if (importPersonalOrg) {
        promise = this.props.updateOrganization({
          avatar: organization.avatar,
          description: organization.description,
          installationId: this.props.almInstallId,
          key: importPersonalOrg.key,
          name: organization.name || organization.key,
          url: organization.url
        });
      } else {
        promise = this.props.createOrganization({
          avatar: organization.avatar,
          description: organization.description,
          installationId: this.props.almInstallId,
          key: organization.key,
          name: organization.name || organization.key,
          url: organization.url
        });
      }
      return promise.then(({ key }) => this.props.onOrgCreated(key));
    } else {
      return Promise.reject();
    }
  };

  render() {
    const { almApplication, almInstallId, almOrganization, importPersonalOrg } = this.props;
    if (almInstallId && almOrganization) {
      const description = importPersonalOrg
        ? translate('onboarding.import_personal_organization_x')
        : translate('onboarding.import_organization_x');
      const submitText = importPersonalOrg
        ? translate('onboarding.import_organization.bind')
        : translate('my_account.create_organization');
      return (
        <OrganizationDetailsStep
          description={
            <p className="huge-spacer-bottom">
              <FormattedMessage
                defaultMessage={description}
                id={description}
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
                  name: <strong>{almOrganization.name}</strong>,
                  personalAvatar: importPersonalOrg && (
                    <OrganizationAvatar organization={importPersonalOrg} small={true} />
                  ),
                  personalName: importPersonalOrg && <strong>{importPersonalOrg.name}</strong>
                }}
              />
            </p>
          }
          finished={false}
          keyReadOnly={Boolean(importPersonalOrg)}
          onContinue={this.handleCreateOrganization}
          onOpen={() => {}}
          open={true}
          organization={importPersonalOrg || almOrganization}
          submitText={submitText}
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
