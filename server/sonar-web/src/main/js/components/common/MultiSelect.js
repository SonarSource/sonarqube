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
import difference from 'lodash/difference';
import MultiSelectOption from './MultiSelectOption';
import { translate } from '../../helpers/l10n';

type Props = {
  selectedElements: Array<string>,
  elements: Array<string>,
  onSearch: (string) => void,
  onSelect: (string) => void,
  onUnselect: (string) => void
};

type State = {
  query: string,
  unselectedElements: Array<string>,
  active: { idx: number, value: string }
};

export default class MultiSelect extends React.PureComponent {
  container: HTMLElement;
  props: Props;
  state: State = {
    query: '',
    unselectedElements: [],
    active: { idx: -1, value: '' }
  };

  componentDidMount() {
    this.container.addEventListener('keydown', this.handleKeyboard);
  }

  componentWillUnmount() {
    this.container.removeEventListener('keydown', this.handleKeyboard);
  }

  componentWillReceiveProps(nextProps: Props) {
    const { active } = this.state;
    if (
      this.props.elements !== nextProps.elements ||
      this.props.selectedElements !== nextProps.selectedElements
    ) {
      this.setState({
        unselectedElements: difference(nextProps.elements, nextProps.selectedElements)
      });
    }
    if (!active.value || this.isNewElement(active.value, nextProps)) {
      this.selectInitialElement(nextProps);
    }
  }

  handleSelectChange = (item: string, selected: boolean) => {
    if (selected) {
      this.props.onSelect(item);
    } else {
      this.props.onUnselect(item);
    }
  };

  handleSearchChange = ({ target }: { target: HTMLInputElement }) => {
    this.setState({ query: target.value });
    this.props.onSearch(target.value);
  };

  handleElementHover = (element: string) => {
    this.setState((prevState, props) => {
      return {
        active: {
          idx: this.getAllElements(props, prevState).indexOf(element),
          value: element
        }
      };
    });
  };

  handleKeyboard = (evt: KeyboardEvent) => {
    switch (evt.keyCode) {
      case 40: // down
        this.setState(this.selectNextElement);
        break;
      case 38: // up
        this.setState(this.selectPreviousElement);
        break;
      case 13: // return
        if (this.state.active.value) {
          this.toggleSelect(this.state.active.value, this.props);
        }
        break;
    }
  };

  isNewElement(elem: string, { selectedElements, elements }: Props) {
    return elem && selectedElements.indexOf(elem) === -1 && elements.indexOf(elem) === -1;
  }

  getAllElements(props: Props, state: State) {
    if (this.isNewElement(state.query, props)) {
      return [...props.selectedElements, ...state.unselectedElements, state.query];
    } else {
      return [...props.selectedElements, ...state.unselectedElements];
    }
  }

  selectInitialElement(props: Props) {
    this.setState({
      active: {
        idx: 0,
        value: this.getAllElements(props, this.state)[0]
      }
    });
  }

  selectNextElement = (state: State, props: Props) => {
    const { active } = state;
    const allElements = this.getAllElements(props, state);
    if (active.idx < 0 || active.idx >= allElements.length - 1) {
      return { active: { idx: 0, value: allElements[0] } };
    } else {
      return {
        active: { idx: active.idx + 1, value: allElements[active.idx + 1] }
      };
    }
  };

  selectPreviousElement = (state: State, props: Props) => {
    const { active } = state;
    const allElements = this.getAllElements(props, state);
    if (active.idx <= 0) {
      const lastIdx = allElements.length - 1;
      return { active: { idx: lastIdx, value: allElements[lastIdx] } };
    } else {
      return {
        active: { idx: active.idx - 1, value: allElements[active.idx - 1] }
      };
    }
  };

  toggleSelect(item: string, props: Props) {
    if (props.selectedElements.indexOf(item) === -1) {
      props.onSelect(item);
    } else {
      props.onUnselect(item);
    }
  }

  render() {
    const { selectedElements } = this.props;
    const { query, active, unselectedElements } = this.state;

    return (
      <div className="multi-select" ref={div => this.container = div}>
        <div className="search-box menu-search">
          <button className="search-box-submit button-clean">
            <i className="icon-search-new" />
          </button>
          <input
            type="search"
            value={query}
            className="search-box-input"
            placeholder={translate('search_verb')}
            onChange={this.handleSearchChange}
            autoComplete="off"
          />
        </div>
        <ul className="menu">
          {selectedElements.length > 0 &&
            selectedElements.map(element => (
              <MultiSelectOption
                key={element}
                element={element}
                selected={true}
                active={active.value === element}
                onSelectChange={this.handleSelectChange}
                onHover={this.handleElementHover}
              />
            ))}
          {unselectedElements.length > 0 &&
            unselectedElements.map(element => (
              <MultiSelectOption
                key={element}
                element={element}
                active={active.value === element}
                onSelectChange={this.handleSelectChange}
                onHover={this.handleElementHover}
              />
            ))}
          {this.isNewElement(query, this.props) &&
            <MultiSelectOption
              key={query}
              element={query}
              custom={true}
              active={active.value === query}
              onSelectChange={this.handleSelectChange}
              onHover={this.handleElementHover}
            />}
        </ul>
      </div>
    );
  }
}
