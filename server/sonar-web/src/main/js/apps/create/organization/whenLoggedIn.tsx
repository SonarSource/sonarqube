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
import { withRouter, WithRouterProps } from 'react-router';
import { CurrentUser, isLoggedIn } from '../../../app/types';
import { Store, getCurrentUser } from '../../../store/rootReducer';

export function whenLoggedIn<P>(WrappedComponent: React.ComponentClass<P>) {
  class Wrapper extends React.Component<P & { currentUser: CurrentUser } & WithRouterProps> {
    static displayName = `whenLoggedIn(${WrappedComponent.displayName})`;

    componentDidMount() {
      if (!isLoggedIn(this.props.currentUser)) {
        const returnTo = window.location.pathname + window.location.search + window.location.hash;
        this.props.router.replace({
          pathname: '/sessions/new',
          query: { return_to: returnTo } // eslint-disable-line camelcase
        });
      }
    }

    render() {
      if (isLoggedIn(this.props.currentUser)) {
        return <WrappedComponent {...this.props} />;
      } else {
        return null;
      }
    }
  }

  function mapStateToProps(state: Store) {
    return { currentUser: getCurrentUser(state) };
  }

  return connect(mapStateToProps)(withRouter(Wrapper));
}
