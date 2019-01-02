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
import * as theme from '../../../../app/theme';
import LockIcon from '../../../../components/icons-components/LockIcon';
import { Button } from '../../../../components/ui/buttons';
import { translate } from '../../../../helpers/l10n';
import { DefaultSpecializedInputProps } from '../../utils';

interface State {
  changing: boolean;
  value: string;
}

export default class InputForPassword extends React.PureComponent<
  DefaultSpecializedInputProps,
  State
> {
  state: State = {
    value: '',
    changing: false
  };

  componentDidUpdate(prevProps: DefaultSpecializedInputProps) {
    if (!prevProps.hasValueChanged && this.props.hasValueChanged) {
      this.setState({ changing: false, value: '' });
    }
  }

  handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.props.onChange(event.target.value);
    this.setState({ changing: true, value: event.target.value });
  };

  handleChangeClick = () => {
    this.setState({ changing: true });
  };

  renderInput() {
    return (
      <form>
        <input className="hidden" type="password" />
        <input
          autoComplete="off"
          autoFocus={this.state.changing}
          className="js-password-input settings-large-input text-top"
          name={this.props.name}
          onChange={this.handleInputChange}
          type="password"
          value={this.state.value}
        />
      </form>
    );
  }

  render() {
    const hasValue = !!this.props.value;

    if (this.state.changing || !hasValue) {
      return this.renderInput();
    }

    return (
      <>
        <LockIcon className="text-middle big-spacer-right" fill={theme.gray60} />
        <Button className="text-middle" onClick={this.handleChangeClick}>
          {translate('change_verb')}
        </Button>
      </>
    );
  }
}
