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

import Spinner from './Spinner';
import MeasureDrilldownComponents from './MeasureDrilldownComponents';
import SourceViewer from '../../code/components/SourceViewer';

import { enhanceWithSingleMeasure } from '../utils';
import { getChildren } from '../../../api/components';

export default class MeasureDrilldownTree extends React.Component {
  componentDidMount () {
    this.mounted = true;
    if (this.props.store.tree.fetching) {
      this.fetchComponents(this.context.component);
    }
  }

  componentDidUpdate (nextProps, nextState, nextContext) {
    if ((nextProps.metric !== this.props.metric) ||
        (nextContext.component !== this.context.component)) {
      this.fetchComponents(nextContext.component);
    }
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  fetchComponents (baseComponent) {
    const { metric, store, updateStore } = this.props;
    const asc = metric.direction === 1;

    const options = {
      s: 'metric,name',
      metricSort: metric.key,
      asc
    };

    const componentKey = baseComponent.refKey || baseComponent.key;

    updateStore({ tree: { ...store.tree, fetching: true } });

    getChildren(componentKey, [metric.key], options).then(children => {
      if (this.mounted) {
        const components = enhanceWithSingleMeasure(children);

        const indexInBreadcrumbs = store.tree.breadcrumbs
            .findIndex(component => component === baseComponent);

        const breadcrumbs = indexInBreadcrumbs !== -1 ?
            store.tree.breadcrumbs.slice(0, indexInBreadcrumbs + 1) :
            [...store.tree.breadcrumbs, baseComponent];

        const tree = {
          ...store.tree,
          baseComponent,
          breadcrumbs,
          components,
          selected: null,
          fetching: false
        };

        updateStore({ tree });
      }
    });
  }

  handleFileClick (component) {
    if (component.qualifier === 'FIL' || component.qualifier === 'UTS') {
      this.handleFileOpen(component);
    } else {
      this.fetchComponents(component);
    }
  }

  handleFileOpen (selected) {
    this.props.updateStore({ tree: { ...this.props.store.tree, selected } });
  }

  render () {
    const { metric, store } = this.props;
    const { components, selected, breadcrumbs, fetching } = store.tree;
    const parent = breadcrumbs.length > 1 ? breadcrumbs[breadcrumbs.length - 2] : null;

    if (fetching) {
      return <Spinner/>;
    }

    return (
        <div className="measure-details-tree">
          <MeasureDrilldownComponents
              components={components}
              selected={selected}
              parent={parent}
              metric={metric}
              onClick={this.handleFileClick.bind(this)}/>

          {selected && (
              <div className="measure-details-viewer">
                <SourceViewer component={selected}/>
              </div>
          )}
        </div>
    );
  }
}

MeasureDrilldownTree.contextTypes = {
  component: React.PropTypes.object
};
