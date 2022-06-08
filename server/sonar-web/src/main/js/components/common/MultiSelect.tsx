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
import { difference } from 'lodash';
import * as React from 'react';
import SearchBox from '../../components/controls/SearchBox';
import { isShortcut } from '../../helpers/keyboardEventHelpers';
import { KeyboardKeys } from '../../helpers/keycodes';
import { translateWithParameters } from '../../helpers/l10n';
import MultiSelectOption from './MultiSelectOption';

export interface MultiSelectProps {
  allowNewElements?: boolean;
  allowSelection?: boolean;
  elements: string[];
  // eslint-disable-next-line react/no-unused-prop-types
  filterSelected?: (query: string, selectedElements: string[]) => string[];
  footerNode?: React.ReactNode;
  listSize?: number;
  onSearch: (query: string) => Promise<void>;
  onSelect: (item: string) => void;
  onUnselect: (item: string) => void;
  placeholder: string;
  renderLabel?: (element: string) => React.ReactNode;
  selectedElements: string[];
  validateSearchInput?: (value: string) => string;
}

interface State {
  activeIdx: number;
  loading: boolean;
  query: string;
  selectedElements: string[];
  unselectedElements: string[];
}

interface DefaultProps {
  // eslint-disable-next-line react/no-unused-prop-types
  filterSelected: (query: string, selectedElements: string[]) => string[];
  listSize: number;
  renderLabel: (element: string) => React.ReactNode;
  validateSearchInput: (value: string) => string;
}

type PropsWithDefault = MultiSelectProps & DefaultProps;

export default class MultiSelect extends React.PureComponent<PropsWithDefault, State> {
  container?: HTMLDivElement | null;
  searchInput?: HTMLInputElement | null;
  mounted = false;

  static defaultProps: DefaultProps = {
    filterSelected: (query: string, selectedElements: string[]) =>
      selectedElements.filter(elem => elem.includes(query)),
    listSize: 0,
    renderLabel: (element: string) => element,
    validateSearchInput: (value: string) => value
  };

