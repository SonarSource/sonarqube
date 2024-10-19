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
import { difference } from 'lodash';
import React, { PureComponent } from 'react';
import { Key } from '../../helpers/keyboard';
import { ItemDivider, ItemHeader } from '../DropdownMenu';
import { InputSearch } from './InputSearch';
import { MultiSelectMenuOption } from './MultiSelectMenuOption';

interface Props {
  allowNewElements?: boolean;
  allowSearch?: boolean;
  allowSelection?: boolean;
  createElementLabel: string;
  elements: string[];
  elementsDisabled?: string[];
  footerNode?: React.ReactNode;
  headerNode?: React.ReactNode;
  inputId?: string;
  listSize: number;
  noResultsLabel: string;
  onSearch?: (query: string) => Promise<void>;
  onSelect: (item: string) => void;
  onUnselect: (item: string) => void;
  placeholder: string;
  renderTooltip?: (element: string, disabled: boolean) => React.ReactNode;
  searchInputAriaLabel: string;
  selectedElements: string[];
  selectedElementsDisabled?: string[];
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
  filterSelected: (query: string, selectedElements: string[]) => string[];
  renderAriaLabel: (element: string) => string;
  renderLabel: (element: string) => React.ReactNode;
  validateSearchInput: (value: string) => string;
}

type PropsWithDefault = Props & DefaultProps;

export class MultiSelectMenu extends PureComponent<Props, State> {
  container?: HTMLDivElement | null;
  searchInput?: HTMLInputElement | null;
  mounted = false;

