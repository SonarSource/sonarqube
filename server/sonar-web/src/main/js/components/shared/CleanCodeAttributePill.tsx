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

import { Popover } from '@sonarsource/echoes-react';
import { Pill, PillVariant } from 'design-system';
import React from 'react';
import { DocLink } from '../../helpers/doc-links';
import { translate } from '../../helpers/l10n';
import { CleanCodeAttribute, CleanCodeAttributeCategory } from '../../types/clean-code-taxonomy';
import DocumentationLink from '../common/DocumentationLink';

export interface Props {
  className?: string;
  cleanCodeAttribute?: CleanCodeAttribute;
  cleanCodeAttributeCategory: CleanCodeAttributeCategory;
  type?: 'issue' | 'rule';
}

export function CleanCodeAttributePill(props: Readonly<Props>) {
  const { className, cleanCodeAttributeCategory, cleanCodeAttribute, type = 'issue' } = props;

  return (
    <Popover
      title={translate(
        type,
        cleanCodeAttribute ? 'clean_code_attribute' : 'clean_code_attribute_category',
        cleanCodeAttribute ?? cleanCodeAttributeCategory,
        'title',
      )}
      description={translate(
        'issue',
        cleanCodeAttribute ? 'clean_code_attribute' : 'clean_code_attribute_category',
        cleanCodeAttribute ?? cleanCodeAttributeCategory,
        'advice',
      )}
      footer={
        <DocumentationLink to={DocLink.CleanCodeIntroduction}>
          {translate('learn_more')}
        </DocumentationLink>
      }
    >
      <Pill variant={PillVariant.Accent} data-guiding-id="issue-1" className={className}>
        <span className="sw-font-semibold">
          {translate(type, 'clean_code_attribute_category', cleanCodeAttributeCategory)}
        </span>
        {cleanCodeAttribute && (
          <span> | {translate(type, 'clean_code_attribute', cleanCodeAttribute)}</span>
        )}
      </Pill>
    </Popover>
  );
}