  constructor(props: PropsWithDefault) {
    super(props);
    this.state = {
      activeIdx: 0,
      loading: true,
      query: '',
      selectedElements: [],
      unselectedElements: []
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.onSearchQuery('');
    this.updateSelectedElements(this.props as PropsWithDefault);
    this.updateUnselectedElements(this.props as PropsWithDefault);
    if (this.container) {
      this.container.addEventListener('keydown', this.handleKeyboard, true);
    }
  }

  componentDidUpdate(prevProps: PropsWithDefault) {
    if (
      prevProps.elements !== this.props.elements ||
      prevProps.selectedElements !== this.props.selectedElements
    ) {
      this.updateSelectedElements(this.props);
      this.updateUnselectedElements(this.props);

      const totalElements = this.getAllElements(this.props, this.state).length;
      if (this.state.activeIdx >= totalElements) {
        this.setState({ activeIdx: totalElements - 1 });
      }
    }

    if (this.searchInput) {
      this.searchInput.focus();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    if (this.container) {
      this.container.removeEventListener('keydown', this.handleKeyboard);
    }
  }

  handleSelectChange = (selected: boolean, item: string) => {
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

  handleKeyboard = (event: KeyboardEvent) => {
    if (isShortcut(event)) {
      return true;
    }
    switch (event.key) {
      case KeyboardKeys.DownArrow:
        event.stopPropagation();
        event.preventDefault();
        this.setState(this.selectNextElement);
        break;
      case KeyboardKeys.UpArrow:
        event.stopPropagation();
        event.preventDefault();
        this.setState(this.selectPreviousElement);
        break;
      case KeyboardKeys.LeftArrow:
      case KeyboardKeys.RightArrow:
        event.stopPropagation();
        break;
      case KeyboardKeys.Enter:
        if (this.state.activeIdx >= 0) {
          this.toggleSelect(this.getAllElements(this.props, this.state)[this.state.activeIdx]);
        }
        break;
    }
  };

  onSearchQuery = (query: string) => {
    this.setState({ activeIdx: 0, loading: true, query });
    this.props.onSearch(query).then(this.stopLoading, this.stopLoading);
  };

  onSelectItem = (item: string) => {
    if (this.isNewElement(item, this.props)) {
      this.onSearchQuery('');
    }
    this.props.onSelect(item);
  };

  onUnselectItem = (item: string) => this.props.onUnselect(item);

  isNewElement = (elem: string, { selectedElements, elements }: PropsWithDefault) =>
    elem.length > 0 && selectedElements.indexOf(elem) === -1 && elements.indexOf(elem) === -1;

  updateSelectedElements = (props: PropsWithDefault) => {
    this.setState((state: State) => {
      if (state.query) {
        return {
          selectedElements: props.filterSelected(state.query, props.selectedElements)
        };
      } else {
        return { selectedElements: [...props.selectedElements] };
      }
    });
  };

  updateUnselectedElements = (props: PropsWithDefault) => {
    this.setState((state: State) => {
      if (props.listSize === 0) {
        return { unselectedElements: difference(props.elements, props.selectedElements) };
      } else if (props.listSize < state.selectedElements.length) {
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

  getAllElements = (props: PropsWithDefault, state: State) => {
    if (this.isNewElement(state.query, props)) {
      return [...state.selectedElements, ...state.unselectedElements, state.query];
    } else {
      return [...state.selectedElements, ...state.unselectedElements];
    }
  };

  setElementActive = (idx: number) => this.setState({ activeIdx: idx });

  selectNextElement = (state: State, props: PropsWithDefault) => {
    const { activeIdx } = state;
    const allElements = this.getAllElements(props, state);
    if (activeIdx < 0 || activeIdx >= allElements.length - 1) {
      return { activeIdx: 0 };
    } else {
      return { activeIdx: activeIdx + 1 };
    }
  };

  selectPreviousElement = (state: State, props: PropsWithDefault) => {
    const { activeIdx } = state;
    const allElements = this.getAllElements(props, state);
    if (activeIdx <= 0) {
      const lastIdx = allElements.length - 1;
      return { activeIdx: lastIdx };
    } else {
      return { activeIdx: activeIdx - 1 };
    }
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
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
    const { allowSelection = true, allowNewElements = true, footerNode = '' } = this.props;
    const { renderLabel } = this.props as PropsWithDefault;
    const { query, activeIdx, selectedElements, unselectedElements } = this.state;
    const activeElement = this.getAllElements(this.props, this.state)[activeIdx];
    const showNewElement = allowNewElements && this.isNewElement(query, this.props);
    const infiniteList = this.props.listSize === 0;
    const listClasses = classNames('menu', {
      'menu-vertically-limited': infiniteList,
      'spacer-top': infiniteList,
      'with-top-separator': infiniteList,
      'with-bottom-separator': Boolean(footerNode)
    });

    return (
      <div className="multi-select" ref={div => (this.container = div)}>
        <div className="menu-search">
          <SearchBox
            autoFocus={true}
            className="little-spacer-top"
            loading={this.state.loading}
            onChange={this.handleSearchChange}
            placeholder={this.props.placeholder}
            value={query}
          />
        </div>
        <ul className={listClasses}>
          {selectedElements.length > 0 &&
            selectedElements.map(element => (
              <MultiSelectOption
                active={activeElement === element}
                element={element}
                key={element}
                onHover={this.handleElementHover}
                onSelectChange={this.handleSelectChange}
                renderLabel={renderLabel}
                selected={true}
              />
            ))}
          {unselectedElements.length > 0 &&
            unselectedElements.map(element => (
              <MultiSelectOption
                active={activeElement === element}
                disabled={!allowSelection}
                element={element}
                key={element}
                onHover={this.handleElementHover}
                onSelectChange={this.handleSelectChange}
                renderLabel={renderLabel}
              />
            ))}
          {showNewElement && (
            <MultiSelectOption
              active={activeElement === query}
              custom={true}
              element={query}
              key={query}
              onHover={this.handleElementHover}
              onSelectChange={this.handleSelectChange}
              renderLabel={renderLabel}
            />
          )}
          {!showNewElement && selectedElements.length < 1 && unselectedElements.length < 1 && (
            <li className="spacer-left">{translateWithParameters('no_results_for_x', query)}</li>
          )}
        </ul>
        {footerNode}
      </div>
    );
  }
}
