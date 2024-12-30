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

import { IconFile, IconFileCode } from '@sonarsource/echoes-react';
import { without } from 'lodash';
import { useIntl } from 'react-intl';
import { FacetBox, FacetItem } from '~design-system';
import { SOURCE_SCOPES } from '../../../helpers/constants';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Dict } from '../../../types/types';
import { Query, formatFacetStat } from '../utils';
import { FacetItemsList } from './FacetItemsList';
import { MultipleSelectionHint } from './MultipleSelectionHint';

export interface ScopeFacetProps {
  fetching: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  scopes: string[];
  stats: Dict<number> | undefined;
}

export function ScopeFacet(props: ScopeFacetProps) {
  const { fetching, open, scopes = [], stats = {} } = props;
  const intl = useIntl();

  const nbSelectableItems = SOURCE_SCOPES.filter(({ scope }) => stats[scope]).length;
  const nbSelectedItems = scopes.length;
  const property = 'scopes';
  const headerId = `facet_${property}`;

  return (
    <FacetBox
      className="it__search-navigator-facet-box it__search-navigator-facet-header"
      count={nbSelectedItems}
      countLabel={translateWithParameters('x_selected', nbSelectedItems)}
      data-property={property}
      id={headerId}
      loading={fetching}
      name={intl.formatMessage({ id: 'issues.facet.scopes' })}
      onClear={() => props.onChange({ scopes: [] })}
      onClick={() => props.onToggle('scopes')}
      open={open}
    >
      <>
        <FacetItemsList labelledby={headerId}>
          {SOURCE_SCOPES.map(({ scope }) => {
            const active = scopes.includes(scope);
            const stat = stats[scope];

            return (
              <FacetItem
                active={active}
                className="it__search-navigator-facet"
                icon={
                  {
                    MAIN: <IconFile className="sw-mr-1" />,
                    TEST: <IconFileCode className="sw-mr-1" />,
                  }[scope]
                }
                key={scope}
                name={translate('issue.scope', scope)}
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
                stat={formatFacetStat(stat) ?? 0}
                value={scope}
              />
            );
          })}
        </FacetItemsList>

        <MultipleSelectionHint
          nbSelectableItems={nbSelectableItems}
          nbSelectedItems={nbSelectedItems}
        />
      </>
    </FacetBox>
  );
}
