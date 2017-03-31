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
import PrimitiveInput from './PrimitiveInput';
import { getEmptyValue } from '../../utils';

export default class MultiValueInput extends React.Component {
  static propTypes = {
    setting: React.PropTypes.object.isRequired,
    value: React.PropTypes.array,
    onChange: React.PropTypes.func.isRequired
  };

  ensureValue() {
    return this.props.value || [];
  }

  handleSingleInputChange(index, value) {
    const newValue = [...this.ensureValue()];
    newValue.splice(index, 1, value);
    this.props.onChange(newValue);
  }

  handleDeleteValue(e, index) {
    e.preventDefault();
    e.target.blur();

    const newValue = [...this.ensureValue()];
    newValue.splice(index, 1);
    this.props.onChange(newValue);
  }

  prepareSetting() {
    const { setting } = this.props;
    const newDefinition = { ...setting.definition, multiValues: false };
    return {
      ...setting,
      definition: newDefinition,
      values: undefined
    };
  }

  renderInput(value, index, isLast) {
    return (
      <li key={index} className="spacer-bottom">
        <PrimitiveInput
          setting={this.prepareSetting()}
          value={value}
          onChange={this.handleSingleInputChange.bind(this, index)}
        />

        {!isLast &&
          <div className="display-inline-block spacer-left">
            <button
              className="js-remove-value button-clean"
              onClick={e => this.handleDeleteValue(e, index)}>
              <i className="icon-delete" />
            </button>
          </div>}
      </li>
    );
  }

  render() {
    const displayedValue = [...this.ensureValue(), ...getEmptyValue(this.props.setting.definition)];

    return (
      <div>
        <ul>
          {displayedValue.map((value, index) =>
            this.renderInput(value, index, index === displayedValue.length - 1))}
        </ul>
      </div>
    );
  }
}
