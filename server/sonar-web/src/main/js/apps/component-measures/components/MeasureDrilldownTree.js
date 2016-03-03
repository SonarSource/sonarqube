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
  state = {
    components: [],
    breadcrumbs: [],
    selected: null,
    fetching: true
  };

  componentDidMount () {
    this.mounted = true;
    this.fetchComponents(this.context.component);
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
    const { metric } = this.props;
    const asc = metric.direction === 1;

    const options = {
      s: 'metric,name',
      metricSort: metric.key,
      asc
    };

    this.setState({ fetching: true });

    getChildren(baseComponent.key, [metric.key], options).then(children => {
      if (this.mounted) {
        const components = enhanceWithSingleMeasure(children);

        const indexInBreadcrumbs = this.state.breadcrumbs
            .findIndex(component => component === baseComponent);

        const breadcrumbs = indexInBreadcrumbs !== -1 ?
            this.state.breadcrumbs.slice(0, indexInBreadcrumbs + 1) :
            [...this.state.breadcrumbs, baseComponent];

        this.setState({
          baseComponent,
          breadcrumbs,
          components,
          selected: null,
          fetching: false
        });
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
    this.setState({ selected });
  }

  render () {
    const { metric } = this.props;
    const { components, selected, breadcrumbs, fetching } = this.state;
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
