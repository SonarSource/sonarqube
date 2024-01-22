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
import { debounce, DebouncedFunc } from 'lodash';
import * as React from 'react';
import { KeyboardKeys } from '../../helpers/keycodes';
import { translate, translateWithParameters } from '../../helpers/l10n';
import SearchIcon from '../icons/SearchIcon';
import Spinner from '../ui/Spinner';
import { ClearButton } from './buttons';
import './SearchBox.css';

interface Props {
  autoFocus?: boolean;
  className?: string;
  id?: string;
  innerRef?: (node: HTMLInputElement | null) => void;
  loading?: boolean;
  maxLength?: number;
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

const DEFAULT_MAX_LENGTH = 100;
const DEBOUNCE_DELAY = 250;

export default class SearchBox extends React.PureComponent<Props, State> {
  debouncedOnChange: DebouncedFunc<(query: string) => void>;
  input?: HTMLInputElement | null;

  constructor(props: Props) {
    super(props);
    this.state = { value: props.value ?? '' };
    this.debouncedOnChange = debounce(this.props.onChange, DEBOUNCE_DELAY);
  }

  componentDidUpdate(prevProps: Props) {
    if (
      // input is controlled
      this.props.value !== undefined &&
      // parent is aware of last change
      // can happen when previous value was less than min length
      this.state.value === prevProps.value &&
      this.state.value !== this.props.value
    ) {
      this.setState({ value: this.props.value });
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
    if (event.nativeEvent.key === KeyboardKeys.Escape) {
      event.preventDefault();
      this.handleResetClick();
    }
    if (this.props.onKeyDown) {
      this.props.onKeyDown(event);
    }
  };

  handleResetClick = () => {
    this.changeValue('', false);
    if (this.props.value === undefined || this.props.value === '') {
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
    const { loading, minLength, maxLength = DEFAULT_MAX_LENGTH } = this.props;
    const { value } = this.state;

    const inputClassName = classNames('search-box-input', {
      touched: value.length > 0 && (!minLength || minLength > value.length),
    });

    const tooShort = minLength !== undefined && value.length > 0 && value.length < minLength;

    return (
      <div
        className={classNames('search-box', this.props.className)}
        id={this.props.id}
        title={
          tooShort && minLength !== undefined
            ? translateWithParameters('select2.tooShort', minLength)
            : ''
        }
      >
        <input
          aria-label={this.props.placeholder}
          autoComplete="off"
          autoFocus={this.props.autoFocus}
          className={inputClassName}
          maxLength={maxLength}
          onChange={this.handleInputChange}
          onClick={this.props.onClick}
          onFocus={this.props.onFocus}
          onKeyDown={this.handleInputKeyDown}
          placeholder={this.props.placeholder}
          ref={this.ref}
          type="search"
          value={value}
        />

        <Spinner loading={loading !== undefined ? loading : false}>
          <SearchIcon className="search-box-magnifier" />
        </Spinner>

        {value && (
          <ClearButton
            aria-label={translate('clear')}
            className="button-tiny search-box-clear"
            iconProps={{ size: 12 }}
            onClick={this.handleResetClick}
          />
        )}

        <span aria-live="polite" className="search-box-note">
          {tooShort &&
            minLength !== undefined &&
            translateWithParameters('select2.tooShort', minLength)}
        </span>
      </div>
    );
  }
}
