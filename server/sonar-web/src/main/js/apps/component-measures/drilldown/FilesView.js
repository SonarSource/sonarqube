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
// @flow
import React from 'react';
import key from 'keymaster';
import { throttle } from 'lodash';
import ComponentsList from './ComponentsList';
import ListFooter from '../../../components/controls/ListFooter';
import { scrollToElement } from '../../../helpers/scrolling';
/*:: import type { ComponentEnhanced, Paging } from '../types'; */
/*:: import type { Metric } from '../../../store/metrics/actions'; */

/*:: type Props = {|
  branch?: string,
  components: Array<ComponentEnhanced>,
  fetchMore: () => void,
  handleSelect: string => void,
  handleOpen: string => void,
  metric: Metric,
  metrics: { [string]: Metric },
  paging: ?Paging,
  selectedKey: ?string,
  selectedIdx: ?number
|}; */

export default class ListView extends React.PureComponent {
  /*:: listContainer: HTMLElement; */
  /*:: props: Props; */

  constructor(props /*: Props */) {
    super(props);
    this.selectNext = throttle(this.selectNext, 100);
    this.selectPrevious = throttle(this.selectPrevious, 100);
  }

  componentDidMount() {
    this.attachShortcuts();
    if (this.props.selectedKey != null) {
      this.scrollToElement();
    }
  }

  componentDidUpdate(prevProps /*: Props */) {
    if (this.props.selectedKey != null && prevProps.selectedKey !== this.props.selectedKey) {
      this.scrollToElement();
    }
  }

  componentWillUnmount() {
    this.detachShortcuts();
  }

  attachShortcuts() {
    key('up', 'measures-files', () => {
      this.selectPrevious();
      return false;
    });
    key('down', 'measures-files', () => {
      this.selectNext();
      return false;
    });
    key('right', 'measures-files', () => {
      this.openSelected();
      return false;
    });
  }

  detachShortcuts() {
    ['up', 'down', 'right'].map(action => key.unbind(action, 'measures-files'));
  }

  openSelected = () => {
    if (this.props.selectedKey != null) {
      this.props.handleOpen(this.props.selectedKey);
    }
  };

  selectPrevious = () => {
    const { selectedIdx } = this.props;
    if (selectedIdx != null && selectedIdx > 0) {
      this.props.handleSelect(this.props.components[selectedIdx - 1].key);
    } else {
      this.props.handleSelect(this.props.components[this.props.components.length - 1].key);
    }
  };

  selectNext = () => {
    const { selectedIdx } = this.props;
    if (selectedIdx != null && selectedIdx < this.props.components.length - 1) {
      this.props.handleSelect(this.props.components[selectedIdx + 1].key);
    } else {
      this.props.handleSelect(this.props.components[0].key);
    }
  };

  scrollToElement = () => {
    if (this.listContainer) {
      const elem = this.listContainer.getElementsByClassName('selected')[0];
      if (elem) {
        scrollToElement(elem, { topOffset: 215, bottomOffset: 100 });
      }
    }
  };

  render() {
    return (
      <div ref={elem => (this.listContainer = elem)}>
        <ComponentsList
          branch={this.props.branch}
          components={this.props.components}
          metrics={this.props.metrics}
          metric={this.props.metric}
          onClick={this.props.handleOpen}
          selectedComponent={this.props.selectedKey}
        />
        {this.props.paging &&
          this.props.components.length > 0 && (
            <ListFooter
              count={this.props.components.length}
              total={this.props.paging.total}
              loadMore={this.props.fetchMore}
            />
          )}
      </div>
    );
  }
}
