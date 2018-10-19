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
import { connect } from 'react-redux';
import { Store, getMyOrganizations } from '../../store/rootReducer';
import { fetchMyOrganizations } from '../../apps/account/organizations/actions';
import { Organization } from '../../app/types';

export function withUserOrganizations<P>(
  WrappedComponent: React.ComponentClass<
    P & {
      personalOrganization?: Organization;
      userOrganizations: Organization[];
    }
  >
) {
  type Props = P & { fetchMyOrganizations: () => Promise<void>; userOrganizations: Organization[] };
  const wrappedDisplayName = WrappedComponent.displayName || WrappedComponent.name || 'Component';

  class Wrapper extends React.Component<Props> {
    static displayName = `withUserOrganizations(${wrappedDisplayName})`;

    componentDidMount() {
      this.props.fetchMyOrganizations();
    }

    render() {
      // @ts-ignore Rest operator not supported yet by TS for generics
      const { fetchMyOrganizations, ...other } = this.props;
      return <WrappedComponent {...other} />;
    }
  }

  const mapDispatchToProps = { fetchMyOrganizations };

  function mapStateToProps(state: Store) {
    return { userOrganizations: getMyOrganizations(state) };
  }

  return connect(
    mapStateToProps,
    mapDispatchToProps
  )(Wrapper);
}
