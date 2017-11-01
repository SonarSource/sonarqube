/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import SearchIcon from '../icons-components/SearchIcon';
import * as theme from '../../app/theme';
import { translateWithParameters } from '../../helpers/l10n';
import { ButtonIcon } from '../ui/buttons';
import { ClearIcon } from '../icons-components/icons';

interface Props {
  minLength?: number;
  onChange: (value: string) => void;
  onKeyDown?: (event: React.KeyboardEvent<HTMLInputElement>) => void;
  placeholder: string;
  value: string;
}

export default class SearchBox extends React.PureComponent<Props> {
  handleChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    const { value } = event.currentTarget;
    this.props.onChange(value);
  };

  handleResetClick = () => {
    this.props.onChange('');
  };

  render() {
    const { minLength, value } = this.props;

    const inputClassName = classNames('search-box-input', {
      touched: value.length > 0 && (!minLength || minLength > value.length)
    });

    const tooShort = minLength !== undefined && value.length > 0 && value.length < minLength;

    return (
      <div className="search-box">
        <input
          className={inputClassName}
          maxLength={100}
          onChange={this.handleChange}
          onKeyDown={this.props.onKeyDown}
          placeholder={this.props.placeholder}
          type="text"
          value={this.props.value}
        />

        <SearchIcon className="search-box-magnifier" />

        {value && (
          <ButtonIcon
            className="button-tiny search-box-clear"
            color={theme.gray60}
            onClick={this.handleResetClick}>
            <ClearIcon size={12} />
          </ButtonIcon>
        )}

        {tooShort && (
          <span className="search-box-note">
            {translateWithParameters('select2.tooShort', minLength!)}
          </span>
        )}
      </div>
    );
  }
}
