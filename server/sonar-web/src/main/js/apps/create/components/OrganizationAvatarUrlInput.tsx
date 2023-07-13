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
import classNames from 'classnames';
import { isWebUri } from 'valid-url';
import { translate } from "../../../helpers/l10n";
import ValidationInput from "../../../components/controls/ValidationInput";

interface Props {
  initialValue?: string;
  onChange: (value: string | undefined) => void;
}

interface State {
  editing: boolean;
  error?: string;
  touched: boolean;
  value: string;
}

export default class OrganizationAvatarUrlInput extends React.PureComponent<Props, State> {
  state: State = {error: undefined, editing: false, touched: false, value: ''};

  componentDidMount() {
    if (this.props.initialValue) {
      const value = this.props.initialValue;
      const error = this.validateUrl(value);
      this.setState({error, touched: Boolean(error), value});
    }
  }

  handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.currentTarget.value.trim();
    const error = this.validateUrl(value);
    this.setState({error, touched: true, value});
    this.props.onChange(error === undefined ? value : undefined);
  };

  handleBlur = () => {
    this.setState({editing: false});
  };

  handleFocus = () => {
    this.setState({editing: true});
  };

  validateUrl(url: string) {
    if (url.length > 0 && !isWebUri(url)) {
      return translate('onboarding.create_organization.url.error');
    }
    return undefined;
  }

  render() {
    const isInvalid = this.state.touched && !this.state.editing && this.state.error !== undefined;
    const isValid = this.state.touched && this.state.error === undefined && this.state.value !== '';
    return (
        <ValidationInput
            error={this.state.error}
            isInvalid={isInvalid}
            isValid={isValid}
            labelHtmlFor="organization-avatar"
            label={translate('onboarding.create_organization.avatar')}>
          <input
              className={classNames('input-super-large', 'text-middle', {
                'is-invalid': isInvalid,
                'is-valid': isValid
              })}
              id="organization-avatar"
              onBlur={this.handleBlur}
              onChange={this.handleChange}
              onFocus={this.handleFocus}
              type="text"
              value={this.state.value}
          />
        </ValidationInput>
    );
  }
}
