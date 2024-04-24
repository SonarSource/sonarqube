/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import {
  BareButton,
  HelperHintIcon,
  SubnavigationAccordion,
  SubnavigationItem,
  SubnavigationSubheading,
} from 'design-system';
import React from 'react';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import {
  getLocalizedCategoryMetricName,
  getLocalizedMetricDomain,
  getLocalizedMetricName,
  hasMessage,
  translate,
} from '../../../helpers/l10n';
import { MeasureEnhanced } from '../../../types/types';
import {
  addMeasureCategories,
  getMetricSubnavigationName,
  hasBubbleChart,
  sortMeasures,
} from '../utils';
import DomainSubnavigationItem from './DomainSubnavigationItem';

interface Props {
  domain: { measures: MeasureEnhanced[]; name: string };
  onChange: (metric: string) => void;
  open: boolean;
  selected: string;
  showFullMeasures: boolean;
}

export default function DomainSubnavigation(props: Readonly<Props>) {
  const { domain, onChange, open, selected, showFullMeasures } = props;
  const helperMessageKey = `component_measures.domain_subnavigation.${domain.name}.help`;
  const helper = hasMessage(helperMessageKey) ? translate(helperMessageKey) : undefined;
  const items = addMeasureCategories(domain.name, domain.measures);
  const hasCategories = items.some((item) => typeof item === 'string');
  const translateMetric = hasCategories ? getLocalizedCategoryMetricName : getLocalizedMetricName;
  let sortedItems = sortMeasures(domain.name, items);

  const hasOverview = (domain: string) => {
    return showFullMeasures && hasBubbleChart(domain);
  };

  // sortedItems contains both measures (type object) and categories (type string)
  // here we are filtering out categories that don't contain any measures (happen on the measures page for PRs)
  sortedItems = sortedItems.filter((item, index) => {
    return (
      typeof item === 'object' ||
      (index < sortedItems.length - 1 && typeof sortedItems[index + 1] === 'object')
    );
  });
  return (
    <SubnavigationAccordion
      header={
        <div className="sw-flex sw-items-center sw-gap-3">
          <strong className="sw-body-sm-highlight">{getLocalizedMetricDomain(domain.name)}</strong>
          {helper && (
            <HelpTooltip overlay={helper}>
              <HelperHintIcon aria-hidden="false" description={helper} />
            </HelpTooltip>
          )}
        </div>
      }
      initExpanded={open}
      id={`measure-${domain.name}`}
    >
      {hasOverview(domain.name) && (
        <SubnavigationItem active={domain.name === selected} onClick={onChange} value={domain.name}>
          <BareButton aria-current={domain.name === selected}>
            {translate('component_measures.domain_overview')}
          </BareButton>
        </SubnavigationItem>
      )}

      {sortedItems.map((item) =>
        typeof item === 'string' ? (
          showFullMeasures && (
            <SubnavigationSubheading key={item}>
              {translate('component_measures.subnavigation_category', item)}
            </SubnavigationSubheading>
          )
        ) : (
          <DomainSubnavigationItem
            key={item.metric.key}
            measure={item}
            name={getMetricSubnavigationName(item.metric, translateMetric)}
            onChange={onChange}
            selected={selected}
          />
        ),
      )}
    </SubnavigationAccordion>
  );
}
