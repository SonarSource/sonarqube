/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import * as React from 'react';
import { DeleteButton } from '../../../../components/controls/buttons';
import { DefaultSpecializedInputProps, getEmptyValue } from '../../utils';
import PrimitiveInput from './PrimitiveInput';

export default class MultiValueInput extends React.PureComponent<DefaultSpecializedInputProps> {
  ensureValue = () => {
    return this.props.value || [];
  };

  handleSingleInputChange = (index: number, value: any) => {
    const newValue = [...this.ensureValue()];
    newValue.splice(index, 1, value);
    this.props.onChange(newValue);
  };

  handleDeleteValue = (index: number) => {
    const newValue = [...this.ensureValue()];
    newValue.splice(index, 1);
    this.props.onChange(newValue);
  };

  renderInput(value: any, index: number, isLast: boolean) {
    const { setting, isDefault, name } = this.props;
    return (
      <li className="spacer-bottom" key={index}>
        <PrimitiveInput
          isDefault={isDefault}
          name={name}
          hasValueChanged={this.props.hasValueChanged}
          onChange={(value) => this.handleSingleInputChange(index, value)}
          setting={setting}
          value={value}
        />

        {!isLast && (
          <div className="display-inline-block spacer-left">
            <DeleteButton
              className="js-remove-value"
              onClick={() => this.handleDeleteValue(index)}
            />
          </div>
        )}
      </li>
    );
  }

  render() {
    const displayedValue = [...this.ensureValue(), ...getEmptyValue(this.props.setting.definition)];
    return (
      <div>
        <ul>
          {displayedValue.map((value, index) =>
            this.renderInput(value, index, index === displayedValue.length - 1)
          )}
        </ul>
      </div>
    );
  }
}
