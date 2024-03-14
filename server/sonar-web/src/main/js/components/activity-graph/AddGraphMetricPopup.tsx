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
import { Badge, FlagMessage, MultiSelectMenu } from 'design-system';
import * as React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { DEPRECATED_ACTIVITY_METRICS } from '../../helpers/constants';
import { getLocalizedMetricName, translate, translateWithParameters } from '../../helpers/l10n';
import { MetricKey } from '../../types/metrics';
import DocumentationLink from '../common/DocumentationLink';

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
}: AddGraphMetricPopupProps) {
  const intl = useIntl();
  let footerNode: React.ReactNode = '';

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

  const renderLabel = (key: string) => {
    const metricName = getLocalizedMetricName({ key });
    const isDeprecated = DEPRECATED_ACTIVITY_METRICS.includes(key as MetricKey);

    return (
      <>
        {metricName}
        {isDeprecated && (
          <Badge className="sw-ml-1">{intl.formatMessage({ id: 'deprecated' })}</Badge>
        )}
      </>
    );
  };

  const renderTooltip = (key: string) => {
    const isDeprecated = DEPRECATED_ACTIVITY_METRICS.includes(key as MetricKey);

    if (isDeprecated) {
      return (
        <FormattedMessage
          id="project_activity.custom_metric.deprecated"
          tagName="div"
          values={{
            learn_more: (
              <DocumentationLink
                className="sw-ml-2 sw-whitespace-nowrap"
                to="/user-guide/clean-code/code-analysis/"
              >
                {intl.formatMessage({ id: 'learn_more' })}
              </DocumentationLink>
            ),
          }}
        />
      );
    }

    return null;
  };

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
      renderLabel={renderLabel}
      renderTooltip={renderTooltip}
      selectedElements={props.selectedElements}
      listSize={0}
    />
  );
}
