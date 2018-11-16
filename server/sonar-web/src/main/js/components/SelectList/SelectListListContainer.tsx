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
import { Filter } from './SelectList';
import SelectListListElement from './SelectListListElement';
import Checkbox from '../controls/Checkbox';
import DeferredSpinner from '../common/DeferredSpinner';
import { translate } from '../../helpers/l10n';

interface Props {
  allowBulkSelection?: boolean;
  elements: string[];
  disabledElements: string[];
  filter: Filter;
  onSelect: (element: string) => Promise<void>;
  onUnselect: (element: string) => Promise<void>;
  readOnly?: boolean;
  renderElement: (element: string) => React.ReactNode;
  selectedElements: string[];
}

interface State {
  loading: boolean;
}

export default class SelectListListContainer extends React.PureComponent<Props, State> {
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

  isDisabled = (element: string): boolean => {
    return this.props.readOnly || this.props.disabledElements.includes(element);
  };

  isSelected = (element: string): boolean => {
    return this.props.selectedElements.includes(element);
  };

  handleBulkChange = (checked: boolean) => {
    this.setState({ loading: true });
    if (checked) {
      Promise.all(this.props.elements.map(element => this.props.onSelect(element)))
        .then(this.stopLoading)
        .catch(this.stopLoading);
    } else {
      Promise.all(this.props.selectedElements.map(element => this.props.onUnselect(element)))
        .then(this.stopLoading)
        .catch(this.stopLoading);
    }
  };

  renderBulkSelector() {
    const { elements, readOnly, selectedElements } = this.props;
    return (
      <>
        <li>
          <Checkbox
            checked={selectedElements.length > 0}
            disabled={this.state.loading || readOnly}
            onCheck={this.handleBulkChange}
            thirdState={selectedElements.length > 0 && elements.length !== selectedElements.length}>
            <span className="big-spacer-left">
              {translate('bulk_change')}
              <DeferredSpinner className="spacer-left" loading={this.state.loading} timeout={10} />
            </span>
          </Checkbox>
        </li>
        <li className="divider" />
      </>
    );
  }

  render() {
    const { allowBulkSelection, elements, filter } = this.props;
    const filteredElements = elements.filter(element => {
      if (filter === Filter.All) {
        return true;
      }
      const isSelected = this.isSelected(element);
      return filter === Filter.Selected ? isSelected : !isSelected;
    });

    return (
      <div className={classNames('select-list-list-container spacer-top')}>
        <ul className="menu">
          {allowBulkSelection &&
            elements.length > 0 &&
            filter === Filter.All &&
            this.renderBulkSelector()}
          {filteredElements.map(element => (
            <SelectListListElement
              disabled={this.isDisabled(element)}
              element={element}
              key={element}
              onSelect={this.props.onSelect}
              onUnselect={this.props.onUnselect}
              renderElement={this.props.renderElement}
              selected={this.isSelected(element)}
            />
          ))}
        </ul>
      </div>
    );
  }
}
