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

export default class BubblePopup extends React.PureComponent {
  static propsType = {
    children: PropTypes.object.isRequired,
    position: PropTypes.object.isRequired,
    customClass: PropTypes.string
  };

  static defaultProps = {
    customClass: ''
  };

  render() {
    const popupClass = classNames('bubble-popup', this.props.customClass);
    const popupStyle = { ...this.props.position };

    return (
      <div className={popupClass} style={popupStyle}>
        {this.props.children}
        <div className="bubble-popup-arrow" />
      </div>
    );
  }
}
