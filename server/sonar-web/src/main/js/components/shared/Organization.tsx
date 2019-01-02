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
import { connect } from 'react-redux';
import { getOrganizationByKey, areThereCustomOrganizations, Store } from '../../store/rootReducer';
import OrganizationLink from '../ui/OrganizationLink';

interface OwnProps {
  organizationKey: string;
}

interface Props {
  link?: boolean;
  linkClassName?: string;
  organization: { key: string; name: string } | null;
  shouldBeDisplayed?: boolean;
}

function Organization(props: Props) {
  const { link = true, organization, shouldBeDisplayed } = props;

  if (!shouldBeDisplayed || !organization) {
    return null;
  }

  return (
    <span>
      {link ? (
        <OrganizationLink className={props.linkClassName} organization={organization}>
          {organization.name}
        </OrganizationLink>
      ) : (
        organization.name
      )}
      <span className="slash-separator" />
    </span>
  );
}

const mapStateToProps = (state: Store, ownProps: OwnProps) => ({
  organization: getOrganizationByKey(state, ownProps.organizationKey),
  shouldBeDisplayed: areThereCustomOrganizations(state)
});

export default connect(mapStateToProps)(Organization);

export const UnconnectedOrganization = Organization;
