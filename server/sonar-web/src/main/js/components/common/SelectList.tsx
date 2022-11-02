/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import * as React from 'react';
import { KeyboardKeys } from '../../helpers/keycodes';
import SelectListItem from './SelectListItem';

interface Props {
  className?: string;
  items: string[];
  currentItem: string;
  onSelect: (item: string) => void;
}

interface State {
  active: string;
  selected: string;
}

export default class SelectList extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      active: props.currentItem,
      selected: props.currentItem,
    };
  }

  componentDidMount() {
    document.addEventListener('keydown', this.handleKeyDown, { capture: true });
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.currentItem !== this.props.currentItem &&
      !this.props.items.includes(this.state.active)
    ) {
      this.setState({ active: this.props.currentItem, selected: this.props.currentItem });
    }
  }

  componentWillUnmount() {
    document.removeEventListener('keydown', this.handleKeyDown, { capture: true });
  }

  handleKeyDown = (event: KeyboardEvent) => {
    if (event.key === KeyboardKeys.DownArrow) {
      event.preventDefault();
      event.stopImmediatePropagation();
      this.setState(this.selectNextElement);
    } else if (event.key === KeyboardKeys.UpArrow) {
      event.preventDefault();
      event.stopImmediatePropagation();
      this.setState(this.selectPreviousElement);
    } else if (event.key === KeyboardKeys.Enter) {
      event.preventDefault();
      event.stopImmediatePropagation();
      if (this.state.selected != null) {
        this.handleSelect(this.state.selected);
      }
    }
  };

  handleSelect = (item: string) => {
    this.props.onSelect(item);
  };

  handleHover = (selected: string) => {
    this.setState({ selected });
  };

  selectNextElement = (state: State, props: Props) => {
    const idx = props.items.indexOf(state.selected);
    if (idx < 0) {
      return { selected: props.items[0] };
    }
    return { selected: props.items[(idx + 1) % props.items.length] };
  };

  selectPreviousElement = (state: State, props: Props) => {
    const idx = props.items.indexOf(state.selected);
    if (idx <= 0) {
      return { selected: props.items[props.items.length - 1] };
    }
    return { selected: props.items[idx - 1] };
  };

  renderChild = (child: any) => {
    if (child == null) {
      return null;
    }
    // do not pass extra props to children like `<li className="divider" />`
    if (child.type !== SelectListItem) {
      return child;
    }
    return React.cloneElement(child, {
      active: this.state.active,
      selected: this.state.selected,
      onHover: this.handleHover,
      onSelect: this.handleSelect,
    });
  };

  render() {
    const { children } = this.props;
    const hasChildren = React.Children.count(children) > 0;
    return (
      <ul className={classNames('menu', this.props.className)}>
        {hasChildren && React.Children.map(children, this.renderChild)}
        {!hasChildren &&
          this.props.items.map((item) => (
            <SelectListItem
              active={this.state.active}
              selected={this.state.selected}
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
