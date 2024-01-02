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
import { isEmpty, remove, xor } from 'lodash';
import * as React from 'react';
import SearchBox from '../../components/controls/SearchBox';
import { translateWithParameters } from '../../helpers/l10n';
import MultiSelectOption, { Element } from './MultiSelectOption';

export interface MultiSelectProps {
  allowNewElements?: boolean;
  allowSelection?: boolean;
  legend: string;
  elements: string[];
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
  loading: boolean;
  query: string;
  elements: Element[];
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
  mounted = false;

  static defaultProps: DefaultProps = {
    filterSelected: (query: string, selectedElements: string[]) =>
      selectedElements.filter((elem) => elem.includes(query)),
    listSize: 0,
    renderLabel: (element: string) => element,
    validateSearchInput: (value: string) => value,
  };

  constructor(props: PropsWithDefault) {
    super(props);
    this.state = {
      loading: false,
      query: '',
      elements: [],
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.onSearchQuery('');
    this.computeElements();
  }

  componentDidUpdate(prevProps: PropsWithDefault, prevState: State) {
    if (
      !isEmpty(
        xor(
          [...prevProps.selectedElements, ...prevProps.elements],
          [...this.props.selectedElements, ...this.props.elements],
        ),
      )
    ) {
      this.computeElements();
    }

    if (prevState.query !== this.state.query) {
      this.setState(({ query, elements }, props) => {
        const newElements = [...elements];
        this.appendCreateElelement(newElements, query, props);
        return { elements: newElements };
      });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  computeElements() {
    this.setState(({ query }, props) => {
      const newStateElement: Element[] = [
        ...this.props
          .filterSelected(query, this.props.selectedElements)
          .map((e) => ({ value: e, selected: true })),
        ...this.props.elements.map((e) => ({
          value: e,
          selected: false,
        })),
      ];

      this.appendCreateElelement(newStateElement, query, props);
      return { elements: newStateElement };
    });
  }

  appendCreateElelement(elements: Element[], query: string, props: PropsWithDefault) {
    const { allowNewElements = true } = props;
    if (this.isNewElement(query, props) && allowNewElements) {
      const create = elements.find((e) => e.custom);
      if (create) {
        create.value = query;
      } else {
        elements.push({ value: query, selected: false, custom: true });
      }
    } else if (!this.isNewElement(query, props) && allowNewElements) {
      remove(elements, (e) => e.custom);
    }
  }

  handleSelectChange = (selected: boolean, item: string) => {
    this.setState(({ elements }) => {
      const newElements = elements.map((e) =>
        e.value === item ? { value: e.value, selected } : e,
      );
      return { elements: newElements };
    });
    if (selected) {
      this.onSelectItem(item);
    } else {
      this.onUnselectItem(item);
    }
  };

  handleSearchChange = (value: string) => {
    this.onSearchQuery((this.props as PropsWithDefault).validateSearchInput(value));
  };

  onSearchQuery = (query: string) => {
    const { allowNewElements = true } = this.props;

    this.props.onSearch(query).then(this.stopLoading, this.stopLoading);
    if (allowNewElements) {
      this.setState({
        loading: true,
        query,
      });
    }
  };

  onSelectItem = (item: string) => {
    if (this.isNewElement(item, this.props)) {
      this.onSearchQuery('');
    }
    this.props.onSelect(item);
  };

  onUnselectItem = (item: string) => this.props.onUnselect(item);

  isNewElement = (elem: string, { selectedElements, elements }: PropsWithDefault) =>
    !isEmpty(elem) && selectedElements.indexOf(elem) === -1 && elements.indexOf(elem) === -1;

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  render() {
    const { legend, allowSelection = true, footerNode = '' } = this.props;
    const { renderLabel } = this.props as PropsWithDefault;
    const { query, elements } = this.state;
    const infiniteList = this.props.listSize === 0;
    const listClasses = classNames('menu', {
      'menu-vertically-limited': infiniteList,
      'spacer-top': infiniteList,
      'with-top-separator': infiniteList,
      'with-bottom-separator': Boolean(footerNode),
    });

    return (
      <div className="multi-select">
        <div className="menu-search">
          <SearchBox
            autoFocus
            className="little-spacer-top"
            loading={this.state.loading}
            onChange={this.handleSearchChange}
            placeholder={this.props.placeholder}
            value={query}
          />
        </div>
        <fieldset aria-label={legend}>
          <ul className={listClasses}>
            {elements.map((e) => (
              <MultiSelectOption
                element={e}
                disabled={!allowSelection && !e.selected}
                key={e.value}
                onSelectChange={this.handleSelectChange}
                renderLabel={renderLabel}
              />
            ))}
            {isEmpty(elements) && (
              <li className="spacer-left">{translateWithParameters('no_results_for_x', query)}</li>
            )}
          </ul>
        </fieldset>
        {footerNode}
      </div>
    );
  }
}
