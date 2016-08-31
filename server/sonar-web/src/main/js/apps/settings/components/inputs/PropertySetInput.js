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
import classNames from 'classnames';
import renderInput from './renderInput';
import { translate } from '../../../../helpers/l10n';
import { getEmptyValue, getSettingValue, isDefaultOrInherited } from '../../utils';

export default class PropertySetInput extends React.Component {
  static propTypes = {
    setting: React.PropTypes.object.isRequired,
    onChange: React.PropTypes.func.isRequired
  };

  constructor (props) {
    super(props);
    this.state = {
      fieldValues: getSettingValue(props.setting) || [],
      removedIndexes: []
    };
  }

  componentWillReceiveProps (nextProps) {
    const isDefault = isDefaultOrInherited(this.props.setting);
    const willBeDefault = isDefaultOrInherited(nextProps.setting);

    if (isDefault !== willBeDefault) {
      this.setState({ fieldValues: getSettingValue(nextProps.setting) || [] });
    }
  }

  handleChange (fieldValues, removedIndexes) {
    const filtered = fieldValues.filter((_, index) => !removedIndexes.includes(index));
    return this.props.onChange(undefined, filtered);
  }

  handleAddValue (e) {
    e.preventDefault();
    e.target.blur();

    const newValue = {};
    this.props.setting.definition.fields.forEach(field => {
      newValue[field.key] = getEmptyValue(field);
    });

    const fieldValues = [...this.state.fieldValues, newValue];
    this.setState({ fieldValues });
  }

  handleDeleteValue (e, index) {
    e.preventDefault();
    e.target.blur();

    // do not actually remove the input, but just hide it in the UI
    // it allows to use index as a key
    const removedIndexes = [...this.state.removedIndexes, index];
    this.setState({ removedIndexes });
    this.handleChange(this.state.fieldValues, removedIndexes);
  }

  handleInputChange (index, fieldKey, _, value) {
    const { fieldValues } = this.state;
    const newValues = [...fieldValues];
    newValues.splice(index, 1, { ...fieldValues[index], [fieldKey]: value });

    this.setState({ fieldValues: newValues });
    return this.handleChange(newValues, this.state.removedIndexes);
  }

  renderFields (fieldValues, index) {
    const { setting } = this.props;
    const className = classNames({ 'hidden': this.state.removedIndexes.includes(index) });

    return (
        <tr key={index} className={className}>
          {setting.definition.fields.map(field => (
              <td key={field.key}>
                {renderInput(
                    { definition: field, value: fieldValues[field.key] },
                    this.handleInputChange.bind(this, index, field.key))}
              </td>
          ))}
          <td className="thin nowrap">
            <button className="js-remove-value button-red" onClick={e => this.handleDeleteValue(e, index)}>
              {translate('delete')}
            </button>
          </td>
        </tr>
    );
  }

  render () {
    const { setting } = this.props;
    const { fieldValues } = this.state;

    return (
        <div>
          <table className="data no-outer-padding" style={{ width: 'auto', minWidth: 480, marginTop: -12 }}>
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
              {fieldValues.map((fieldValues, index) => this.renderFields(fieldValues, index))}
            </tbody>
          </table>

          <div className="spacer-top">
            <button className="js-add-value" onClick={e => this.handleAddValue(e)}>{translate('add_verb')}</button>
          </div>
        </div>
    );
  }
}
