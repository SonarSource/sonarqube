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
import './Tabs.css';

interface Props<T extends string> {
  onChange: (tab: T) => void;
  selected?: T;
  tabs: Array<{ disabled?: boolean; key: T; node: React.ReactNode }>;
}

export default function Tabs<T extends string>({ onChange, selected, tabs }: Props<T>) {
  return (
    <ul className="flex-tabs">
      {tabs.map(tab => (
        <Tab
          disabled={tab.disabled}
          key={tab.key}
          name={tab.key}
          onSelect={onChange}
          selected={selected === tab.key}>
          {tab.node}
        </Tab>
      ))}
    </ul>
  );
}

interface TabProps<T> {
  children: React.ReactNode;
  disabled?: boolean;
  name: T;
  onSelect: (tab: T) => void;
  selected: boolean;
}

export class Tab<T> extends React.PureComponent<TabProps<T>> {
  handleClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.stopPropagation();
    if (!this.props.disabled) {
      this.props.onSelect(this.props.name);
    }
  };

  render() {
    const { children, disabled, name, selected } = this.props;
    return (
      <li>
        <a
          className={classNames('js-' + name, { disabled, selected })}
          href="#"
          onClick={this.handleClick}>
          {children}
        </a>
      </li>
    );
  }
}
