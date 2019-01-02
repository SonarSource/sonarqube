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
import Checkbox from '../controls/Checkbox';

interface Props {
  active?: boolean;
  disabled?: boolean;
  element: string;
  onSelect: (element: string) => Promise<void>;
  onUnselect: (element: string) => Promise<void>;
  renderElement: (element: string) => React.ReactNode;
  selected: boolean;
}

interface State {
  loading: boolean;
}

export default class SelectListListElement extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  handleCheck = (checked: boolean) => {
    this.setState({ loading: true });
    const request = checked ? this.props.onSelect : this.props.onUnselect;
    request(this.props.element).then(this.stopLoading, this.stopLoading);
  };

  render() {
    return (
      <li className={classNames({ 'select-list-list-disabled': this.props.disabled })}>
        <Checkbox
          checked={this.props.selected}
          className={classNames('select-list-list-checkbox', { active: this.props.active })}
          disabled={this.props.disabled}
          loading={this.state.loading}
          onCheck={this.handleCheck}>
          <span className="little-spacer-left">{this.props.renderElement(this.props.element)}</span>
        </Checkbox>
      </li>
    );
  }
}
