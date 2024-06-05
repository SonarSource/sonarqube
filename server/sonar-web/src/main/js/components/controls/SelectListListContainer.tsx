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
import styled from '@emotion/styled';
import { Checkbox, ListItem, UnorderedList, themeBorder } from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import { SelectListFilter } from './SelectList';
import SelectListListElement from './SelectListListElement';

interface Props {
  allowBulkSelection?: boolean;
  disabledElements: string[];
  elements: string[];
  filter: SelectListFilter;
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
      Promise.all(this.props.elements.map((element) => this.props.onSelect(element)))
        .then(this.stopLoading)
        .catch(this.stopLoading);
    } else {
      Promise.all(this.props.selectedElements.map((element) => this.props.onUnselect(element)))
        .then(this.stopLoading)
        .catch(this.stopLoading);
    }
  };

  renderBulkSelector() {
    const { elements, readOnly, selectedElements } = this.props;
    return (
      <BorderedListItem className="sw-pb-4">
        <Checkbox
          checked={selectedElements.length > 0}
          disabled={this.state.loading || readOnly}
          onCheck={this.handleBulkChange}
          thirdState={selectedElements.length > 0 && elements.length !== selectedElements.length}
          loading={this.state.loading}
        >
          <span className="sw-ml-4">{translate('bulk_change')}</span>
        </Checkbox>
      </BorderedListItem>
    );
  }

  render() {
    const { allowBulkSelection, elements, filter } = this.props;

    return (
      <ListContainer className="sw-mt-2 sw-p-3 sw-rounded-1 it__select-list-list-container">
        <UnorderedList className="-sw-mt-3">
          {allowBulkSelection &&
            elements.length > 0 &&
            filter === SelectListFilter.All &&
            this.renderBulkSelector()}
          {elements.map((element) => (
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
        </UnorderedList>
      </ListContainer>
    );
  }
}

const BorderedListItem = styled(ListItem)`
  border-bottom: ${themeBorder('default', 'discreetBorder')};
`;

const ListContainer = styled.div`
  overflow: auto;
  height: 330px;
  border: ${themeBorder('default')};
`;
