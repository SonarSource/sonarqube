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
import { debounce } from 'lodash';
import ValidationInput from "../../../components/controls/ValidationInput";
import {translate} from "../../../helpers/l10n";
import HelpTooltip from "../../../components/controls/HelpTooltip";

interface Props {
  initialValue?: string;
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
    console.log(this.props.initialValue);
    if (this.props.initialValue !== undefined) {
      this.setState({ value: this.props.initialValue });
      this.validateKey(this.props.initialValue);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  checkFreeKey = (key: string) => {
    this.setState({ validating: false });
    this.setState({ error: undefined});
    this.props.onChange(key)
  };

  handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.currentTarget;
    this.setState({ touched: true, value });
    this.validateKey(value);
  };

  validateKey(key: string) {
    if (key.length > 80 || !/^[a-z0-9][a-z0-9-]*[a-z0-9]?$/.test(key)) {
      this.setState({
        error: translate('onboarding.create_organization.organization_name.error'),
        touched: true
      });
      this.props.onChange(undefined);
    } else {
      this.checkFreeKey(key);
    }
  }

  render() {
    const isInvalid = this.state.touched && this.state.error !== undefined;
    const isValid = this.state.touched && !this.state.validating && this.state.error === undefined;

    return (
      <ValidationInput
        error={this.state.error}
        isInvalid={isInvalid}
        isValid={isValid}
        label={translate('onboarding.create_organization.organization_name')}>
        <div className="display-inline-flex-center">
          <input
            className={classNames('input-super-large', {
              'is-invalid': isInvalid,
              'is-valid': isValid
            })}
            id="organization-name"
            maxLength={80}
            onChange={this.handleChange}
            type="text"
            value={this.state.value}
          />
          <HelpTooltip
            className="little-spacer-left"
            overlay={
              <div className="big-padded-top big-padded-bottom">
                {translate('organization.name.description')}
              </div>
            }
          />
        </div>
      </ValidationInput>
    );
  }
}