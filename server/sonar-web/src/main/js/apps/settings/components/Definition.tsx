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
import * as React from 'react';
import { getValue, resetSettingValue, setSettingValue } from '../../../api/settings';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { parseError } from '../../../helpers/request';
import { ExtendedSettingDefinition, SettingType, SettingValue } from '../../../types/settings';
import { Component } from '../../../types/types';
import { isEmptyValue, isURLKind } from '../utils';
import DefinitionRenderer from './DefinitionRenderer';

interface Props {
  component?: Component;
  definition: ExtendedSettingDefinition;
  initialSettingValue?: SettingValue;
  onUpdate?: () => void;
}

interface State {
  changedValue?: string;
  isEditing: boolean;
  loading: boolean;
  success: boolean;
  validationMessage?: string;
  settingValue?: SettingValue;
}

const SAFE_SET_STATE_DELAY = 3000;

export default class Definition extends React.PureComponent<Props, State> {
  timeout?: number;
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      isEditing: false,
      loading: false,
      success: false,
      settingValue: props.initialSettingValue,
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
    clearTimeout(this.timeout);
  }

  handleChange = (changedValue: any) => {
    clearTimeout(this.timeout);

    this.setState({ changedValue, success: false }, this.handleCheck);
  };

  handleReset = async () => {
    const { component, definition } = this.props;

    this.setState({ loading: true, success: false });

    try {
      await resetSettingValue({ keys: definition.key, component: component?.key });
      const settingValue = await getValue({ key: definition.key, component: component?.key });

      if (this.props.onUpdate) {
        this.props.onUpdate();
      }

      this.setState({
        changedValue: undefined,
        loading: false,
        success: true,
        validationMessage: undefined,
        settingValue,
      });

      this.timeout = window.setTimeout(
        () => this.setState({ success: false }),
        SAFE_SET_STATE_DELAY
      );
    } catch (e) {
      const validationMessage = await parseError(e as Response);
      this.setState({ loading: false, validationMessage });
    }
  };

  handleCancel = () => {
    this.setState({ changedValue: undefined, validationMessage: undefined, isEditing: false });
  };

  handleCheck = () => {
    const { definition } = this.props;
    const { changedValue } = this.state;

    if (isEmptyValue(definition, changedValue)) {
      if (definition.defaultValue === undefined) {
        this.setState({
          validationMessage: translate('settings.state.value_cant_be_empty_no_default'),
        });
      } else {
        this.setState({ validationMessage: translate('settings.state.value_cant_be_empty') });
      }
      return false;
    }

    if (isURLKind(definition)) {
      try {
        // eslint-disable-next-line no-new
        new URL(changedValue ?? '');
      } catch (e) {
        this.setState({
          validationMessage: translateWithParameters(
            'settings.state.url_not_valid',
            changedValue ?? ''
          ),
        });
        return false;
      }
    }

    if (definition.type === SettingType.JSON) {
      try {
        JSON.parse(changedValue ?? '');
      } catch (e) {
        this.setState({ validationMessage: (e as Error).message });

        return false;
      }
    }

    this.setState({ validationMessage: undefined });
    return true;
  };

  handleEditing = () => {
    this.setState({ isEditing: true });
  };

  handleSave = async () => {
    const { component, definition } = this.props;
    const { changedValue } = this.state;

    if (changedValue !== undefined) {
      this.setState({ success: false });

      if (isEmptyValue(definition, changedValue)) {
        this.setState({ validationMessage: translate('settings.state.value_cant_be_empty') });

        return;
      }

      this.setState({ loading: true });

      try {
        await setSettingValue(definition, changedValue, component?.key);
        const settingValue = await getValue({ key: definition.key, component: component?.key });

        if (this.props.onUpdate) {
          this.props.onUpdate();
        }

        this.setState({
          changedValue: undefined,
          isEditing: false,
          loading: false,
          success: true,
          settingValue,
        });

        this.timeout = window.setTimeout(
          () => this.setState({ success: false }),
          SAFE_SET_STATE_DELAY
        );
      } catch (e) {
        const validationMessage = await parseError(e as Response);
        this.setState({ loading: false, validationMessage });
      }
    }
  };

  render() {
    const { definition } = this.props;
    return (
      <DefinitionRenderer
        definition={definition}
        onCancel={this.handleCancel}
        onChange={this.handleChange}
        onEditing={this.handleEditing}
        onReset={this.handleReset}
        onSave={this.handleSave}
        {...this.state}
      />
    );
  }
}
