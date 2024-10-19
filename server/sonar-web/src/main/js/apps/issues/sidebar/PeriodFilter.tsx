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
import { BasicSeparator, FacetItem } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { CodeScope } from '../../../helpers/urls';
import { Query } from '../utils';
import { FacetItemsList } from './FacetItemsList';

export interface PeriodFilterProps {
  newCodeSelected: boolean;
  onChange: (changes: Partial<Query>) => void;
}

enum Period {
  NewCode = 'inNewCodePeriod',
}

const PROPERTY = 'period';

export function PeriodFilter(props: PeriodFilterProps) {
  const { newCodeSelected, onChange } = props;

  const handleClick = React.useCallback(() => {
    // We need to clear creation date filters they conflict with the new code period
    onChange({
      createdAfter: undefined,
      createdAt: undefined,
      createdBefore: undefined,
      createdInLast: undefined,
      [Period.NewCode]: !newCodeSelected,
    });
  }, [newCodeSelected, onChange]);

  return (
    <FacetItemsList label={translate('issues.facet', PROPERTY)}>
      <FacetItem
        active={newCodeSelected}
        className="it__search-navigator-facet"
        name={translate('issues.new_code')}
        onClick={handleClick}
        value={newCodeSelected ? CodeScope.New : CodeScope.Overall}
      />

      <BasicSeparator className="sw-mb-5 sw-mt-4" />
    </FacetItemsList>
  );
}
