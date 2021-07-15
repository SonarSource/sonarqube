/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import CheckIcon from '../icons/CheckIcon';
import { Button } from './buttons';
import './Toggle.css';

interface Props {
  disabled?: boolean;
  name?: string;
  onChange?: (value: boolean) => void;
  value: boolean | string;
}

export default class Toggle extends React.PureComponent<Props> {
  getValue = () => {
    const { value } = this.props;
    return typeof value === 'string' ? value === 'true' : value;
  };

  handleClick = () => {
    if (this.props.onChange) {
      const value = this.getValue();
      this.props.onChange(!value);
    }
  };

  render() {
    const { disabled, name } = this.props;
    const value = this.getValue();
    const className = classNames('boolean-toggle', { 'boolean-toggle-on': value });

    return (
      <Button className={className} disabled={disabled} name={name} onClick={this.handleClick}>
        <div aria-label={translate(value ? 'on' : 'off')} className="boolean-toggle-handle">
          <CheckIcon size={12} />
        </div>
      </Button>
    );
  }
}