  static readonly defaultProps: DefaultProps = {
    filterSelected: (query: string, selectedElements: string[]) =>
      selectedElements.filter((elem) => elem.includes(query)),
    renderAriaLabel: (element: string) => element,
    renderLabel: (element: string) => element,
    validateSearchInput: (value: string) => value,
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      activeIdx: 0,
      loading: true,
      query: '',
      selectedElements: [],
      unselectedElements: [],
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.onSearchQuery('');
    this.updateSelectedElements(this.props as PropsWithDefault);
    this.updateUnselectedElements();
    if (this.container) {
      this.container.addEventListener('keydown', this.handleKeyboard, true);
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (this.searchInput) {
      this.searchInput.focus();
    }

    if (
      prevProps.elements !== this.props.elements ||
      prevProps.selectedElements !== this.props.selectedElements
    ) {
      this.updateSelectedElements(this.props as PropsWithDefault);
      this.updateUnselectedElements();

      const totalElements = this.getAllElements(this.props, this.state).length;

      if (this.state.activeIdx >= totalElements) {
        this.setState({ activeIdx: totalElements - 1 });
      }
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

  handleKeyboard = (evt: KeyboardEvent) => {
    switch (evt.key) {
      case Key.ArrowDown:
        evt.stopPropagation();
        evt.preventDefault();
        this.setState(this.selectNextElement);
        break;
      case Key.ArrowUp:
        evt.stopPropagation();
        evt.preventDefault();
        this.setState(this.selectPreviousElement);
        break;
      case Key.ArrowLeft:
      case Key.ArrowRight:
        evt.stopPropagation();
        break;
      case Key.Enter: {
        const allElements = this.getAllElements(this.props, this.state);
        if (this.state.activeIdx >= 0 && this.state.activeIdx < allElements.length) {
          this.toggleSelect(allElements[this.state.activeIdx]);
        }
        break;
      }
    }
  };

  onSearchQuery = (query: string) => {
    if (this.props.onSearch) {
      this.setState({ activeIdx: 0, loading: true, query });
      this.props.onSearch(query).then(this.stopLoading, this.stopLoading);
    }
  };

  onSelectItem = (item: string) => {
    if (this.isNewElement(item, this.props)) {
      this.onSearchQuery('');
    }
    this.props.onSelect(item);
  };

  onUnselectItem = (item: string) => {
    this.props.onUnselect(item);
  };

  isNewElement = (elem: string, { selectedElements, elements }: Props) =>
    elem.length > 0 && !selectedElements.includes(elem) && !elements.includes(elem);

  updateSelectedElements = (props: PropsWithDefault) => {
    this.setState((state: State) => {
      if (state.query) {
        return {
          selectedElements: props.filterSelected(state.query, props.selectedElements),
        };
      }
      return { selectedElements: [...props.selectedElements] };
    });
  };

  updateUnselectedElements = () => {
    const { listSize } = this.props;
    this.setState((state: State) => {
      if (listSize === 0) {
        return { unselectedElements: difference(this.props.elements, this.props.selectedElements) };
      } else if (listSize < state.selectedElements.length) {
        return { unselectedElements: [] };
      }
      return {
        unselectedElements: difference(this.props.elements, this.props.selectedElements).slice(
          0,
          listSize - state.selectedElements.length,
        ),
      };
    });
  };

  getAllElements = (props: Props, state: State) => {
    const { allowNewElements = true } = props;
    if (allowNewElements && this.isNewElement(state.query, props)) {
      return [...state.selectedElements, ...state.unselectedElements, state.query];
    }
    return [...state.selectedElements, ...state.unselectedElements];
  };

  selectNextElement = (state: State, props: Props) => {
    const { activeIdx } = state;
    const allElements = this.getAllElements(props, state);
    if (activeIdx < 0 || activeIdx >= allElements.length - 1) {
      return { activeIdx: 0 };
    }
    return { activeIdx: activeIdx + 1 };
  };

  selectPreviousElement = (state: State, props: Props) => {
    const { activeIdx } = state;
    const allElements = this.getAllElements(props, state);
    if (activeIdx <= 0) {
      const lastIdx = allElements.length - 1;
      return { activeIdx: lastIdx };
    }
    return { activeIdx: activeIdx - 1 };
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  toggleSelect = (item: string) => {
    if (!this.props.selectedElements.includes(item)) {
      this.onSelectItem(item);
      this.setState(this.selectNextElement);
    } else {
      this.onUnselectItem(item);
    }
  };

  render() {
    const {
      allowSearch = true,
      allowSelection = true,
      allowNewElements = true,
      createElementLabel,
      selectedElementsDisabled = [],
      headerNode = '',
      footerNode = '',
      inputId,
      noResultsLabel,
      searchInputAriaLabel,
      elementsDisabled,
      renderTooltip,
    } = this.props;
    const { renderAriaLabel, renderLabel } = this.props as PropsWithDefault;

    const { query, activeIdx, selectedElements, unselectedElements } = this.state;
    const activeElement = this.getAllElements(this.props, this.state)[activeIdx];
    const showNewElement = allowNewElements && this.isNewElement(query, this.props);
    const isFixedHeight = this.props.listSize === 0;
    const hasFooter = Boolean(footerNode);

    return (
      <div ref={(div) => (this.container = div)}>
        {allowSearch && (
          <>
            <div className="sw-px-3">
              <InputSearch
                autoFocus
                className="sw-mt-1"
                id={inputId}
                loading={this.state.loading}
                onChange={this.handleSearchChange}
                placeholder={this.props.placeholder}
                searchInputAriaLabel={searchInputAriaLabel}
                size="full"
                value={query}
              />
            </div>
            <ItemHeader>{headerNode}</ItemHeader>
          </>
        )}
        <ul
          className={classNames({
            'sw-mt-2': allowSearch,
            'sw-max-h-abs-200 sw-overflow-y-auto': isFixedHeight,
          })}
        >
          {selectedElements.length > 0 &&
            selectedElements.map((element) => (
              <MultiSelectMenuOption
                active={activeElement === element}
                createElementLabel={createElementLabel}
                disabled={selectedElementsDisabled.includes(element)}
                element={element}
                key={element}
                onHover={this.handleElementHover}
                onSelectChange={this.handleSelectChange}
                renderAriaLabel={renderAriaLabel}
                renderLabel={renderLabel}
                renderTooltip={renderTooltip}
                selected
              />
            ))}
          {unselectedElements.length > 0 &&
            unselectedElements.map((element) => (
              <MultiSelectMenuOption
                active={activeElement === element}
                createElementLabel={createElementLabel}
                disabled={!allowSelection}
                element={element}
                key={element}
                onHover={this.handleElementHover}
                onSelectChange={this.handleSelectChange}
                renderAriaLabel={renderAriaLabel}
                renderLabel={renderLabel}
                renderTooltip={renderTooltip}
              />
            ))}
          {elementsDisabled?.map((element) => (
            <MultiSelectMenuOption
              active={activeElement === element}
              createElementLabel={createElementLabel}
              disabled
              element={element}
              key={element}
              onHover={this.handleElementHover}
              onSelectChange={this.handleSelectChange}
              renderAriaLabel={renderAriaLabel}
              renderLabel={renderLabel}
              renderTooltip={renderTooltip}
            />
          ))}
          {showNewElement && (
            <MultiSelectMenuOption
              active={activeElement === query}
              createElementLabel={createElementLabel}
              custom
              element={query}
              key={query}
              onHover={this.handleElementHover}
              onSelectChange={this.handleSelectChange}
            />
          )}
          {!showNewElement &&
            selectedElements.length < 1 &&
            unselectedElements.length < 1 &&
            (elementsDisabled ?? []).length < 1 && <li className="sw-ml-2">{noResultsLabel}</li>}
        </ul>
        {hasFooter && <ItemDivider className="sw-mt-2" />}
        <div className="sw-px-3">{footerNode}</div>
      </div>
    );
  }
}
