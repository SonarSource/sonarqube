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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getOrganizationUrl } from '../../../helpers/urls';

function getOrganizationLink(org: T.Organization, i: number, organizations: T.Organization[]) {
  return (
    <span key={org.key}>
      <Link to={getOrganizationUrl(org.key)}>{org.name}</Link>
      {i < organizations.length - 1 && ', '}
    </span>
  );
}

export function ShowOrganizationsToTransferOrDelete({
  organizations
}: {
  organizations: T.Organization[];
}) {
  return (
    <>
      <p className="big-spacer-bottom">
        <FormattedMessage
          defaultMessage={translate('my_profile.delete_account.info.orgs_to_transfer_or_delete')}
          id="my_profile.delete_account.info.orgs_to_transfer_or_delete"
          values={{
            organizations: <>{organizations.map(getOrganizationLink)}</>
          }}
        />
      </p>

      <Alert className="big-spacer-bottom" variant="warning">
        <FormattedMessage
          defaultMessage={translate(
            'my_profile.delete_account.info.orgs_to_transfer_or_delete.info'
          )}
          id="my_profile.delete_account.info.orgs_to_transfer_or_delete.info"
          values={{
            link: (
              <a
                href="https://sieg.eu.ngrok.io/documentation/organizations/overview/#how-to-transfer-ownership-of-an-organization"
                rel="noopener noreferrer"
                target="_blank">
                {translate('my_profile.delete_account.info.orgs_to_transfer_or_delete.info.link')}
              </a>
            )
          }}
        />
      </Alert>
    </>
  );
}

export function ShowOrganizations({
  className,
  organizations
}: {
  className?: string;
  organizations: T.Organization[];
}) {
  const organizationsIAdministrate = organizations.filter(o => o.actions && o.actions.admin);

  return (
    <ul className={className}>
      <li className="spacer-bottom">{translate('my_profile.delete_account.info')}</li>

      <li className="spacer-bottom">
        <FormattedMessage
          defaultMessage={translate('my_profile.delete_account.data.info')}
          id="my_profile.delete_account.data.info"
          values={{
            help: (
              <a
                href="/documentation/user-guide/user-account/#delete-your-user-account"
                rel="noopener noreferrer"
                target="_blank">
                {translate('learn_more')}
              </a>
            )
          }}
        />
      </li>

      {organizations.length > 0 && (
        <li className="spacer-bottom">
          <FormattedMessage
            defaultMessage={translate('my_profile.delete_account.info.orgs.members')}
            id="my_profile.delete_account.info.orgs.members"
            values={{
              organizations: <>{organizations.map(getOrganizationLink)}</>
            }}
          />
        </li>
      )}

      {organizationsIAdministrate.length > 0 && (
        <li className="spacer-bottom">
          <FormattedMessage
            defaultMessage={translate('my_profile.delete_account.info.orgs.administrators')}
            id="my_profile.delete_account.info.orgs.administrators"
            values={{
              organizations: <>{organizationsIAdministrate.map(getOrganizationLink)}</>
            }}
          />
        </li>
      )}
    </ul>
  );
}

interface UserDeleteAccountContentProps {
  className?: string;
  organizationsSafeToDelete: T.Organization[];
  organizationsToTransferOrDelete: T.Organization[];
}

export default function UserDeleteAccountContent({
  className,
  organizationsSafeToDelete,
  organizationsToTransferOrDelete
}: UserDeleteAccountContentProps) {
  if (organizationsToTransferOrDelete.length > 0) {
    return <ShowOrganizationsToTransferOrDelete organizations={organizationsToTransferOrDelete} />;
  }

  return <ShowOrganizations className={className} organizations={organizationsSafeToDelete} />;
}
