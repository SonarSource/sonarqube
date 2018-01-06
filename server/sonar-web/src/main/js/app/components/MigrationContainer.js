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
// @flow
import React from 'react';
import { withRouter } from 'react-router';
import GlobalLoading from './GlobalLoading';
import { getSystemStatus } from '../../api/system';

class MigrationContainer extends React.PureComponent {
  /*::
  props: {
    children?: React.Element<*>,
    router: { push: ({ pathname: string, query?: { return_to: string } }) => void }
  };
  */

  state = { loading: true };

  componentDidMount() {
    getSystemStatus().then(r => {
      if (r.status === 'UP') {
        this.setState({ loading: false });
      } else {
        this.props.router.push({
          pathname: '/maintenance',
          query: {
            return_to: window.location.pathname + window.location.search + window.location.hash
          }
        });
      }
    });
  }

  render() {
    if (this.state.loading) {
      return <GlobalLoading />;
    }

    return this.props.children;
  }
}

export default withRouter(MigrationContainer);
