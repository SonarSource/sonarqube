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
import PrimitiveInput from './PrimitiveInput';
import { getEmptyValue, getUniqueName } from '../../utils';
import { DeleteButton } from '../../../../components/ui/buttons';

export default class PropertySetInput extends React.PureComponent {
  static propTypes = {
    setting: PropTypes.object.isRequired,
    value: PropTypes.array,
    onChange: PropTypes.func.isRequired
  };

  ensureValue() {
    return this.props.value || [];
  }

  getFieldName(field) {
    return getUniqueName(this.props.setting.definition, field.key);
  }

  handleDeleteValue(index) {
    const newValue = [...this.ensureValue()];
    newValue.splice(index, 1);
    this.props.onChange(newValue);
  }

  handleInputChange(index, fieldKey, value) {
    const emptyValue = getEmptyValue(this.props.setting.definition)[0];
    const newValue = [...this.ensureValue()];
    const newFields = { ...emptyValue, ...newValue[index], [fieldKey]: value };
    newValue.splice(index, 1, newFields);
    return this.props.onChange(newValue);
  }

  renderFields(fieldValues, index, isLast) {
    const { setting } = this.props;

    return (
      <tr key={index}>
        {setting.definition.fields.map(field => (
          <td key={field.key}>
            <PrimitiveInput
              name={this.getFieldName(field)}
              setting={{ definition: field, value: fieldValues[field.key] }}
              value={fieldValues[field.key]}
              onChange={this.handleInputChange.bind(this, index, field.key)}
            />
          </td>
        ))}
        <td className="thin nowrap text-middle">
          {!isLast && (
            <DeleteButton
              className="js-remove-value"
              onClick={this.handleDeleteValue.bind(this, index)}
            />
          )}
        </td>
      </tr>
    );
  }

  render() {
    const { setting } = this.props;

    const displayedValue = [...this.ensureValue(), ...getEmptyValue(this.props.setting.definition)];

    return (
      <div>
        <table
          className="data zebra-hover no-outer-padding"
          style={{ width: 'auto', minWidth: 480, marginTop: -12 }}>
          <thead>
            <tr>
              {setting.definition.fields.map(field => (
                <th key={field.key}>
                  {field.name}
                  {field.description != null && (
                    <span className="spacer-top small">{field.description}</span>
                  )}
                </th>
              ))}
              <th>&nbsp;</th>
            </tr>
          </thead>
          <tbody>
            {displayedValue.map((fieldValues, index) =>
              this.renderFields(fieldValues, index, index === displayedValue.length - 1)
            )}
          </tbody>
        </table>
      </div>
    );
  }
}
