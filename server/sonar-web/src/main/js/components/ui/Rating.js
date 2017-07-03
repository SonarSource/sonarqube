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
import { formatMeasure } from '../../helpers/measures';
import './Rating.css';

export default class Rating extends React.PureComponent {
  static propTypes = {
    className: React.PropTypes.string,
    value: (props, propName, componentName) => {
      // allow both numbers and strings
      const numberValue = Number(props[propName]);
      if (numberValue < 1 || numberValue > 5) {
        throw new Error(`Invalid prop "${propName}" passed to "${componentName}".`);
      }
    },
    small: React.PropTypes.bool,
    muted: React.PropTypes.bool
  };

  static defaultProps = {
    small: false,
    muted: false
  };

  render() {
    const formatted = formatMeasure(this.props.value, 'RATING');
    const className = classNames(
      'rating',
      'rating-' + formatted,
      {
        'rating-small': this.props.small,
        'rating-muted': this.props.muted
      },
      this.props.className
    );
    return <span className={className}>{formatted}</span>;
  }
}
