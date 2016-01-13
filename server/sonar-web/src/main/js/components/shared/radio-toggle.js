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
import React from 'react';

export default React.createClass({
  propTypes: {
    value: React.PropTypes.string,
    options: React.PropTypes.array.isRequired,
    name: React.PropTypes.string.isRequired,
    onCheck: React.PropTypes.func.isRequired
  },

  getDefaultProps: function () {
    return { disabled: false, value: null };
  },

  onChange(e) {
    let newValue = e.currentTarget.value;
    this.props.onCheck(newValue);
  },

  renderOption(option) {
    let checked = option.value === this.props.value;
    let htmlId = this.props.name + '__' + option.value;
    return (
        <li key={option.value}>
          <input onChange={this.onChange}
                 type="radio"
                 name={this.props.name}
                 value={option.value}
                 id={htmlId}
                 checked={checked}
                 disabled={this.props.disabled}/>
          <label htmlFor={htmlId}>{option.label}</label>
        </li>
    );
  },

  render() {
    let options = this.props.options.map(this.renderOption);
    return (
        <ul className="radio-toggle">{options}</ul>
    );
  }
});
