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
import Select from 'react-select';
import { some } from 'lodash';
import { translate } from '../../../helpers/l10n';

export default class Form extends React.PureComponent {
  static propTypes = {
    allGates: PropTypes.array.isRequired,
    gate: PropTypes.object,
    onChange: PropTypes.func.isRequired
  };

  state = {
    loading: false
  };

  componentWillMount() {
    this.handleChange = this.handleChange.bind(this);
    this.renderGateName = this.renderGateName.bind(this);
  }

  componentDidUpdate(prevProps) {
    if (prevProps.gate !== this.props.gate) {
      this.stopLoading();
    }
  }

  stopLoading() {
    this.setState({ loading: false });
  }

  handleChange(option) {
    const { gate } = this.props;

    const isSet = gate == null && option.value != null;
    const isUnset = gate != null && option.value == null;
    const isChanged = gate != null && gate.id !== option.value;
    const hasChanged = isSet || isUnset || isChanged;

    if (hasChanged) {
      this.setState({ loading: true });
      this.props.onChange(gate && gate.id, option.value);
    }
  }

  renderGateName(gateOption) {
    if (gateOption.isDefault) {
      return (
        <span>
          <strong>
            {translate('default')}
          </strong>
          {': '}
          {gateOption.label}
        </span>
      );
    }

    return gateOption.label;
  }

  renderSelect() {
    const { gate, allGates } = this.props;

    const options = allGates.map(gate => ({
      value: gate.id,
      label: gate.name,
      isDefault: gate.isDefault
    }));

    const hasDefault = some(allGates, gate => gate.isDefault);
    if (!hasDefault) {
      options.unshift({
        value: null,
        label: translate('none')
      });
    }

    return (
      <Select
        options={options}
        valueRenderer={this.renderGateName}
        optionRenderer={this.renderGateName}
        value={gate && gate.id}
        clearable={false}
        placeholder={translate('none')}
        style={{ width: 300 }}
        disabled={this.state.loading}
        onChange={this.handleChange.bind(this)}
      />
    );
  }

  render() {
    return (
      <div>
        {this.renderSelect()}
        {this.state.loading && <i className="spinner spacer-left" />}
      </div>
    );
  }
}
