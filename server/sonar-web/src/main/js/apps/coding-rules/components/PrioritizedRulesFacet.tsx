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
import { translate } from '../../../helpers/l10n';
import Facet, { BasicProps } from './Facet';

interface Props extends Omit<BasicProps, 'onChange' | 'values'> {
  disabled: boolean;
  onChange: (changes: { prioritizedRule: boolean | undefined }) => void;
  value: boolean | undefined;
}

export default function PrioritizedRulesFacet(props: Readonly<Props>) {
  const { value, disabled, ...rest } = props;

  const handleChange = (changes: { prioritizedRule: string | any[] }) => {
    const prioritizedRule =
      // empty array is returned when a user cleared the facet
      // otherwise `"true"`, `"false"` or `undefined` can be returned
      Array.isArray(changes.prioritizedRule) || changes.prioritizedRule === undefined
        ? undefined
        : changes.prioritizedRule === 'true';
    props.onChange({ ...changes, prioritizedRule });
  };

  const renderName = (value: string) => translate('coding_rules.filters.prioritizedRule', value);

  return (
    <Facet
      {...rest}
      disabled={disabled}
      disabledHelper={translate('coding_rules.filters.prioritizedRule.disabled')}
      onChange={handleChange}
      options={['true', 'false']}
      property="prioritizedRule"
      renderName={renderName}
      renderTextName={renderName}
      singleSelection
      values={value !== undefined ? [String(value)] : []}
    />
  );
}
