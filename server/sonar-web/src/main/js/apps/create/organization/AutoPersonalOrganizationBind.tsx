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
import OrganizationDetailsForm from './OrganizationDetailsForm';
import { Query } from './utils';
import { DeleteButton } from '../../../components/ui/buttons';
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
  almOrganization: AlmOrganization;
  importPersonalOrg: Organization;
  onOrgCreated: (organization: string) => void;
  updateOrganization: (
    organization: OrganizationBase & { installationId?: string }
  ) => Promise<Organization>;
  updateUrlQuery: (query: Partial<Query>) => void;
}

export default class AutoPersonalOrganizationBind extends React.PureComponent<Props> {
  handleCancelImport = () => {
    this.props.updateUrlQuery({ almInstallId: undefined, almKey: undefined });
  };

  handleCreateOrganization = (organization: Required<OrganizationBase>) => {
    return this.props
      .updateOrganization({
        avatar: organization.avatar,
        description: organization.description,
        installationId: this.props.almInstallId,
        key: this.props.importPersonalOrg.key,
        name: organization.name || organization.key,
        url: organization.url
      })
      .then(({ key }) => this.props.onOrgCreated(key));
  };

  render() {
    const { almApplication, importPersonalOrg } = this.props;
    return (
      <div className="boxed-group">
        <div className="boxed-group-inner">
          <div className="display-flex-center big-spacer-bottom">
            <FormattedMessage
              defaultMessage={translate('onboarding.import_personal_organization_x')}
              id="onboarding.import_personal_organization_x"
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
                name: <strong>{this.props.almOrganization.name}</strong>,
                personalAvatar: importPersonalOrg && (
                  <OrganizationAvatar organization={importPersonalOrg} small={true} />
                ),
                personalName: importPersonalOrg && <strong>{importPersonalOrg.name}</strong>
              }}
            />
            <DeleteButton className="little-spacer-left" onClick={this.handleCancelImport} />
          </div>
          <OrganizationDetailsForm
            keyReadOnly={true}
            onContinue={this.handleCreateOrganization}
            organization={importPersonalOrg}
            submitText={translate('onboarding.import_organization.bind')}
          />
        </div>
      </div>
    );
  }
}
