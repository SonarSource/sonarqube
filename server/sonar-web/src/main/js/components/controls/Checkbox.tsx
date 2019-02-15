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
import DeferredSpinner from '../common/DeferredSpinner';

interface Props {
  checked: boolean;
  disabled?: boolean;
  children?: React.ReactNode;
  className?: string;
  id?: string;
  loading?: boolean;
  onCheck: (checked: boolean, id?: string) => void;
  right?: boolean;
  thirdState?: boolean;
}

export default class Checkbox extends React.PureComponent<Props> {
  static defaultProps = {
    thirdState: false
  };

  handleClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    if (!this.props.disabled) {
      this.props.onCheck(!this.props.checked, this.props.id);
    }
  };

  render() {
    const { children, disabled, loading, right } = this.props;
    const className = classNames('icon-checkbox', {
      'icon-checkbox-checked': this.props.checked,
      'icon-checkbox-single': this.props.thirdState,
      'icon-checkbox-disabled': disabled
    });

    if (children) {
      return (
        <a
          className={classNames('link-checkbox', this.props.className, {
            note: disabled,
            disabled
          })}
          href="#"
          id={this.props.id}
          onClick={this.handleClick}>
          {right && children}
          <DeferredSpinner loading={Boolean(loading)}>
            <i className={className} />
          </DeferredSpinner>
          {!right && children}
        </a>
      );
    }

    if (loading) {
      return <DeferredSpinner />;
    }

    return (
      <a
        className={classNames(className, this.props.className)}
        href="#"
        id={this.props.id}
        onClick={this.handleClick}
      />
    );
  }
}
