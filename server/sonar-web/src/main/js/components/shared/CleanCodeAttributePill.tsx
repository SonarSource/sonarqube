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
import { Pill } from 'design-system';
import React from 'react';
import { translate } from '../../helpers/l10n';
import { CleanCodeAttribute, CleanCodeAttributeCategory } from '../../types/issues';
import DocumentationTooltip from '../common/DocumentationTooltip';

export interface Props {
  className?: string;
  cleanCodeAttributeCategory: CleanCodeAttributeCategory;
  cleanCodeAttribute?: CleanCodeAttribute;
}

export function CleanCodeAttributePill(props: Props) {
  const { className, cleanCodeAttributeCategory, cleanCodeAttribute } = props;

  const translationKey = cleanCodeAttribute
    ? `issue.clean_code_attribute.${cleanCodeAttribute}`
    : `issue.clean_code_attribute_category.${cleanCodeAttributeCategory}`;

  return (
    <DocumentationTooltip
      content={
        <>
          <p className="sw-mb-4">{translate(translationKey, 'title')}</p>
          <p>{translate(translationKey, 'advice')}</p>
        </>
      }
      links={[
        {
          href: '/user-guide/clean-code',
          label: translate('learn_more'),
        },
      ]}
    >
      <Pill variant="neutral" className={classNames('sw-mr-2', className)}>
        <span className={classNames({ 'sw-font-semibold': !!cleanCodeAttribute })}>
          {translate('issue.clean_code_attribute_category', cleanCodeAttributeCategory, 'issue')}
        </span>
        {cleanCodeAttribute && (
          <span> | {translate('issue.clean_code_attribute', cleanCodeAttribute)}</span>
        )}
      </Pill>
    </DocumentationTooltip>
  );
}
