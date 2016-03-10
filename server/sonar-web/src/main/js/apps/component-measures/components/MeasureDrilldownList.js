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
import { getFiles } from '../../../api/components';

export default class MeasureDrilldownList extends React.Component {
  componentDidMount () {
    this.mounted = true;
    if (this.props.store.list.fetching) {
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

    updateStore({
      list: {
        ...store.list,
        fetching: true
      }
    });

    getFiles(baseComponent.key, [metric.key], options).then(files => {
      if (this.mounted) {
        const components = enhanceWithSingleMeasure(files);

        updateStore({
          list: {
            ...store.list,
            components,
            selected: null,
            fetching: false
          }
        });
      }
    });
  }

  handleFileClick (selected) {
    const { store, updateStore } = this.props;
    updateStore({
      list: {
        ...store.list,
        selected
      }
    });
  }

  render () {
    const { metric, store, leakPeriod } = this.props;
    const { components, selected, fetching } = store.list;

    if (fetching) {
      return <Spinner/>;
    }

    const sourceViewerPeriod = metric.key.indexOf('new_') === 0 && !!leakPeriod ? leakPeriod : null;

    return (
        <div className="measure-details-plain-list">
          <MeasureDrilldownComponents
              components={components}
              selected={selected}
              metric={metric}
              onClick={this.handleFileClick.bind(this)}/>

          {selected && (
              <div className="measure-details-viewer">
                <SourceViewer component={selected} period={sourceViewerPeriod}/>
              </div>
          )}
        </div>
    );
  }
}

MeasureDrilldownList.contextTypes = {
  component: React.PropTypes.object
};
