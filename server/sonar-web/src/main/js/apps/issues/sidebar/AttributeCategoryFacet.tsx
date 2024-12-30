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

import { FacetHelp } from '../../../components/facets/FacetHelp';
import { CLEAN_CODE_CATEGORIES } from '../../../helpers/constants';
import { DocLink } from '../../../helpers/doc-links';
import { CleanCodeAttributeCategory } from '../../../types/clean-code-taxonomy';
import { CommonProps, SimpleListStyleFacet } from './SimpleListStyleFacet';

interface Props extends CommonProps {
  categories: Array<CleanCodeAttributeCategory>;
}

export function AttributeCategoryFacet(props: Props) {
  const { categories = [], ...rest } = props;

  return (
    <SimpleListStyleFacet
      property="cleanCodeAttributeCategories"
      itemNamePrefix="issue.clean_code_attribute_category"
      listItems={CLEAN_CODE_CATEGORIES}
      selectedItems={categories}
      help={
        <FacetHelp
          property="cleanCodeAttributeCategories"
          noDescription
          link={DocLink.CleanCodeDefinition}
        />
      }
      {...rest}
    />
  );
}
