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
import { difference } from 'lodash';
import MultiSelectOption from './MultiSelectOption';
import SearchBox from '../controls/SearchBox';

interface Props {
  selectedElements: Array<string>;
  elements: Array<string>;
  listSize?: number;
  onSearch: (query: string) => void;
  onSelect: (item: string) => void;
  onUnselect: (item: string) => void;
  validateSearchInput?: (value: string) => string;
  placeholder: string;
}

interface State {
  query: string;
  selectedElements: Array<string>;
  unselectedElements: Array<string>;
  activeIdx: number;
}

interface DefaultProps {
  listSize: number;
  validateSearchInput: (value: string) => string;
}

type PropsWithDefault = Props & DefaultProps;

export default class MultiSelect extends React.PureComponent<Props, State> {
  container?: HTMLDivElement | null;
  searchInput?: HTMLInputElement | null;

  static defaultProps: DefaultProps = {
    listSize: 10,
    validateSearchInput: (value: string) => value
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      query: '',
      selectedElements: [],
      unselectedElements: [],
      activeIdx: 0
    };
  }

  componentDidMount() {
    this.updateSelectedElements(this.props);
    this.updateUnselectedElements(this.props as PropsWithDefault);
    if (this.container) {
      this.container.addEventListener('keydown', this.handleKeyboard, true);
    }
  }

  componentWillReceiveProps(nextProps: PropsWithDefault) {
    if (
      this.props.elements !== nextProps.elements ||
      this.props.selectedElements !== nextProps.selectedElements
    ) {
      this.updateSelectedElements(nextProps);
      this.updateUnselectedElements(nextProps);

      const totalElements = this.getAllElements(nextProps, this.state).length;
      if (this.state.activeIdx >= totalElements) {
        this.setState({ activeIdx: totalElements - 1 });
      }
    }
  }

  componentDidUpdate() {
    if (this.searchInput) {
      this.searchInput.focus();
    }
  }

  componentWillUnmount() {
    if (this.container) {
      this.container.removeEventListener('keydown', this.handleKeyboard);
    }
  }

  handleSelectChange = (item: string, selected: boolean) => {
    if (selected) {
      this.onSelectItem(item);
    } else {
      this.onUnselectItem(item);
    }
  };

  handleSearchChange = (value: string) => {
    this.onSearchQuery((this.props as PropsWithDefault).validateSearchInput(value));
  };

  handleElementHover = (element: string) => {
    this.setState((prevState, props) => {
      return { activeIdx: this.getAllElements(props, prevState).indexOf(element) };
    });
  };

  handleKeyboard = (evt: KeyboardEvent) => {
    switch (evt.keyCode) {
      case 40: // down
        this.setState(this.selectNextElement);
        evt.stopPropagation();
        evt.preventDefault();
        break;
      case 38: // up
        this.setState(this.selectPreviousElement);
        evt.stopPropagation();
        evt.preventDefault();
        break;
      case 37: // left
      case 39: // right
        evt.stopPropagation();
        break;
      case 13: // enter
        if (this.state.activeIdx >= 0) {
          this.toggleSelect(this.getAllElements(this.props, this.state)[this.state.activeIdx]);
        }
        break;
    }
  };

  onSearchQuery = (query: string) => {
    this.setState({ query, activeIdx: 0 });
    this.props.onSearch(query);
  };

  onSelectItem = (item: string) => {
    if (this.isNewElement(item, this.props)) {
      this.onSearchQuery('');
    }
    this.props.onSelect(item);
  };

  onUnselectItem = (item: string) => this.props.onUnselect(item);

  isNewElement = (elem: string, { selectedElements, elements }: Props) =>
    elem && selectedElements.indexOf(elem) === -1 && elements.indexOf(elem) === -1;

  updateSelectedElements = (props: Props) => {
    this.setState((state: State) => {
      if (state.query) {
        return {
          selectedElements: [...props.selectedElements.filter(elem => elem.includes(state.query))]
        };
      } else {
        return { selectedElements: [...props.selectedElements] };
      }
    });
  };

  updateUnselectedElements = (props: PropsWithDefault) => {
    this.setState((state: State) => {
      if (props.listSize < state.selectedElements.length) {
        return { unselectedElements: [] };
      } else {
        return {
          unselectedElements: difference(props.elements, props.selectedElements).slice(
            0,
            props.listSize - state.selectedElements.length
          )
        };
      }
    });
  };

  getAllElements = (props: Props, state: State) => {
    if (this.isNewElement(state.query, props)) {
      return [...state.selectedElements, ...state.unselectedElements, state.query];
    } else {
      return [...state.selectedElements, ...state.unselectedElements];
    }
  };

  setElementActive = (idx: number) => this.setState({ activeIdx: idx });

  selectNextElement = (state: State, props: Props) => {
    const { activeIdx } = state;
    const allElements = this.getAllElements(props, state);
    if (activeIdx < 0 || activeIdx >= allElements.length - 1) {
      return { activeIdx: 0 };
    } else {
      return { activeIdx: activeIdx + 1 };
    }
  };

  selectPreviousElement = (state: State, props: Props) => {
    const { activeIdx } = state;
    const allElements = this.getAllElements(props, state);
    if (activeIdx <= 0) {
      const lastIdx = allElements.length - 1;
      return { activeIdx: lastIdx };
    } else {
      return { activeIdx: activeIdx - 1 };
    }
  };

  toggleSelect = (item: string) => {
    if (this.props.selectedElements.indexOf(item) === -1) {
      this.onSelectItem(item);
    } else {
      this.onUnselectItem(item);
    }
  };

  render() {
    const { query, activeIdx, selectedElements, unselectedElements } = this.state;
    const activeElement = this.getAllElements(this.props, this.state)[activeIdx];

    return (
      <div className="multi-select" ref={div => (this.container = div)}>
        <div className="menu-search">
          <SearchBox
            autoFocus={true}
            onChange={this.handleSearchChange}
            placeholder={this.props.placeholder}
            value={query}
          />
        </div>
        <ul className="menu">
          {selectedElements.length > 0 &&
            selectedElements.map(element => (
              <MultiSelectOption
                key={element}
                element={element}
                selected={true}
                active={activeElement === element}
                onSelectChange={this.handleSelectChange}
                onHover={this.handleElementHover}
              />
            ))}
          {unselectedElements.length > 0 &&
            unselectedElements.map(element => (
              <MultiSelectOption
                key={element}
                element={element}
                active={activeElement === element}
                onSelectChange={this.handleSelectChange}
                onHover={this.handleElementHover}
              />
            ))}
          {this.isNewElement(query, this.props) && (
            <MultiSelectOption
              key={query}
              element={query}
              custom={true}
              active={activeElement === query}
              onSelectChange={this.handleSelectChange}
              onHover={this.handleElementHover}
            />
          )}
        </ul>
      </div>
    );
  }
}
