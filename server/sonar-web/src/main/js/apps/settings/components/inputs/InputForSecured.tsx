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
import { colors } from '../../../../app/theme';
import { Button } from '../../../../components/controls/buttons';
import LockIcon from '../../../../components/icons/LockIcon';
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
  input: React.ComponentType<
    React.PropsWithChildren<React.PropsWithChildren<DefaultSpecializedInputProps>>
  >;
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
        <input className="hidden" type="password" />
        <Input
          aria-label={getPropertyName(setting.definition)}
          autoComplete="off"
          className="js-setting-input settings-large-input"
          isDefault={isDefaultOrInherited(setting)}
          name={name}
          onChange={this.handleInputChange}
          setting={setting}
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
      <>
        <LockIcon className="text-middle big-spacer-right" fill={colors.gray60} />
        <Button className="text-middle" onClick={this.handleChangeClick}>
          {translate('change_verb')}
        </Button>
      </>
    );
  }
}
