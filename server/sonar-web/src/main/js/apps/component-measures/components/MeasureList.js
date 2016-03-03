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
import ComponentsList from './ComponentsList';
import SourceViewer from '../../code/components/SourceViewer';
import NoResults from './NoResults';
import { getSingleMeasureValue, getSingleLeakValue } from '../utils';
import { getFiles } from '../../../api/components';

export default class MeasurePlainList extends React.Component {
  state = {
    components: [],
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

    getFiles(baseComponent.key, [metric.key], options).then(children => {
      if (this.mounted) {
        const componentsWithMappedMeasure = children
            .map(component => {
              return {
                ...component,
                value: getSingleMeasureValue(component.measures),
                leak: getSingleLeakValue(component.measures)
              };
            })
            .filter(component => component.value != null || component.leak != null);

        this.setState({
          components: componentsWithMappedMeasure,
          selected: null,
          fetching: false
        });
      }
    });
  }

  handleFileClick (selected) {
    this.setState({ selected });
  }

  render () {
    const { metric } = this.props;
    const { components, selected, fetching } = this.state;

    if (fetching) {
      return <Spinner/>;
    }

    if (!components.length) {
      return <NoResults/>;
    }

    return (
        <div className="measure-details-plain-list">
          <div className="measure-details-components">
            <ComponentsList
                components={components}
                selected={selected}
                metric={metric}
                onClick={this.handleFileClick.bind(this)}/>
          </div>

          {selected && (
              <div className="measure-details-viewer">
                <SourceViewer component={selected}/>
              </div>
          )}
        </div>
    );
  }
}

MeasurePlainList.contextTypes = {
  component: React.PropTypes.object
};
