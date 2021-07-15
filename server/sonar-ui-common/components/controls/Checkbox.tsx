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
import DeferredSpinner from '../ui/DeferredSpinner';
import './Checkbox.css';

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
  title?: string;
}

export default class Checkbox extends React.PureComponent<Props> {
  static defaultProps = {
    thirdState: false,
  };

  handleClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    if (!this.props.disabled) {
      this.props.onCheck(!this.props.checked, this.props.id);
    }
  };

  render() {
    const { checked, children, disabled, id, loading, right, thirdState, title } = this.props;
    const className = classNames('icon-checkbox', {
      'icon-checkbox-checked': checked,
      'icon-checkbox-single': thirdState,
      'icon-checkbox-disabled': disabled,
    });

    if (children) {
      return (
        <a
          aria-checked={checked}
          className={classNames('link-checkbox', this.props.className, {
            note: disabled,
            disabled,
          })}
          href="#"
          id={id}
          onClick={this.handleClick}
          role="checkbox"
          title={title}>
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
        aria-checked={checked}
        className={classNames(className, this.props.className)}
        href="#"
        id={id}
        onClick={this.handleClick}
        role="checkbox"
        title={title}
      />
    );
  }
}
