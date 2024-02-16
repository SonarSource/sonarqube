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

import { ButtonPrimary, FlagMessage, InputTextArea } from 'design-system';
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import { DefaultSpecializedInputProps, getPropertyName } from '../../utils';

const JSON_SPACE_SIZE = 4;

interface State {
  formatError: boolean;
}

export default class InputForJSON extends React.PureComponent<DefaultSpecializedInputProps, State> {
  state: State = { formatError: false };

  handleInputChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({ formatError: false });
    this.props.onChange(event.target.value);
  };

  format = () => {
    const { value } = this.props;
    try {
      if (value) {
        this.props.onChange(JSON.stringify(JSON.parse(value), undefined, JSON_SPACE_SIZE));
      }
    } catch (e) {
      this.setState({ formatError: true });
    }
  };

  render() {
    const { value, name, setting, isInvalid } = this.props;
    const { formatError } = this.state;

    return (
      <>
        <div className="sw-flex sw-items-end">
          <InputTextArea
            size="large"
            name={name}
            onChange={this.handleInputChange}
            rows={5}
            value={value || ''}
            aria-label={getPropertyName(setting.definition)}
            isInvalid={isInvalid}
          />
          <div className="sw-ml-2">
            <ButtonPrimary className="sw-mt-2" onClick={this.format}>
              {translate('settings.json.format')}
            </ButtonPrimary>
          </div>
        </div>
        {formatError && (
          <FlagMessage className="sw-mt-2" variant="warning">
            {translate('settings.json.format_error')}{' '}
          </FlagMessage>
        )}
      </>
    );
  }
}
