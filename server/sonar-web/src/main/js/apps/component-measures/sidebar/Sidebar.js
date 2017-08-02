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
// @flow
import React from 'react';
import DomainFacet from './DomainFacet';
import { groupByDomains } from '../utils';
import type { MeasureEnhanced } from '../../../components/measure/types';
import type { Query } from '../types';

type Props = {|
  measures: Array<MeasureEnhanced>,
  selectedMetric: string,
  updateQuery: Query => void
|};

type State = {|
  closedFacets: { [string]: boolean }
|};

export default class Sidebar extends React.PureComponent {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = { closedFacets: {} };
  }

  toggleFacet = (name: string) => {
    this.setState(({ closedFacets }) => ({
      closedFacets: { ...closedFacets, [name]: !closedFacets[name] }
    }));
  };

  changeMetric = (metric: string) => this.props.updateQuery({ metric, selected: null });

  render() {
    return (
      <div className="search-navigator-facets-list">
        {groupByDomains(this.props.measures).map(domain =>
          <DomainFacet
            key={domain.name}
            domain={domain}
            onChange={this.changeMetric}
            onToggle={this.toggleFacet}
            open={!this.state.closedFacets[domain.name]}
            selected={this.props.selectedMetric}
          />
        )}
      </div>
    );
  }
}
