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
import { fetchOrganization } from '../../../store/rootActions';
import { getOrganizationByKey, Store } from '../../../store/rootReducer';
import NotFound from '../NotFound';
import Extension from './Extension';

interface StateToProps {
  organization?: T.Organization;
}

interface DispatchProps {
  fetchOrganization: (organizationKey: string) => void;
}

interface OwnProps {
  location: {};
  params: {
    extensionKey: string;
    organizationKey: string;
    pluginKey: string;
  };
}

type Props = OwnProps & StateToProps & DispatchProps;

class OrganizationPageExtension extends React.PureComponent<Props> {
  refreshOrganization = () => {
    return this.props.organization && this.props.fetchOrganization(this.props.organization.key);
  };

  render() {
    const { extensionKey, pluginKey } = this.props.params;
    const { organization } = this.props;

    if (!organization) {
      return null;
    }

    const { actions = {} } = organization;
    let { pages = [] } = organization;
    if (actions.admin && organization.adminPages) {
      pages = pages.concat(organization.adminPages);
    }

    const extension = pages.find(p => p.key === `${pluginKey}/${extensionKey}`);
    return extension ? (
      <Extension
        extension={extension}
        options={{ organization, refreshOrganization: this.refreshOrganization }}
      />
    ) : (
      <NotFound withContainer={false} />
    );
  }
}

const mapStateToProps = (state: Store, ownProps: OwnProps) => ({
  organization: getOrganizationByKey(state, ownProps.params.organizationKey)
});

const mapDispatchToProps = { fetchOrganization };

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(OrganizationPageExtension);
