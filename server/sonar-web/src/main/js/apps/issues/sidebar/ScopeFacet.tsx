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
import { without } from 'lodash';
import * as React from 'react';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import MultipleSelectionHint from '../../../components/facet/MultipleSelectionHint';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import { SOURCE_SCOPES } from '../../../helpers/constants';
import { translate } from '../../../helpers/l10n';
import { Dict } from '../../../types/types';
import { formatFacetStat, Query } from '../utils';

export interface ScopeFacetProps {
  fetching: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  scopes: string[];
  stats: Dict<number> | undefined;
}

export default function ScopeFacet(props: ScopeFacetProps) {
  const { fetching, open, scopes = [], stats = {} } = props;
  const values = scopes.map((scope) => translate('issue.scope', scope));

  return (
    <FacetBox property="scopes">
      <FacetHeader
        fetching={fetching}
        name={translate('issues.facet.scopes')}
        onClear={() => props.onChange({ scopes: [] })}
        onClick={() => props.onToggle('scopes')}
        open={open}
        values={values}
      />

      {open && (
        <>
          <FacetItemsList>
            {SOURCE_SCOPES.map(({ scope, qualifier }) => {
              const active = scopes.includes(scope);
              const stat = stats[scope];

              return (
                <FacetItem
                  active={active}
                  key={scope}
                  name={
                    <span className="display-flex-center">
                      <QualifierIcon
                        className="little-spacer-right"
                        qualifier={qualifier}
                        aria-hidden={true}
                      />{' '}
                      {translate('issue.scope', scope)}
                    </span>
                  }
                  onClick={(itemValue: string, multiple: boolean) => {
                    if (multiple) {
                      props.onChange({
                        scopes: active ? without(scopes, itemValue) : [...scopes, itemValue],
                      });
                    } else {
                      props.onChange({
                        scopes: active && scopes.length === 1 ? [] : [itemValue],
                      });
                    }
                  }}
                  stat={formatFacetStat(stat)}
                  value={scope}
                />
              );
            })}
          </FacetItemsList>
          <MultipleSelectionHint options={Object.keys(stats).length} values={scopes.length} />
        </>
      )}
    </FacetBox>
  );
}
