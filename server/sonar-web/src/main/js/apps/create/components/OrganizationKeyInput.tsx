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
import { ButtonIcon, ButtonVariety, IconQuestionMark } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { FormField, InputField } from 'design-system';
import { debounce } from 'lodash';
import * as React from 'react';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { checkOrganizationKeyExistence } from '../../../api/organizations';
import { translate } from '../../../helpers/l10n';
import { getHostUrl } from '../../../helpers/urls';

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

export default class OrganizationKeyInput extends React.PureComponent<Props, State> {
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

  checkFreeKey = (key: string) => {
    this.setState({ validating: true });
    return checkOrganizationKeyExistence(key)
      .then((exists) => {
        if (this.mounted && this.state.value === key) {
          if (exists) {
            this.setState({
              error: translate('onboarding.create_organization.organization_name.taken'),
              touched: true,
              validating: false,
            });
            this.props.onChange(undefined);
          } else {
            this.setState({ error: undefined, validating: false });
            this.props.onChange(key);
          }
        }
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ error: undefined, validating: false });
          this.props.onChange(key);
        }
      });
  };

  handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.currentTarget;
    this.setState({ touched: true, value });
    this.validateKey(value);
  };

  validateKey(key: string) {
    if (key.length > 255 || !/^[a-z0-9][a-z0-9-]*[a-z0-9]?$/.test(key)) {
      this.setState({
        error: translate('onboarding.create_organization.organization_name.error'),
        touched: true,
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
      <FormField
        error={this.state.error}
        isInvalid={isInvalid}
        isValid={isValid}
        label={translate('onboarding.create_organization.organization_name')}
        required
        className="sw-mt-2"
      >
        <div className="display-inline-flex-center">
          <span className="little-spacer-right nowrap">
            {getHostUrl().replace(/https*:\/\//, '') + '/organizations/'}
          </span>
          <InputField
            autoFocus={true}
            className={classNames('input-super-large', {
              'is-invalid': isInvalid,
              'is-valid': isValid,
            })}
            id="organization-key"
            maxLength={50}
            onChange={this.handleChange}
            type="text"
            value={this.state.value}
          />
          <HelpTooltip
            className="sw-ml-2"
            overlay={<div className="sw-py-4">{translate('organization.key.description')}</div>}
          >
            <ButtonIcon
              Icon={IconQuestionMark}
              ariaLabel={translate('help')}
              isIconFilled
              variety={ButtonVariety.DefaultGhost}
            />
          </HelpTooltip>
        </div>
      </FormField>
    );
  }
}
