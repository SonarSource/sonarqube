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
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { formatMeasure } from '../../helpers/measures';
import './Level.css';

export default class Level extends React.PureComponent {
  static propTypes = {
    level: PropTypes.oneOf(['ERROR', 'WARN', 'OK']).isRequired,
    small: PropTypes.bool,
    muted: PropTypes.bool
  };

  static defaultProps = {
    small: false,
    muted: false
  };

  render() {
    const formatted = formatMeasure(this.props.level, 'LEVEL');
    const className = classNames('level', 'level-' + this.props.level, {
      'level-small': this.props.small,
      'level-muted': this.props.muted
    });
    return (
      <span className={className}>
        {formatted}
      </span>
    );
  }
}
