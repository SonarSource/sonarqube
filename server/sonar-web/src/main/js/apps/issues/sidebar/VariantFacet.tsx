/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import MultipleSelectionHint from '../../../components/facet/MultipleSelectionHint';
import { translate } from '../../../helpers/l10n';
import { Dict } from '../../../types/types';
import { Query, formatFacetStat } from '../utils';

interface VariantFacetProps {
  fetching: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  stats?: Dict<number>;
  values: string[];
}

const FACET_NAME = 'codeVariants';

export default function VariantFacet(props: VariantFacetProps) {
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
          values.includes(value) ? without(values, value) : [...values, value]
        );
        onChange({ [FACET_NAME]: newValues });
      } else {
        onChange({
          [FACET_NAME]: values.includes(value) && values.length === 1 ? [] : [value],
        });
      }
    },
    [values, onChange]
  );

  const id = `facet_${FACET_NAME}`;

  return (
    <FacetBox property={FACET_NAME}>
      <FacetHeader
        fetching={fetching}
        name={translate('issues.facet', FACET_NAME)}
        id={id}
        onClear={handleClear}
        onClick={handleHeaderClick}
        open={open}
        values={values}
      />
      {open && (
        <>
          <FacetItemsList labelledby={id}>
            {Object.keys(stats).length === 0 && (
              <div className="note spacer-bottom">{translate('no_results')}</div>
            )}
            {sortBy(
              Object.keys(stats),
              (key) => -stats[key],
              (key) => key
            ).map((codeVariant) => (
              <FacetItem
                active={values.includes(codeVariant)}
                key={codeVariant}
                name={codeVariant}
                onClick={handleItemClick}
                stat={formatFacetStat(stats[codeVariant])}
                value={codeVariant}
              />
            ))}
          </FacetItemsList>
          <MultipleSelectionHint options={Object.keys(stats).length} values={values.length} />
        </>
      )}
    </FacetBox>
  );
}
