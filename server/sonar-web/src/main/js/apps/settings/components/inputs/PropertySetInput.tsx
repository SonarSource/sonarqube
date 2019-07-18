/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { DeleteButton } from 'sonar-ui-common/components/controls/buttons';
import { DefaultInputProps, getEmptyValue, getUniqueName, isCategoryDefinition } from '../../utils';
import PrimitiveInput from './PrimitiveInput';

export default class PropertySetInput extends React.PureComponent<DefaultInputProps> {
  ensureValue() {
    return this.props.value || [];
  }

  handleDeleteValue = (index: number) => {
    const newValue = [...this.ensureValue()];
    newValue.splice(index, 1);
    this.props.onChange(newValue);
  };

  handleInputChange = (index: number, fieldKey: string, value: any) => {
    const emptyValue = getEmptyValue(this.props.setting.definition)[0];
    const newValue = [...this.ensureValue()];
    const newFields = { ...emptyValue, ...newValue[index], [fieldKey]: value };
    newValue.splice(index, 1, newFields);
    return this.props.onChange(newValue);
  };

  renderFields(fieldValues: any, index: number, isLast: boolean) {
    const { setting } = this.props;
    const { definition } = setting;

    return (
      <tr key={index}>
        {isCategoryDefinition(definition) &&
          definition.fields.map(field => (
            <td key={field.key}>
              <PrimitiveInput
                hasValueChanged={this.props.hasValueChanged}
                name={getUniqueName(definition, field.key)}
                onChange={value => this.handleInputChange(index, field.key, value)}
                setting={{ ...setting, definition: field, value: fieldValues[field.key] }}
                value={fieldValues[field.key]}
              />
            </td>
          ))}
        <td className="thin nowrap text-middle">
          {!isLast && (
            <DeleteButton
              className="js-remove-value"
              onClick={() => this.handleDeleteValue(index)}
            />
          )}
        </td>
      </tr>
    );
  }

  render() {
    const { definition } = this.props.setting;
    const displayedValue = [...this.ensureValue(), ...getEmptyValue(definition)];

    return (
      <div>
        <table
          className="data zebra-hover no-outer-padding"
          style={{ width: 'auto', minWidth: 480, marginTop: -12 }}>
          <thead>
            <tr>
              {isCategoryDefinition(definition) &&
                definition.fields.map(field => (
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
