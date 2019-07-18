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
import { Location } from 'history';
import * as React from 'react';
import { connect } from 'react-redux';
import { withRouter, WithRouterProps } from 'react-router';
import { getHomePageUrl } from '../../helpers/urls';
import { isLoggedIn } from '../../helpers/users';
import { getCurrentUser, Store } from '../../store/rootReducer';

interface StateProps {
  currentUser: T.CurrentUser | undefined;
}

interface OwnProps {
  location: Location;
}

class Landing extends React.PureComponent<StateProps & OwnProps & WithRouterProps> {
  componentDidMount() {
    const { currentUser } = this.props;
    if (currentUser && isLoggedIn(currentUser)) {
      if (currentUser.homepage) {
        const homepage = getHomePageUrl(currentUser.homepage);
        this.props.router.replace(homepage);
      } else {
        this.props.router.replace('/projects');
      }
    } else {
      this.props.router.replace('/about');
    }
  }

  render() {
    return null;
  }
}

const mapStateToProps = (state: Store) => ({
  currentUser: getCurrentUser(state)
});

export default withRouter(connect(mapStateToProps)(Landing));
