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
  HelperHintIcon,
  SubnavigationAccordion,
  SubnavigationItem,
  SubnavigationSubheading,
} from '~design-system';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import {
  getLocalizedCategoryMetricName,
  getLocalizedMetricDomain,
  getLocalizedMetricName,
  hasMessage,
  translate,
} from '../../../helpers/l10n';
import { useStandardExperienceModeQuery } from '../../../queries/mode';
import { MeasureEnhanced } from '../../../types/types';
import { useBubbleChartMetrics } from '../hooks';
import {
  addMeasureCategories,
  getMetricSubnavigationName,
  hasBubbleChart,
  sortMeasures,
} from '../utils';
import DomainSubnavigationItem from './DomainSubnavigationItem';

interface Props {
  componentKey: string;
  domain: { measures: MeasureEnhanced[]; name: string };
  measures: MeasureEnhanced[];
  onChange: (metric: string) => void;
  open: boolean;
  selected: string;
  showFullMeasures: boolean;
}

export default function DomainSubnavigation(props: Readonly<Props>) {
  const { componentKey, domain, onChange, open, selected, showFullMeasures, measures } = props;
  const { data: isStandardMode = false } = useStandardExperienceModeQuery();
  const helperMessageKey = `component_measures.domain_subnavigation.${domain.name}.help`;
  const helper = hasMessage(helperMessageKey) ? translate(helperMessageKey) : undefined;
  const items = addMeasureCategories(domain.name, domain.measures);
  const bubbles = useBubbleChartMetrics(measures);
  const hasCategories = items.some((item) => typeof item === 'string');
  const translateMetric = hasCategories ? getLocalizedCategoryMetricName : getLocalizedMetricName;
  let sortedItems = sortMeasures(domain.name, items);

  const hasOverview = (domain: string) => {
    return showFullMeasures && hasBubbleChart(bubbles, domain);
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
          <strong className="sw-typo-semibold">{getLocalizedMetricDomain(domain.name)}</strong>
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
        <SubnavigationItem
          active={domain.name === selected}
          ariaCurrent={domain.name === selected}
          onClick={onChange}
          value={domain.name}
        >
          {translate('component_measures.domain_overview')}
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
            componentKey={componentKey}
            measure={item}
            name={getMetricSubnavigationName(item.metric, translateMetric, false, isStandardMode)}
            onChange={onChange}
            selected={selected}
          />
        ),
      )}
    </SubnavigationAccordion>
  );
}
