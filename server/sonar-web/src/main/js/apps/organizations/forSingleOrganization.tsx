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
import { withRouter, WithRouterProps } from 'react-router';
import { areThereCustomOrganizations, Store } from '../../store/rootReducer';

type ReactComponent<P> = React.ComponentClass<P> | React.StatelessComponent<P>;

export default function forSingleOrganization<P>(ComposedComponent: ReactComponent<P>) {
  interface StateProps {
    customOrganizations: boolean | undefined;
  }

  class ForSingleOrganization extends React.Component<StateProps & WithRouterProps> {
    static displayName = `forSingleOrganization(${ComposedComponent.displayName})}`;

    render() {
      const { customOrganizations, router, ...other } = this.props;

      if (!other.params.organizationKey && customOrganizations) {
        router.replace('/not_found');
        return null;
      }

      return <ComposedComponent {...other} />;
    }
  }

  const mapStateToProps = (state: Store) => ({
    customOrganizations: areThereCustomOrganizations(state)
  });

  return connect(mapStateToProps)(withRouter(ForSingleOrganization));
}
