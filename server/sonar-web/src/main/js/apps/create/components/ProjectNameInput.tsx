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
import * as classNames from 'classnames';
import ValidationInput from '../../../components/controls/ValidationInput';
import { translate } from '../../../helpers/l10n';

interface Props {
  className?: string;
  value?: string;
  onChange: (value: string | undefined) => void;
}

interface State {
  editing: boolean;
  error?: string;
  touched: boolean;
}

export default class ProjectNameInput extends React.PureComponent<Props, State> {
  state: State = { error: undefined, editing: false, touched: false };

  componentDidMount() {
    if (this.props.value) {
      const error = this.validateName(this.props.value);
      this.setState({ error, touched: Boolean(error) });
    }
  }

  handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.currentTarget;
    const error = this.validateName(value);
    this.setState({ error, touched: true });
    this.props.onChange(error === undefined ? value : undefined);
  };

  handleBlur = () => {
    this.setState({ editing: false });
  };

  handleFocus = () => {
    this.setState({ editing: true });
  };

  validateName(name: string) {
    if (name.length > 255) {
      return translate('onboarding.create_project.display_name.error');
    }
    return undefined;
  }

  render() {
    const isInvalid = this.state.touched && !this.state.editing && this.state.error !== undefined;
    const isValid = this.state.touched && this.state.error === undefined && this.props.value !== '';
    return (
      <ValidationInput
        className={this.props.className}
        description={translate('onboarding.create_project.display_name.description')}
        error={this.state.error}
        help={translate('onboarding.create_project.display_name.help')}
        id="project-name"
        isInvalid={isInvalid}
        isValid={isValid}
        label={translate('onboarding.create_project.display_name')}
        required={true}>
        <input
          className={classNames('input-super-large', {
            'is-invalid': isInvalid,
            'is-valid': isValid
          })}
          id="project-name"
          maxLength={500}
          minLength={1}
          onBlur={this.handleBlur}
          onChange={this.handleChange}
          onFocus={this.handleFocus}
          type="text"
          value={this.props.value || ''}
        />
      </ValidationInput>
    );
  }
}
