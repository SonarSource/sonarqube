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
import { Button } from '@sonarsource/echoes-react';
import { LockIcon } from 'design-system';
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import {
  DefaultInputProps,
  DefaultSpecializedInputProps,
  getPropertyName,
  getUniqueName,
  isDefaultOrInherited,
} from '../../utils';

interface State {
  changing: boolean;
}

interface Props extends DefaultInputProps {
  input: React.ComponentType<React.PropsWithChildren<DefaultSpecializedInputProps>>;
}

export default class InputForSecured extends React.PureComponent<Props, State> {
  state: State = {
    changing: !this.props.setting.hasValue,
  };

  componentDidUpdate(prevProps: Props) {
    /*
     * Reset `changing` if:
     *  - the value is reset (valueChanged -> !valueChanged)
     *     or
     *  - the value changes from outside the input (i.e. store update/reset/cancel)
     */
    if (
      (prevProps.hasValueChanged || this.props.setting.value !== prevProps.setting.value) &&
      !this.props.hasValueChanged
    ) {
      this.setState({ changing: !this.props.setting.hasValue });
    }
  }

  handleInputChange = (value: string) => {
    this.props.onChange(value);
  };

  handleChangeClick = () => {
    this.setState({ changing: true });
  };

  renderInput() {
    const { input: Input, setting, value } = this.props;
    const name = getUniqueName(setting.definition);
    return (
      // The input hidden will prevent browser asking for saving login information
      <>
        <input aria-hidden className="sw-hidden" tabIndex={-1} type="password" />
        <Input
          aria-label={getPropertyName(setting.definition)}
          autoComplete="off"
          className="js-setting-input"
          isDefault={isDefaultOrInherited(setting)}
          name={name}
          onChange={this.handleInputChange}
          setting={setting}
          size="large"
          type="password"
          value={value}
        />
      </>
    );
  }

  render() {
    if (this.state.changing) {
      return this.renderInput();
    }

    return (
      <div className="sw-flex sw-items-center">
        <LockIcon className="sw-mr-4" />
        <Button onClick={this.handleChangeClick}>{translate('change_verb')}</Button>
      </div>
    );
  }
}
