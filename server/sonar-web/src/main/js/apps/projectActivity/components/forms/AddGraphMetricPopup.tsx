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
import MultiSelect from '../../../../components/common/MultiSelect';
import { translate, translateWithParameters } from '../../../../helpers/l10n';

interface Props {
  elements: string[];
  filterSelected: (query: string, selectedElements: string[]) => string[];
  metricsTypeFilter: string[];
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
      <span className="alert alert-info spacer-left spacer-right spacer-top">
        {translate('project_activity.graphs.custom.add_metric_info')}
      </span>
    );
  } else if (metricsTypeFilter != null && metricsTypeFilter.length > 0) {
    footerNode = (
      <span className="alert alert-info spacer-left spacer-right spacer-top">
        {translateWithParameters(
          'project_activity.graphs.custom.type_x_message',
          metricsTypeFilter
            .map((type: string) => translate('metric.type', type))
            .sort()
            .join(', ')
        )}
      </span>
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
