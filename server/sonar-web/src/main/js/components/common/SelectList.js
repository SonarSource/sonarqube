/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import SelectListItem from './SelectListItem';

type Props = {
  children?: SelectListItem,
  items: Array<string>,
  currentItem: string,
  onSelect: (string) => void
};

type State = {
  active: string
};

export default class SelectList extends React.PureComponent {
  list: HTMLElement;
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      active: props.currentItem
    };
  }

  componentDidMount() {
    this.list.focus();
  }

  componentWillReceiveProps(nextProps: Props) {
    if (
      nextProps.currentItem !== this.props.currentItem &&
      !nextProps.items.includes(this.state.active)
    ) {
      this.setState({ active: nextProps.currentItem });
    }
  }

  handleKeyboard = (evt: KeyboardEvent) => {
    switch (evt.keyCode) {
      case 40: // down
        this.setState(this.selectNextElement);
        break;
      case 38: // up
        this.setState(this.selectPreviousElement);
        break;
      case 13: // return
        if (this.state.active) {
          this.handleSelect(this.state.active);
        }
        break;
      default:
        return;
    }
    evt.preventDefault();
    evt.stopPropagation();
  };

  handleSelect = (item: string) => {
    this.props.onSelect(item);
  };

  handleHover = (item: string) => {
    this.setState({ active: item });
  };

  selectNextElement = (state: State, props: Props) => {
    const idx = props.items.indexOf(state.active);
    if (idx < 0) {
      return { active: props.items[0] };
    }
    return { active: props.items[(idx + 1) % props.items.length] };
  };

  selectPreviousElement = (state: State, props: Props) => {
    const idx = props.items.indexOf(state.active);
    if (idx <= 0) {
      return { active: props.items[props.items.length - 1] };
    }
    return { active: props.items[idx - 1] };
  };

  render() {
    const { children } = this.props;
    const hasChildren = React.Children.count(children) > 0;
    return (
      <ul
        className="menu"
        onKeyDown={this.handleKeyboard}
        ref={list => this.list = list}
        tabIndex={0}>
        {hasChildren &&
          React.Children.map(children, child =>
            React.cloneElement(child, {
              active: this.state.active,
              onHover: this.handleHover,
              onSelect: this.handleSelect
            }))}
        {!hasChildren &&
          this.props.items.map(item => (
            <SelectListItem
              active={this.state.active}
              item={item}
              key={item}
              onHover={this.handleHover}
              onSelect={this.handleSelect}
            />
          ))}
      </ul>
    );
  }
}
