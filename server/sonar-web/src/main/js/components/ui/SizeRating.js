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
import classNames from 'classnames';
import { inRange } from 'lodash';
import './SizeRating.css';

export default class SizeRating extends React.PureComponent {
  /*:: props: {
    value: number,
    small?: boolean,
    muted?: boolean
  };
*/

  static defaultProps = {
    small: false,
    muted: false
  };

  render() {
    const { value } = this.props;

    if (value == null) {
      return <div className="size-rating size-rating-muted">&nbsp;</div>;
    }

    let letter;
    if (inRange(value, 0, 1000)) {
      letter = 'XS';
    } else if (inRange(value, 1000, 10000)) {
      letter = 'S';
    } else if (inRange(value, 10000, 100000)) {
      letter = 'M';
    } else if (inRange(value, 100000, 500000)) {
      letter = 'L';
    } else if (value >= 500000) {
      letter = 'XL';
    }

    const className = classNames('size-rating', {
      'size-rating-small': this.props.small,
      'size-rating-muted': this.props.muted
    });

    return (
      <div className={className}>
        {letter}
      </div>
    );
  }
}
