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
import React from 'react';
import PropTypes from 'prop-types';

export default class Footer extends React.PureComponent {
  static propTypes = {
    count: PropTypes.number.isRequired,
    total: PropTypes.number.isRequired,
    loadMore: PropTypes.func.isRequired
  };

  loadMore = e => {
    e.preventDefault();
    this.props.loadMore();
  };

  renderLoadMoreLink = () => {
    const hasMore = this.props.total > this.props.count;
    if (!hasMore) {
      return null;
    }
    return (
      <a onClick={this.loadMore} className="spacer-left" href="#">
        show more
      </a>
    );
  };

  render() {
    return (
      <footer className="spacer-top note text-center">
        {this.props.count}/{this.props.total} shown
        {this.renderLoadMoreLink()}
      </footer>
    );
  }
}
