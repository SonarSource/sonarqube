/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import AllProjectsContainer from './AllProjectsContainer';
import { getCurrentUser } from '../../../store/rootReducer';
import { isFavoriteSet } from '../utils';

type Props = {
  currentUser: { isLoggedIn: boolean },
  location: { query: {} },
  router: { replace: (path: string) => void }
};

type State = {
  shouldByRedirected?: boolean
};

class DefaultPageSelector extends React.PureComponent {
  props: Props;
  state: State;

  constructor (props: Props) {
    super(props);
    this.state = {};
  }

  componentDidMount () {
    this.defineIfShouldBeRedirected();
  }

  componentDidUpdate () {
    if (this.state.shouldByRedirected === true) {
      this.props.router.replace('/projects/favorite');
    }
  }

  defineIfShouldBeRedirected () {
    const shouldByRedirected = Object.keys(this.props.location.query).length === 0 &&
      this.props.currentUser.isLoggedIn &&
      isFavoriteSet();
    this.setState({ shouldByRedirected });
  }

  render () {
    if (this.state.shouldByRedirected == null || this.state.shouldByRedirected === true) {
      return null;
    } else {
      return (
        <AllProjectsContainer
          isFavorite={false}
          location={this.props.location}
          user={this.props.currentUser}/>
      );
    }
  }
}

const mapStateToProps = state => ({
  currentUser: getCurrentUser(state)
});

export default connect(mapStateToProps)(withRouter(DefaultPageSelector));
