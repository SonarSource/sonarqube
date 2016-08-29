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
import { getSettingValue, isDefaultOrInherited, getEmptyValue } from '../../utils';
import { translate } from '../../../../helpers/l10n';

export default class MultiValueInput extends React.Component {
  static propTypes = {
    setting: React.PropTypes.object.isRequired,
    onChange: React.PropTypes.func.isRequired
  };

  constructor (props) {
    super(props);
    this.state = {
      values: getSettingValue(props.setting) || [],
      removedIndexes: []
    };
  }

  componentWillReceiveProps (nextProps) {
    const isDefault = isDefaultOrInherited(this.props.setting);
    const willBeDefault = isDefaultOrInherited(nextProps.setting);

    if (isDefault !== willBeDefault) {
      this.setState({ values: getSettingValue(nextProps.setting) || [] });
    }
  }

  handleChange (values, removedIndexes) {
    const filtered = values.filter((_, index) => !removedIndexes.includes(index));
    this.props.onChange(undefined, filtered);
  }

  handleSingleInputChange (index, _, value) {
    const values = this.state.values;
    const newValues = values ? [...values] : [];
    newValues.splice(index, 1, value);

    this.setState({ values: newValues });
    this.handleChange(newValues, this.state.removedIndexes);
  }

  handleAddValue (e) {
    e.preventDefault();
    e.target.blur();

    const values = [...this.state.values, getEmptyValue(this.props.setting.definition)];
    this.setState({ values });
  }

  handleDeleteValue (e, index) {
    e.preventDefault();
    e.target.blur();

    // do not actually remove the input, but just hide it in the UI
    // it allows to use index as a key
    const removedIndexes = [...this.state.removedIndexes, index];
    this.setState({ removedIndexes });
    this.handleChange(this.state.values, removedIndexes);
  }

  prepareSetting (value) {
    const { setting } = this.props;
    const newDefinition = { ...setting.definition, multiValues: false };
    return {
      ...setting,
      value,
      definition: newDefinition,
      values: undefined
    };
  }

  renderInput (value, index) {
    const className = classNames('spacer-bottom', {
      'hidden': this.state.removedIndexes.includes(index)
    });

    return (
        <li key={index} className={className}>
          {renderInput(
              this.prepareSetting(value),
              this.handleSingleInputChange.bind(this, index))}

          <button className="js-remove-value spacer-left button-red" onClick={e => this.handleDeleteValue(e, index)}>
            {translate('delete')}
          </button>
        </li>
    );
  }

  render () {
    return (
        <div>
          <ul>
            {this.state.values.map((value, index) => this.renderInput(value, index))}
          </ul>
          <button className="js-add-value" onClick={e => this.handleAddValue(e)}>{translate('add_verb')}</button>
        </div>
    );
  }
}
