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
import * as React from 'react';
import { QualityGate } from '../../api/quality-gates';
import Select from '../../components/controls/Select';
import { translate } from '../../helpers/l10n';

interface Props {
  allGates: QualityGate[];
  gate?: QualityGate;
  onChange: (oldGate?: number, newGate?: number) => Promise<void>;
}

interface State {
  loading: boolean;
}

interface Option {
  isDefault?: boolean;
  label: string;
  value: string;
}

export default class Form extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  handleChange = (option: { value: string }) => {
    const { gate } = this.props;

    const isSet = gate == null && option.value != null;
    const isUnset = gate != null && option.value == null;
    const isChanged = gate != null && gate.id !== Number(option.value);
    const hasChanged = isSet || isUnset || isChanged;

    if (hasChanged) {
      this.setState({ loading: true });
      this.props
        .onChange(gate && gate.id, Number(option.value))
        .then(this.stopLoading, this.stopLoading);
    }
  };

  renderGateName = (option: { isDefault?: boolean; label: string }) => {
    if (option.isDefault) {
      return (
        <span>
          <strong>{translate('default')}</strong>
          {': '}
          {option.label}
        </span>
      );
    }

    return <span>{option.label}</span>;
  };

  renderSelect() {
    const { gate, allGates } = this.props;

    const options: Option[] = allGates.map(gate => ({
      value: String(gate.id),
      label: gate.name,
      isDefault: gate.isDefault
    }));

    return (
      <Select
        clearable={false}
        disabled={this.state.loading}
        onChange={this.handleChange}
        optionRenderer={this.renderGateName}
        options={options}
        style={{ width: 300 }}
        value={gate && String(gate.id)}
        valueRenderer={this.renderGateName}
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
