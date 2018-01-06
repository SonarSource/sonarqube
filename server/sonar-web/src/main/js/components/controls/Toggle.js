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
import classNames from 'classnames';
import './styles.css';

export default class Toggle extends React.PureComponent {
  static propTypes = {
    value: PropTypes.oneOfType([PropTypes.string, PropTypes.bool]).isRequired,
    name: PropTypes.string,
    onChange: PropTypes.func
  };

  handleClick(e, value) {
    e.preventDefault();
    e.currentTarget.blur();
    if (this.props.onChange) {
      this.props.onChange(!value);
    }
  }

  render() {
    const { value } = this.props;
    const booleanValue = typeof value === 'string' ? value === 'true' : value;

    const className = classNames('boolean-toggle', { 'boolean-toggle-on': booleanValue });

    return (
      <button
        className={className}
        name={this.props.name}
        onClick={e => this.handleClick(e, booleanValue)}>
        <div className="boolean-toggle-handle" />
      </button>
    );
  }
}
