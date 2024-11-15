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
import { IconQuestionMark } from '@sonarsource/echoes-react';
import { FormField, InputTextArea } from 'design-system';
import { debounce } from 'lodash';
import * as React from 'react';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { translate } from '../../../helpers/l10n';

interface Props {
  initialValue?: string;
  showHelpIcon: boolean;
  onChange: (value: string | undefined) => void;
}

interface State {
  error?: string;
  touched: boolean;
  validating: boolean;
  value: string;
}

export default class OrganizationDescriptionInput extends React.PureComponent<Props, State> {
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
      this.validateDescription(this.props.initialValue);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  checkFreeKey = (key: string) => {
    this.setState({ validating: false });
    this.setState({ error: undefined });
    this.props.onChange(key);
  };

  handleChange = (event: React.ChangeEvent<any>) => {
    const { value } = event.currentTarget;
    this.setState({ touched: true, value });
    this.validateDescription(value);
  };

  validateDescription(desc: string) {
    if (desc.length == 0) {
      this.checkFreeKey('');
    } else if (desc.length > 256 || !/^[A-Za-z0-9][A-Za-z0-9- ]*[A-Za-z0-9]?$/.test(desc)) {
      this.setState({
        error: translate('onboarding.create_organization.organization_name.error'),
        touched: true,
      });
      this.props.onChange(undefined);
    } else {
      this.checkFreeKey(desc);
    }
  }

  render() {
    const isInvalid = this.state.touched && this.state.error !== undefined;
    const isValid = this.state.touched && !this.state.validating && this.state.error === undefined;
    const { showHelpIcon } = this.props;
    return (
      <FormField
        error={this.state.error}
        isInvalid={isInvalid}
        isValid={isValid}
        label={translate('description')}
        description={translate('organization.description.description')}
      >
        <div className="display-inline-flex-center">
          <InputTextArea
            id="organization-description"
            maxLength={256}
            onChange={this.handleChange}
            value={this.state.value}
            rows={3}
          />
          {showHelpIcon && (
            <HelpTooltip
              overlay={<div className="sw-py-4">{translate('organization.name.description')}</div>}
            >
              <IconQuestionMark />
            </HelpTooltip>
          )}
        </div>
      </FormField>
    );
  }
}
