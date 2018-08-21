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
import ProjectOverviewFacet from './ProjectOverviewFacet';
import DomainFacet from './DomainFacet';
import { getDefaultView, groupByDomains, KNOWN_DOMAINS, PROJECT_OVERVEW, Query } from '../utils';
import { MeasureEnhanced } from '../../../app/types';

interface Props {
  hasOverview: boolean;
  measures: MeasureEnhanced[];
  selectedMetric: string;
  updateQuery: (query: Partial<Query>) => void;
}

interface State {
  openFacets: { [metric: string]: boolean };
}

export default class Sidebar extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { openFacets: this.getOpenFacets({}, props) };
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.selectedMetric !== this.props.selectedMetric) {
      this.setState(({ openFacets }) => ({
        openFacets: this.getOpenFacets(openFacets, nextProps)
      }));
    }
  }

  getOpenFacets = (
    openFacets: { [metric: string]: boolean },
    { measures, selectedMetric }: Props
  ) => {
    const newOpenFacets = { ...openFacets };
    const measure = measures.find(measure => measure.metric.key === selectedMetric);
    if (measure && measure.metric && measure.metric.domain) {
      newOpenFacets[measure.metric.domain] = true;
    } else if (KNOWN_DOMAINS.includes(selectedMetric)) {
      newOpenFacets[selectedMetric] = true;
    }
    return newOpenFacets;
  };

  toggleFacet = (name: string) => {
    this.setState(({ openFacets }) => ({
      openFacets: { ...openFacets, [name]: !openFacets[name] }
    }));
  };

  resetSelection = (metric: string) => ({ selected: undefined, view: getDefaultView(metric) });

  changeMetric = (metric: string) =>
    this.props.updateQuery({ metric, ...this.resetSelection(metric) });

  render() {
    const { hasOverview } = this.props;
    return (
      <div>
        {hasOverview && (
          <ProjectOverviewFacet
            onChange={this.changeMetric}
            selected={this.props.selectedMetric}
            value={PROJECT_OVERVEW}
          />
        )}
        {groupByDomains(this.props.measures).map(domain => (
          <DomainFacet
            domain={domain}
            hasOverview={hasOverview}
            key={domain.name}
            onChange={this.changeMetric}
            onToggle={this.toggleFacet}
            open={this.state.openFacets[domain.name] === true}
            selected={this.props.selectedMetric}
          />
        ))}
      </div>
    );
  }
}
