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
import classNames from 'classnames';
import { sortBy } from 'lodash';
import * as React from 'react';
import withKeyboardNavigation, {
  WithKeyboardNavigationProps,
} from '../../../components/hoc/withKeyboardNavigation';
import { getComponentMeasureUniqueKey } from '../../../helpers/component';
import { BranchLike } from '../../../types/branch-like';
import { ComponentMeasure } from '../../../types/types';
import ComponentName from './ComponentName';

export interface SearchResultsProps extends WithKeyboardNavigationProps {
  branchLike?: BranchLike;
  rootComponent: ComponentMeasure;
  newCodeSelected?: boolean;
}

function SearchResults(props: SearchResultsProps) {
  const { branchLike, components, newCodeSelected, rootComponent, selected } = props;

  return (
    <ul>
      {components &&
        components.length > 0 &&
        sortBy(
          components,
          (c) => c.qualifier,
          (c) => c.name.toLowerCase(),
          (c) => (c.branch ? c.branch.toLowerCase() : '')
        ).map((component) => (
          <li
            className={classNames({ selected: selected?.key === component.key })}
            key={getComponentMeasureUniqueKey(component)}
          >
            <ComponentName
              branchLike={branchLike}
              canBrowse={true}
              component={component}
              rootComponent={rootComponent}
              newCodeSelected={newCodeSelected}
            />
          </li>
        ))}
    </ul>
  );
}

export default withKeyboardNavigation(SearchResults);
