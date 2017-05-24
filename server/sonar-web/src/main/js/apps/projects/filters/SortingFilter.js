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
import React from 'react';
import classNames from 'classnames';
import { Link } from 'react-router';
import { getFilterUrl } from './utils';
import { translate } from '../../../helpers/l10n';

export default class SortingFilter extends React.PureComponent {
  static propTypes = {
    property: React.PropTypes.string.isRequired,
    query: React.PropTypes.object.isRequired,
    isFavorite: React.PropTypes.bool,
    organization: React.PropTypes.object,
    sortDesc: React.PropTypes.oneOf(['left', 'right']),
    leftText: React.PropTypes.string,
    rightText: React.PropTypes.string
  };

  static defaultProps = {
    sortDesc: 'left'
  };

  isSortActive(side) {
    const { sort } = this.props.query;
    if (sort && sort[0] === '-') {
      return sort.substr(1) === this.props.property && side === this.props.sortDesc;
    } else {
      return sort === this.props.property && side !== this.props.sortDesc;
    }
  }

  getLinkClass(side) {
    return classNames('button button-small button-grey', {
      'button-active': this.isSortActive(side)
    });
  }

  getLinkPath(side) {
    if (this.isSortActive(side)) {
      return getFilterUrl(this.props, { sort: null });
    }
    return getFilterUrl(this.props, {
      sort: (this.props.sortDesc === side ? '-' : '') + this.props.property
    });
  }

  blurLink(event) {
    event.target.blur();
  }

  render() {
    const { leftText, rightText } = this.props;

    return (
      <div className="projects-facet-sort">
        <span>{translate('projects.sort_list')}</span>
        <div className="spacer-left button-group">
          <Link
            onClick={this.blurLink}
            className={this.getLinkClass('left')}
            to={this.getLinkPath('left')}>
            {leftText || translate('worst')}
          </Link>
          <Link
            onClick={this.blurLink}
            className={this.getLinkClass('right')}
            to={this.getLinkPath('right')}>
            {rightText || translate('best')}
          </Link>
        </div>
      </div>
    );
  }
}
