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
import { Metric } from '../../../app/types';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import { ActionsDropdownItem } from '../../../components/controls/ActionsDropdown';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  metric: Metric;
  onDelete: (metricKey: string) => Promise<void>;
}

export default function DeleteButton({ metric, onDelete }: Props) {
  return (
    <ConfirmButton
      confirmButtonText={translate('delete')}
      confirmData={metric.key}
      isDestructive={true}
      modalBody={translateWithParameters('custom_metrics.delete_metric.confirmation', metric.name)}
      modalHeader={translate('custom_metrics.delete_metric')}
      onConfirm={onDelete}>
      {({ onClick }) => (
        <ActionsDropdownItem className="js-metric-delete" destructive={true} onClick={onClick}>
          {translate('delete')}
        </ActionsDropdownItem>
      )}
    </ConfirmButton>
  );
}
