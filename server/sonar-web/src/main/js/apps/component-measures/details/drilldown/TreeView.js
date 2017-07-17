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
import React from 'react';
import moment from 'moment';
import ComponentsList from './ComponentsList';
import ListHeader from './ListHeader';
import Spinner from '../../components/Spinner';
import SourceViewer from '../../../../components/SourceViewer/SourceViewer';
import ListFooter from '../../../../components/controls/ListFooter';

export default class TreeView extends React.PureComponent {
  componentDidMount() {
    this.handleChangeBaseComponent(this.props.component);
  }

  componentDidUpdate(nextProps) {
    if (nextProps.metric !== this.props.metric) {
      this.handleChangeBaseComponent(this.props.component);
    }

    if (this.props.selected) {
      this.scrollToViewer();
    } else if (this.scrollTop) {
      this.scrollToStoredPosition();
    }
  }

  scrollToViewer() {
    const { container } = this.refs;
    const top = container.getBoundingClientRect().top + window.scrollY - 95 - 10;

    // scroll only to top
    if (window.scrollY > top) {
      window.scrollTo(0, top);
    }
  }

  scrollToStoredPosition() {
    window.scrollTo(0, this.scrollTop);
    this.scrollTop = null;
  }

  storeScrollPosition() {
    this.scrollTop = window.scrollY;
  }

  handleChangeBaseComponent(baseComponent) {
    const { metric, onStart } = this.props;
    const periodIndex = this.props.location.query.period || 1;
    onStart(baseComponent, metric, Number(periodIndex));
  }

  handleFetchMore() {
    this.props.onFetchMore();
  }

  changeSelected(selected) {
    this.props.onSelect(selected);
  }

  canDrilldown(component) {
    return !['FIL', 'UTS'].includes(component.qualifier);
  }

  handleClick(selected) {
    if (this.canDrilldown(selected)) {
      this.props.onDrilldown(selected);
    } else {
      this.storeScrollPosition();
      this.props.onSelect(selected);
    }
  }

  handleBreadcrumbClick(component) {
    this.props.onUseBreadcrumbs(component, this.props.metric);
  }

  render() {
    const {
      components,
      metrics,
      breadcrumbs,
      metric,
      leakPeriod,
      selected,
      total,
      fetching
    } = this.props;
    const { onSelectNext, onSelectPrevious } = this.props;

    const selectedIndex = components.indexOf(selected);
    const sourceViewerPeriod = metric.key.indexOf('new_') === 0 && !!leakPeriod ? leakPeriod : null;
    const sourceViewerPeriodDate =
      sourceViewerPeriod != null ? moment(sourceViewerPeriod.date).toDate() : null;

    const filterLine =
      sourceViewerPeriodDate != null
        ? line => {
            if (line.scmDate) {
              const scmDate = moment(line.scmDate).toDate();
              return scmDate >= sourceViewerPeriodDate;
            } else {
              return false;
            }
          }
        : undefined;

    return (
      <div ref="container" className="measure-details-plain-list">
        <ListHeader
          metric={metric}
          breadcrumbs={breadcrumbs}
          componentsCount={components.length}
          selectedIndex={selectedIndex}
          onSelectPrevious={onSelectPrevious}
          onSelectNext={onSelectNext}
          onBrowse={this.handleBreadcrumbClick.bind(this)}
        />

        {!selected &&
          <div>
            {!fetching || components.length !== 0
              ? <div>
                  <ComponentsList
                    components={components}
                    metrics={metrics}
                    selected={selected}
                    metric={metric}
                    onClick={this.handleClick.bind(this)}
                  />
                  <ListFooter
                    count={components.length}
                    total={total}
                    loadMore={this.handleFetchMore.bind(this)}
                  />
                </div>
              : <Spinner />}
          </div>}

        {!!selected &&
          <div className="measure-details-viewer">
            <SourceViewer component={selected.key} filterLine={filterLine} />
          </div>}
      </div>
    );
  }
}
