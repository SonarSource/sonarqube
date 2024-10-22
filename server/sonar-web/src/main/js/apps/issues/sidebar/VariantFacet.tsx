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

import { orderBy, sortBy, without } from 'lodash';
import * as React from 'react';
import { FacetBox, FacetItem, Note } from '~design-system';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Dict } from '../../../types/types';
import { Query, formatFacetStat } from '../utils';
import { FacetItemsList } from './FacetItemsList';
import { MultipleSelectionHint } from './MultipleSelectionHint';

interface VariantFacetProps {
  fetching: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  stats?: Dict<number>;
  values: string[];
}

const FACET_NAME = 'codeVariants';

export function VariantFacet(props: VariantFacetProps) {
  const { open, fetching, stats = {}, values, onToggle, onChange } = props;

  const handleClear = React.useCallback(() => {
    onChange({ [FACET_NAME]: undefined });
  }, [onChange]);

  const handleHeaderClick = React.useCallback(() => {
    onToggle(FACET_NAME);
  }, [onToggle]);

  const handleItemClick = React.useCallback(
    (value: string, multiple: boolean) => {
      if (value === '') {
        onChange({ [FACET_NAME]: undefined });
      } else if (multiple) {
        const newValues = orderBy(
          values.includes(value) ? without(values, value) : [...values, value],
        );

        onChange({ [FACET_NAME]: newValues });
      } else {
        onChange({
          [FACET_NAME]: values.includes(value) && values.length === 1 ? [] : [value],
        });
      }
    },

    [values, onChange],
  );

  const id = `facet_${FACET_NAME}`;

  const nbSelectableItems = Object.keys(stats).length;
  const nbSelectedItems = values.length;

  return (
    <FacetBox
      className="it__search-navigator-facet-box it__search-navigator-facet-header"
      clearIconLabel={translate('clear')}
      count={nbSelectedItems}
      countLabel={translateWithParameters('x_selected', nbSelectedItems)}
      data-property={FACET_NAME}
      id={id}
      loading={fetching}
      name={translate('issues.facet', FACET_NAME)}
      onClear={handleClear}
      onClick={handleHeaderClick}
      open={open}
    >
      <FacetItemsList labelledby={id}>
        {nbSelectableItems === 0 && (
          <Note as="div" className="sw-mb-2 sw-text-center">
            {translate('no_results')}
          </Note>
        )}

        {sortBy(
          Object.keys(stats),
          (key) => -stats[key],
          (key) => key,
        ).map((codeVariant) => (
          <FacetItem
            active={values.includes(codeVariant)}
            className="it__search-navigator-facet"
            key={codeVariant}
            name={codeVariant}
            onClick={handleItemClick}
            stat={formatFacetStat(stats[codeVariant])}
            value={codeVariant}
          />
        ))}
      </FacetItemsList>

      <MultipleSelectionHint
        nbSelectableItems={nbSelectableItems}
        nbSelectedItems={nbSelectedItems}
      />
    </FacetBox>
  );
}
