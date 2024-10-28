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

import { ReactNode, useCallback } from 'react';
import { FlagMessage, MultiSelectMenu } from '~design-system';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { getLocalizedMetricName, translate, translateWithParameters } from '../../helpers/l10n';

export interface AddGraphMetricPopupProps {
  elements: string[];
  filterSelected: (query: string, selectedElements: string[]) => string[];
  metricsTypeFilter?: string[];
  onSearch: (query: string) => Promise<void>;
  onSelect: (item: string) => void;
  onUnselect: (item: string) => void;
  popupPosition?: any;
  selectedElements: string[];
}

export default function AddGraphMetricPopup({
  elements,
  metricsTypeFilter,
  ...props
}: Readonly<AddGraphMetricPopupProps>) {
  let footerNode: ReactNode = '';

  if (props.selectedElements.length >= 6) {
    footerNode = (
      <FlagMessage className="sw-m-2" variant="info">
        {translate('project_activity.graphs.custom.add_metric_info')}
      </FlagMessage>
    );
  } else if (metricsTypeFilter && metricsTypeFilter.length > 0) {
    footerNode = (
      <FlagMessage className="sw-m-2" variant="info">
        {translateWithParameters(
          'project_activity.graphs.custom.type_x_message',
          metricsTypeFilter
            .map((type: string) => translate('metric.type', type))
            .sort((a, b) => a.localeCompare(b))
            .join(', '),
        )}
      </FlagMessage>
    );
  }

  const renderLabel = useCallback((key: MetricKey) => {
    return getLocalizedMetricName({ key });
  }, []);

  return (
    <MultiSelectMenu
      createElementLabel=""
      searchInputAriaLabel={translate('project_activity.graphs.custom.select_metric')}
      allowNewElements={false}
      allowSelection={props.selectedElements.length < 6}
      elements={elements}
      filterSelected={props.filterSelected}
      footerNode={footerNode}
      noResultsLabel={translateWithParameters('no_results')}
      onSearch={props.onSearch}
      onSelect={(item: string) => elements.includes(item) && props.onSelect(item)}
      onUnselect={props.onUnselect}
      placeholder={translate('search.search_for_metrics')}
      renderAriaLabel={renderLabel}
      renderLabel={renderLabel}
      selectedElements={props.selectedElements}
      listSize={0}
    />
  );
}
