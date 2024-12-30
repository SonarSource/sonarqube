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
import classNames from 'classnames';
import {FlagMessage, FormField, InputField} from '~design-system';
import { debounce } from 'lodash';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';

interface Props {
  initialValue?: string;
  showHelpIcon: boolean;
  isEditMode: boolean;
  onChange: (value: string | undefined) => void;
}

interface State {
  error?: string;
  touched: boolean;
  validating: boolean;
  value: string;
}

export default class OrganizationNameInput extends React.PureComponent<Props, State> {
  mounted = false;
  constructor(props: Props) {
    super(props);
    this.state = { error: undefined, touched: false, validating: false, value: '' };
    this.checkFreeKey = debounce(this.checkFreeKey, 250);
  }

  componentDidMount() {
    this.mounted = true;
    if (this.props.initialValue !== undefined) {
      this.setState({ value: this.props.initialValue });
      this.validateKey(this.props.initialValue);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  checkFreeKey = (key: string | undefined) => {
    this.setState({ validating: false });
    this.setState({ error: undefined });
    this.props.onChange(key);
  };

  handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.currentTarget;
    this.setState({ touched: true, value });
    this.validateKey(value);
  };

  validateKey(key: string) {
    if (this.props.isEditMode) {
      if (key.length === 0) {
        this.setState({
          error: translate('onboarding.create_organization.organization_name.required'),
          touched: true,
        });
        this.props.onChange(undefined);
      } else if (key.length > 80 || !/^(?:[a-zA-Z0-9]+(?:[\s-][a-zA-Z0-9]+)*)$/.test(key)) {
        this.setState({
          error: translate('onboarding.create_organization.organization_name.error'),
          touched: true,
        });
        this.props.onChange(undefined);
      } else {
        this.checkFreeKey(key);
      }
    } else {
      if (key.length == 0) {
        this.checkFreeKey('');
      } else if (key.length > 80 || !/^(?:[a-zA-Z0-9]+(?:[\s-][a-zA-Z0-9]+)*)$/.test(key)) {
        this.setState({
          error: translate('onboarding.create_organization.organization_name.error'),
          touched: true,
        });
        this.props.onChange(undefined);
      } else {
        this.checkFreeKey(key);
      }
    }
  }

  render() {
    const isInvalid = this.state.touched && this.state.error !== undefined;
    const isValid = this.state.touched && !this.state.validating && this.state.error === undefined;
    return (
      <FormField
        htmlFor="organization-name"
        label={translate('organization.name')}
        description={translate('organization.name.description')}
        required
      >
        <InputField
          className={classNames('input-super-large', {
            'is-invalid': isInvalid,
            'is-valid': isValid,
          })}
          id="organization-name"
          maxLength={80}
          onChange={this.handleChange}
          type="text"
          value={this.state.value}
        />

        {isInvalid && (
            <FlagMessage id="it__error-message" className="sw-mt-2" variant="error">
              {this.state.error}
            </FlagMessage>
        )}
      </FormField>
    );
  }
}
