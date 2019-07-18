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
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import MultiSelect from '../../../../components/common/MultiSelect';

interface Props {
  elements: string[];
  filterSelected: (query: string, selectedElements: string[]) => string[];
  metricsTypeFilter?: string[];
  onSearch: (query: string) => Promise<void>;
  onSelect: (item: string) => void;
  onUnselect: (item: string) => void;
  popupPosition?: any;
  renderLabel: (element: string) => React.ReactNode;
  selectedElements: string[];
}

export default function AddGraphMetricPopup({ elements, metricsTypeFilter, ...props }: Props) {
  let footerNode: React.ReactNode = '';

  if (props.selectedElements.length >= 6) {
    footerNode = (
      <Alert className="spacer-left spacer-right spacer-top" variant="info">
        {translate('project_activity.graphs.custom.add_metric_info')}
      </Alert>
    );
  } else if (metricsTypeFilter && metricsTypeFilter.length > 0) {
    footerNode = (
      <Alert className="spacer-left spacer-right spacer-top" variant="info">
        {translateWithParameters(
          'project_activity.graphs.custom.type_x_message',
          metricsTypeFilter
            .map((type: string) => translate('metric.type', type))
            .sort()
            .join(', ')
        )}
      </Alert>
    );
  }

  return (
    <div className="menu abs-width-300">
      <MultiSelect
        allowNewElements={false}
        allowSelection={props.selectedElements.length < 6}
        elements={elements}
        filterSelected={props.filterSelected}
        footerNode={footerNode}
        onSearch={props.onSearch}
        onSelect={props.onSelect}
        onUnselect={props.onUnselect}
        placeholder={translate('search.search_for_tags')}
        renderLabel={props.renderLabel}
        selectedElements={props.selectedElements}
      />
    </div>
  );
}
