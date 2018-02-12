/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { debounce, Cancelable } from 'lodash';
import SearchIcon from '../icons-components/SearchIcon';
import ClearIcon from '../icons-components/ClearIcon';
import { ButtonIcon } from '../ui/buttons';
import * as theme from '../../app/theme';
import { translateWithParameters } from '../../helpers/l10n';
import './SearchBox.css';

interface Props {
  autoFocus?: boolean;
  className?: string;
  innerRef?: (node: HTMLInputElement | null) => void;
  id?: string;
  minLength?: number;
  onChange: (value: string) => void;
  onClick?: React.MouseEventHandler<HTMLInputElement>;
  onFocus?: React.FocusEventHandler<HTMLInputElement>;
  onKeyDown?: React.KeyboardEventHandler<HTMLInputElement>;
  placeholder: string;
  value?: string;
}

interface State {
  value: string;
}

export default class SearchBox extends React.PureComponent<Props, State> {
  debouncedOnChange: ((query: string) => void) & Cancelable;
  input?: HTMLInputElement | null;

  constructor(props: Props) {
    super(props);
    this.state = { value: props.value || '' };
    this.debouncedOnChange = debounce(this.props.onChange, 250);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (
      // input is controlled
      nextProps.value !== undefined &&
      // parent is aware of last change
      // can happen when previous value was less than min length
      this.state.value === this.props.value &&
      nextProps.value !== this.state.value
    ) {
      this.setState({ value: nextProps.value });
    }
  }

  changeValue = (value: string, debounced = true) => {
    const { minLength } = this.props;
    if (value.length === 0) {
      // immediately notify when value is empty
      this.props.onChange('');
      // and cancel scheduled callback
      this.debouncedOnChange.cancel();
    } else if (!minLength || minLength <= value.length) {
      if (debounced) {
        this.debouncedOnChange(value);
      } else {
        this.props.onChange(value);
      }
    }
  };

  handleInputChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    const { value } = event.currentTarget;
    this.setState({ value });
    this.changeValue(value);
  };

  handleInputKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.keyCode === 27) {
      // escape
      event.preventDefault();
      this.handleResetClick();
    }
    if (this.props.onKeyDown) {
      this.props.onKeyDown(event);
    }
  };

  handleResetClick = () => {
    this.changeValue('', false);
    if (this.props.value === undefined) {
      this.setState({ value: '' });
    }
    if (this.input) {
      this.input.focus();
    }
  };

  ref = (node: HTMLInputElement | null) => {
    this.input = node;
    if (this.props.innerRef) {
      this.props.innerRef(node);
    }
  };

  render() {
    const { minLength } = this.props;
    const { value } = this.state;

    const inputClassName = classNames('search-box-input', {
      touched: value.length > 0 && (!minLength || minLength > value.length)
    });

    const tooShort = minLength !== undefined && value.length > 0 && value.length < minLength;

    return (
      <div className={classNames('search-box', this.props.className)} id={this.props.id}>
        <input
          autoComplete="off"
          autoFocus={this.props.autoFocus}
          className={inputClassName}
          maxLength={100}
          onChange={this.handleInputChange}
          onClick={this.props.onClick}
          onFocus={this.props.onFocus}
          onKeyDown={this.handleInputKeyDown}
          placeholder={this.props.placeholder}
          ref={this.ref}
          type="search"
          value={value}
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
          <span
            className="search-box-note"
            title={translateWithParameters('select2.tooShort', minLength!)}>
            {translateWithParameters('select2.tooShort', minLength!)}
          </span>
        )}
      </div>
    );
  }
}
