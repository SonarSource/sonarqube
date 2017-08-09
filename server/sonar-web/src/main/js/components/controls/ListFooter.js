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
import classNames from 'classnames';
import React from 'react';
import PropTypes from 'prop-types';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { formatMeasure } from '../../helpers/measures';

export default class ListFooter extends React.PureComponent {
  static propTypes = {
    count: PropTypes.number.isRequired,
    total: PropTypes.number.isRequired,
    loadMore: PropTypes.func,
    ready: PropTypes.bool
  };

  static defaultProps = {
    ready: true
  };

  canLoadMore() {
    return typeof this.props.loadMore === 'function';
  }

  handleLoadMore = event => {
    event.preventDefault();
    event.target.blur();
    this.props.loadMore();
  };

  render() {
    const hasMore = this.props.total > this.props.count;
    const loadMoreLink = (
      <a className="spacer-left" href="#" onClick={this.handleLoadMore}>
        {translate('show_more')}
      </a>
    );
    const className = classNames('spacer-top note text-center', {
      'new-loading': !this.props.ready
    });

    return (
      <footer className={className}>
        {translateWithParameters(
          'x_of_y_shown',
          formatMeasure(this.props.count, 'INT'),
          formatMeasure(this.props.total, 'INT')
        )}
        {this.canLoadMore() && hasMore ? loadMoreLink : null}
      </footer>
    );
  }
}
