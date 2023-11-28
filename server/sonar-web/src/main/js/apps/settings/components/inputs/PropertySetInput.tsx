/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import {
  ActionCell,
  ContentCell,
  DestructiveIcon,
  Note,
  Table,
  TableRow,
  TrashIcon,
} from 'design-system';
import * as React from 'react';
import { translateWithParameters } from '../../../../helpers/l10n';
import {
  DefaultSpecializedInputProps,
  getEmptyValue,
  getPropertyName,
  getUniqueName,
  isCategoryDefinition,
} from '../../utils';
import PrimitiveInput from './PrimitiveInput';

export default class PropertySetInput extends React.PureComponent<DefaultSpecializedInputProps> {
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
    const { setting, isDefault } = this.props;
    const { definition } = setting;

    return (
      <TableRow key={index}>
        {isCategoryDefinition(definition) &&
          definition.fields.map((field) => {
            const newSetting = {
              ...setting,
              definition: field,
              value: fieldValues[field.key],
            };
            return (
              <ContentCell className="sw-py-2 sw-border-0" key={field.key}>
                <PrimitiveInput
                  isDefault={isDefault}
                  hasValueChanged={this.props.hasValueChanged}
                  name={getUniqueName(definition, field.key)}
                  onChange={(value) => this.handleInputChange(index, field.key, value)}
                  setting={newSetting}
                  size="auto"
                  value={fieldValues[field.key]}
                />
              </ContentCell>
            );
          })}
        <ActionCell className="sw-border-0">
          {!isLast && (
            <DestructiveIcon
              Icon={TrashIcon}
              aria-label={translateWithParameters(
                'settings.definitions.delete_fields',
                getPropertyName(setting.definition),
                index,
              )}
              className="js-remove-value"
              onClick={() => this.handleDeleteValue(index)}
            />
          )}
        </ActionCell>
      </TableRow>
    );
  }

  render() {
    const { definition } = this.props.setting;
    const displayedValue = [...this.ensureValue(), ...getEmptyValue(definition)];

    const columnWidths = (isCategoryDefinition(definition) ? definition.fields : [])
      .map(() => 'auto')
      .concat('1px');

    return (
      <div>
        <Table
          header={
            <TableRow>
              {isCategoryDefinition(definition) &&
                definition.fields.map((field) => (
                  <ContentCell key={field.key}>
                    <div className="sw-text-start sw-h-full">
                      {field.name}
                      {field.description != null && (
                        <Note as="p" className="sw-mt-2">
                          {field.description}
                        </Note>
                      )}
                    </div>
                  </ContentCell>
                ))}
              <ContentCell />
            </TableRow>
          }
          columnCount={columnWidths.length}
          columnWidths={columnWidths}
          noHeaderTopBorder
          noSidePadding
        >
          {displayedValue.map((fieldValues, index) =>
            this.renderFields(fieldValues, index, index === displayedValue.length - 1),
          )}
        </Table>
      </div>
    );
  }
}
