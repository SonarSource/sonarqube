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
import BubblePopup from '../../../../components/common/BubblePopup';
import MultiSelect from '../../../../components/common/MultiSelect';
import { translate } from '../../../../helpers/l10n';

interface Props {
  elements: string[];
  onSearch: (query: string) => Promise<void>;
  onSelect: (item: string) => void;
  onUnselect: (item: string) => void;
  popupPosition?: any;
  renderLabel: (element: string) => React.ReactNode;
  selectedElements: string[];
}

export default function AddGraphMetricPopup(props: Props) {
  return (
    <BubblePopup
      customClass="bubble-popup-bottom-right  bubble-popup-menu abs-width-300"
      position={props.popupPosition}>
      <MultiSelect
        alertMessage={translate('project_activity.graphs.custom.add_metric_info')}
        allowNewElements={false}
        allowSelection={props.selectedElements.length < 6}
        displayAlertMessage={props.selectedElements.length >= 6}
        elements={props.elements}
        onSearch={props.onSearch}
        onSelect={props.onSelect}
        onUnselect={props.onUnselect}
        placeholder={translate('search.search_for_tags')}
        renderLabel={props.renderLabel}
        selectedElements={props.selectedElements}
      />
    </BubblePopup>
  );
}
