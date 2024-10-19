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
import * as React from 'react';
import { useIntl } from 'react-intl';
import { CleanCodeAttribute, CleanCodeAttributeCategory } from '../../../types/clean-code-taxonomy';

interface Props {
  newCleanCodeAttribute: CleanCodeAttribute;
  newCleanCodeAttributeCategory: CleanCodeAttributeCategory;
  oldCleanCodeAttribute: CleanCodeAttribute;
  oldCleanCodeAttributeCategory: CleanCodeAttributeCategory;
}

export default function CleanCodeAttributeChange(props: Readonly<Props>) {
  const {
    oldCleanCodeAttribute,
    oldCleanCodeAttributeCategory,
    newCleanCodeAttribute,
    newCleanCodeAttributeCategory,
  } = props;

  const intl = useIntl();

  const onlyAttributeChanged = oldCleanCodeAttributeCategory === newCleanCodeAttributeCategory;

  const labels = {
    newCleanCodeAttribute: intl.formatMessage({
      id: `rule.clean_code_attribute.${newCleanCodeAttribute}`,
    }),
    newCleanCodeAttributeCategory: intl.formatMessage({
      id: `rule.clean_code_attribute_category.${newCleanCodeAttributeCategory}`,
    }),
    oldCleanCodeAttribute: intl.formatMessage({
      id: `rule.clean_code_attribute.${oldCleanCodeAttribute}`,
    }),
    oldCleanCodeAttributeCategory: intl.formatMessage({
      id: `rule.clean_code_attribute_category.${oldCleanCodeAttributeCategory}`,
    }),
  };

  return (
    <div>
      {onlyAttributeChanged
        ? intl.formatMessage({ id: 'quality_profiles.changelog.cca_only_changed' }, labels)
        : intl.formatMessage({ id: 'quality_profiles.changelog.cca_and_category_changed' }, labels)}
    </div>
  );
}
