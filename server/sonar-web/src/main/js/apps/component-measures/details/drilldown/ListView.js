/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import classNames from 'classnames';

import ComponentsList from './ComponentsList';
import ListHeader from './ListHeader';
import Spinner from '../../components/Spinner';
import SourceViewer from '../../../code/components/SourceViewer';
import ListFooter from '../../../../components/shared/list-footer';

export default class ListView extends React.Component {
  componentDidMount () {
    this.handleChangeBaseComponent(this.props.component);
  }

  componentDidUpdate (nextProps) {
    if (nextProps.metric !== this.props.metric) {
      this.handleChangeBaseComponent(this.props.component);
    }

    if (this.props.selected) {
      this.scrollToViewer();
    } else if (this.scrollTop) {
      this.scrollToStoredPosition();
    }
  }

  fetchMore () {
    const { metric, component, onFetchMore } = this.props;
    onFetchMore(component, metric);
  }

  scrollToViewer () {
    const { container } = this.refs;
    const top = container.getBoundingClientRect().top + window.scrollY - 95 - 10;
    window.scrollTo(0, top);
  }

  scrollToStoredPosition () {
    window.scrollTo(0, this.scrollTop);
    this.scrollTop = null;
  }

  storeScrollPosition () {
    this.scrollTop = window.scrollY;
  }

  handleChangeBaseComponent (baseComponent) {
    const { metric, onFetchList } = this.props;
    onFetchList(baseComponent, metric);
  }

  changeSelected (selected) {
    this.props.onSelect(selected);
  }

  handleClick (selected) {
    this.storeScrollPosition();
    this.props.onSelect(selected);
  }

  handleBreadcrumbClick () {
    this.props.onSelect(undefined);
  }

  render () {
    const { component, components, metric, leakPeriod, selected, fetching, total } = this.props;
    const { onSelectNext, onSelectPrevious } = this.props;

    const breadcrumbs = [component];
    if (selected) {
      breadcrumbs.push(selected);
    }
    const selectedIndex = components.indexOf(selected);
    const sourceViewerPeriod = metric.key.indexOf('new_') === 0 && !!leakPeriod ? leakPeriod : null;

    return (
        <div ref="container" className="measure-details-plain-list">
          <ListHeader
              metric={metric}
              breadcrumbs={breadcrumbs}
              componentsCount={components.length}
              selectedIndex={selectedIndex}
              onSelectPrevious={onSelectPrevious}
              onSelectNext={onSelectNext}
              onBrowse={this.handleBreadcrumbClick.bind(this)}/>

          {!selected && (
              <div className={classNames({ 'new-loading': fetching })}>
                {(!fetching || components.length !== 0) ? (
                    <ComponentsList
                        components={components}
                        selected={selected}
                        metric={metric}
                        onClick={this.handleClick.bind(this)}/>
                ) : (
                    <Spinner/>
                )}
                <ListFooter
                    count={components.length}
                    total={total}
                    loadMore={this.fetchMore.bind(this)}
                    ready={!fetching}/>
              </div>
          )}

          {!!selected && (
              <div className="measure-details-viewer">
                <SourceViewer
                    component={selected}
                    period={sourceViewerPeriod}/>
              </div>
          )}
        </div>
    );
  }
}
