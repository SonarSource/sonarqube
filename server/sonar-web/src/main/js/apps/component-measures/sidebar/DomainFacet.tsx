/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
  translate,
  hasMessage
} from '../../../helpers/l10n';

interface Props {
  domain: { name: string; measures: T.MeasureEnhanced[] };
  onChange: (metric: string) => void;
  onToggle: (property: string) => void;
  open: boolean;
  selected: string;
  showFullMeasures: boolean;
}

export default class DomainFacet extends React.PureComponent<Props> {
  getValues = () => {
    const { domain, selected } = this.props;
    const measureSelected = domain.measures.find(measure => measure.metric.key === selected);
    const overviewSelected = domain.name === selected && this.hasOverview(domain.name);
    if (measureSelected) {
      return [getLocalizedMetricName(measureSelected.metric)];
    }
    return overviewSelected ? [translate('component_measures.domain_overview')] : [];
  };

  handleHeaderClick = () => {
    this.props.onToggle(this.props.domain.name);
  };

  hasFacetSelected = (
    domain: { name: string },
    measures: T.MeasureEnhanced[],
    selected: string
  ) => {
    const measureSelected = measures.find(measure => measure.metric.key === selected);
    const overviewSelected = domain.name === selected && this.hasOverview(domain.name);
    return measureSelected || overviewSelected;
  };

  hasOverview = (domain: string) => {
    return this.props.showFullMeasures && hasBubbleChart(domain);
  };

  renderItemFacetStat = (item: T.MeasureEnhanced) => {
    return hasFacetStat(item.metric.key) ? (
      <FacetMeasureValue displayLeak={this.props.showFullMeasures} measure={item} />
    ) : null;
  };

  renderCategoryItem = (item: string) => {
    return this.props.showFullMeasures || item === 'new_code_category' ? (
      <span className="facet search-navigator-facet facet-category" key={item}>
        <span className="facet-name">{translate('component_measures.facet_category', item)}</span>
      </span>
    ) : null;
  };

  renderItemsFacet = () => {
    const { domain, selected } = this.props;
    const items = addMeasureCategories(domain.name, filterMeasures(domain.measures));
    const hasCategories = items.some(item => typeof item === 'string');
    const translateMetric = hasCategories ? getLocalizedCategoryMetricName : getLocalizedMetricName;
    let sortedItems = sortMeasures(domain.name, items);

    sortedItems = sortedItems.filter((item, index) => {
      return (
        typeof item !== 'string' ||
        (index + 1 !== sortedItems.length && typeof sortedItems[index + 1] !== 'string')
      );
    });

    return sortedItems.map(item =>
      typeof item === 'string' ? (
        this.renderCategoryItem(item)
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
          tooltip={translateMetric(item.metric)}
          value={item.metric.key}
        />
      )
    );
  };

  renderOverviewFacet = () => {
    const { domain, selected } = this.props;
    if (!this.hasOverview(domain.name)) {
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
        tooltip={translate('component_measures.domain_overview')}
        value={domain.name}
      />
    );
  };

  render() {
    const { domain } = this.props;
    const helperMessageKey = `component_measures.domain_facets.${domain.name}.help`;
    const helper = hasMessage(helperMessageKey) ? translate(helperMessageKey) : undefined;
    return (
      <FacetBox property={domain.name}>
        <FacetHeader
          helper={helper}
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
