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
import { getWrappedDisplayName } from './utils';
import { Store, getMyOrganizations } from '../../store/rootReducer';
import { fetchMyOrganizations } from '../../apps/account/organizations/actions';

interface OwnProps {
  fetchMyOrganizations: () => Promise<void>;
  userOrganizations: T.Organization[];
}

export function withUserOrganizations<P>(
  WrappedComponent: React.ComponentType<P & Partial<OwnProps>>
) {
  class Wrapper extends React.Component<P & OwnProps> {
    static displayName = getWrappedDisplayName(WrappedComponent, 'withUserOrganizations');

    componentDidMount() {
      this.props.fetchMyOrganizations();
    }

    render() {
      return <WrappedComponent {...this.props} />;
    }
  }

  const mapDispatchToProps = { fetchMyOrganizations: fetchMyOrganizations as any };

  function mapStateToProps(state: Store) {
    return { userOrganizations: getMyOrganizations(state) };
  }

  return connect(
    mapStateToProps,
    mapDispatchToProps
  )(Wrapper);
}
