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
import * as key from 'keymaster';
import { throttle } from 'lodash';
import ComponentsList from './ComponentsList';
import ListFooter from '../../../components/controls/ListFooter';
import { Button } from '../../../components/ui/buttons';
import {
  ComponentMeasure,
  ComponentMeasureEnhanced,
  Metric,
  Paging,
  BranchLike
} from '../../../app/types';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { isPeriodBestValue, isDiffMetric, formatMeasure } from '../../../helpers/measures';
import { scrollToElement } from '../../../helpers/scrolling';
import { Alert } from '../../../components/ui/Alert';

interface Props {
  branchLike?: BranchLike;
  components: ComponentMeasureEnhanced[];
  fetchMore: () => void;
  handleSelect: (component: string) => void;
  handleOpen: (component: string) => void;
  loadingMore: boolean;
  metric: Metric;
  metrics: { [metric: string]: Metric };
  paging?: Paging;
  rootComponent: ComponentMeasure;
  selectedKey?: string;
  selectedIdx?: number;
}

interface State {
  showBestMeasures: boolean;
}

export default class ListView extends React.PureComponent<Props, State> {
  listContainer?: HTMLElement | null;

  constructor(props: Props) {
    super(props);
    this.state = { showBestMeasures: false };
    this.selectNext = throttle(this.selectNext, 100);
    this.selectPrevious = throttle(this.selectPrevious, 100);
  }

  componentDidMount() {
    this.attachShortcuts();
    if (this.props.selectedKey !== undefined) {
      this.scrollToElement();
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.selectedKey !== undefined && prevProps.selectedKey !== this.props.selectedKey) {
      this.scrollToElement();
    }
    if (prevProps.metric.key !== this.props.metric.key) {
      this.setState({ showBestMeasures: false });
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
    ['up', 'down', 'right'].forEach(action => key.unbind(action, 'measures-files'));
  }

  getVisibleComponents = (components: ComponentMeasureEnhanced[], showBestMeasures: boolean) => {
    if (showBestMeasures) {
      return components;
    }
    const filtered = components.filter(component => !this.hasBestValue(component));
    if (filtered.length === 0) {
      return components;
    }
    return filtered;
  };

  handleShowBestMeasures = () => {
    this.setState({ showBestMeasures: true });
  };

  hasBestValue = (component: ComponentMeasureEnhanced) => {
    const { metric } = this.props;
    const focusedMeasure = component.measures.find(measure => measure.metric.key === metric.key);
    if (focusedMeasure && isDiffMetric(metric.key)) {
      return isPeriodBestValue(focusedMeasure, 1);
    }
    return Boolean(focusedMeasure && focusedMeasure.bestValue);
  };

  openSelected = () => {
    if (this.props.selectedKey !== undefined) {
      this.props.handleOpen(this.props.selectedKey);
    }
  };

  selectPrevious = () => {
    const { components, selectedIdx } = this.props;
    const visibleComponents = this.getVisibleComponents(components, this.state.showBestMeasures);
    if (selectedIdx !== undefined && selectedIdx > 0) {
      this.props.handleSelect(visibleComponents[selectedIdx - 1].key);
    } else {
      this.props.handleSelect(visibleComponents[visibleComponents.length - 1].key);
    }
  };

  selectNext = () => {
    const { components, selectedIdx } = this.props;
    const visibleComponents = this.getVisibleComponents(components, this.state.showBestMeasures);
    if (selectedIdx !== undefined && selectedIdx < visibleComponents.length - 1) {
      this.props.handleSelect(visibleComponents[selectedIdx + 1].key);
    } else {
      this.props.handleSelect(visibleComponents[0].key);
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
    const { components } = this.props;
    const filteredComponents = this.getVisibleComponents(components, this.state.showBestMeasures);
    const hidingBestMeasures = filteredComponents.length < components.length;
    return (
      <div ref={elem => (this.listContainer = elem)}>
        <ComponentsList
          branchLike={this.props.branchLike}
          components={filteredComponents}
          metric={this.props.metric}
          metrics={this.props.metrics}
          onClick={this.props.handleOpen}
          rootComponent={this.props.rootComponent}
          selectedComponent={this.props.selectedKey}
        />
        {hidingBestMeasures && (
          <Alert className="spacer-top" variant="info">
            {translateWithParameters(
              'component_measures.hidden_best_score_metrics',
              components.length - filteredComponents.length,
              formatMeasure(this.props.metric.bestValue, this.props.metric.type)
            )}
            <Button className="button-link spacer-left" onClick={this.handleShowBestMeasures}>
              {translate('show_all')}
            </Button>
          </Alert>
        )}
        {!hidingBestMeasures &&
          this.props.paging &&
          this.props.components.length > 0 && (
            <ListFooter
              count={this.props.components.length}
              loadMore={this.props.fetchMore}
              loading={this.props.loadingMore}
              total={this.props.paging.total}
            />
          )}
      </div>
    );
  }
}
