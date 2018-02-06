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
// @flow
import React from 'react';
import FacetMeasureValue from './FacetMeasureValue';
import BubblesIcon from '../../../components/icons-components/BubblesIcon';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import {
  addMeasureCategories,
  filterMeasures,
  hasBubbleChart,
  hasFacetStat,
  sortMeasures
} from '../utils';
import {
  getLocalizedCategoryMetricName,
  getLocalizedMetricDomain,
  getLocalizedMetricName,
  translate
} from '../../../helpers/l10n';
/*:: import type { MeasureEnhanced } from '../../../components/measure/types'; */

/*:: type Props = {|
  onChange: (metric: string) => void,
  onToggle: (property: string) => void,
  open: boolean,
  domain: { name: string, measures: Array<MeasureEnhanced> },
  selected: string
|}; */

export default class DomainFacet extends React.PureComponent {
  /*:: props: Props; */

  handleHeaderClick = () => this.props.onToggle(this.props.domain.name);

  hasFacetSelected = (
    domain /*: { name: string } */,
    measures /*: Array<MeasureEnhanced> */,
    selected /*: string */
  ) => {
    const measureSelected = measures.find(measure => measure.metric.key === selected);
    const overviewSelected = domain.name === selected && hasBubbleChart(domain.name);
    return measureSelected || overviewSelected;
  };

  getValues = () => {
    const { domain, selected } = this.props;
    const measureSelected = domain.measures.find(measure => measure.metric.key === selected);
    const overviewSelected = domain.name === selected && hasBubbleChart(domain.name);
    return measureSelected
      ? [getLocalizedMetricName(measureSelected.metric)]
      : overviewSelected ? [translate('component_measures.domain_overview')] : [];
  };

  renderItemFacetStat = (item /*: MeasureEnhanced */) =>
    hasFacetStat(item.metric.key) ? <FacetMeasureValue measure={item} /> : null;

  renderItemsFacet = () => {
    const { domain, selected } = this.props;
    const items = addMeasureCategories(domain.name, filterMeasures(domain.measures));
    const hasCategories = items.some(item => typeof item === 'string');
    const translateMetric = hasCategories ? getLocalizedCategoryMetricName : getLocalizedMetricName;
    const sortedItems = sortMeasures(domain.name, items);
    return sortedItems.map(
      item =>
        typeof item === 'string' ? (
          <span key={item} className="facet search-navigator-facet facet-category">
            <span className="facet-name">
              {translate('component_measures.facet_category', item)}
            </span>
          </span>
        ) : (
          <FacetItem
            active={item.metric.key === selected}
            disabled={false}
            key={item.metric.key}
            name={
              <span className="big-spacer-left" id={`measure-${item.metric.key}-name`}>
                {translateMetric(item.metric)}
              </span>
            }
            onClick={this.props.onChange}
            stat={this.renderItemFacetStat(item)}
            value={item.metric.key}
          />
        )
    );
  };

  renderOverviewFacet = () => {
    const { domain, selected } = this.props;
    if (!hasBubbleChart(domain.name)) {
      return null;
    }
    return (
      <FacetItem
        active={domain.name === selected}
        disabled={false}
        key={domain.name}
        name={
          <span id={`measure-overview-${domain.name}-name`}>
            {translate('component_measures.domain_overview')}
          </span>
        }
        onClick={this.props.onChange}
        stat={<BubblesIcon size={14} />}
        value={domain.name}
      />
    );
  };

  render() {
    const { domain } = this.props;
    const helper = `component_measures.domain_facets.${domain.name}.help`;
    const translatedHelper = translate(helper);
    return (
      <FacetBox property={domain.name}>
        <FacetHeader
          helper={helper !== translatedHelper ? translatedHelper : undefined}
          name={getLocalizedMetricDomain(domain.name)}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={this.getValues()}
        />

        {this.props.open && (
          <FacetItemsList>
            {this.renderOverviewFacet()}
            {this.renderItemsFacet()}
          </FacetItemsList>
        )}
      </FacetBox>
    );
  }
}
