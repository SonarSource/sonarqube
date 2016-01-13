/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import classNames from 'classnames';
import React from 'react';


export default React.createClass({
  propTypes: {
    count: React.PropTypes.number.isRequired,
    total: React.PropTypes.number.isRequired,
    loadMore: React.PropTypes.func.isRequired
  },

  render() {
    if (this.props.componentId) {
      return null;
    }
    let hasMore = this.props.total > this.props.count;
    let loadMoreLink = <a onClick={this.props.loadMore} className="spacer-left" href="#">show more</a>;
    let className = classNames('spacer-top note text-center', { 'new-loading': !this.props.ready });
    return (
        <footer className={className}>
          {this.props.count}/{this.props.total} shown
          {hasMore ? loadMoreLink : null}
        </footer>
    );
  }
});
