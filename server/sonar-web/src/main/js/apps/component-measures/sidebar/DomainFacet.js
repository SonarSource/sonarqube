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
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import FacetMeasureValue from './FacetMeasureValue';
import IssueTypeIcon from '../../../components/ui/IssueTypeIcon';
import Tooltip from '../../../components/controls/Tooltip';
import { filterMeasures, sortMeasures } from '../utils';
import { getLocalizedMetricDomain, getLocalizedMetricName } from '../../../helpers/l10n';
import type { MeasureEnhanced } from '../../../components/measure/types';

type Props = {|
  onChange: (metric: string) => void,
  onToggle: (property: string) => void,
  open: boolean,
  domain: { name: string, measures: Array<MeasureEnhanced> },
  selected: string
|};

export default class DomainFacet extends React.PureComponent {
  props: Props;

  handleHeaderClick = () => this.props.onToggle(this.props.domain.name);

  render() {
    const { domain, selected } = this.props;
    const measures = sortMeasures(domain.name, filterMeasures(domain.measures));
    return (
      <FacetBox>
        <FacetHeader
          name={getLocalizedMetricDomain(domain.name)}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={measures.find(measure => measure.metric.key === selected) ? 1 : 0}
        />

        {this.props.open &&
          <FacetItemsList>
            {measures.map(measure =>
              <FacetItem
                active={measure.metric.key === selected}
                disabled={false}
                key={measure.metric.key}
                name={
                  <Tooltip overlay={getLocalizedMetricName(measure.metric)} mouseEnterDelay={1}>
                    <span id={`measure-${measure.metric.key}-name`}>
                      <IssueTypeIcon query={measure.metric.key} className="little-spacer-right" />
                      {getLocalizedMetricName(measure.metric)}
                    </span>
                  </Tooltip>
                }
                onClick={this.props.onChange}
                stat={<FacetMeasureValue measure={measure} />}
                value={measure.metric.key}
              />
            )}
          </FacetItemsList>}
      </FacetBox>
    );
  }
}
