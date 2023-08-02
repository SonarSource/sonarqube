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
import { Link, Pill } from 'design-system';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { useDocUrl } from '../../helpers/docs';
import { translate } from '../../helpers/l10n';
import { CleanCodeAttributeCategory } from '../../types/issues';
import Tooltip from '../controls/Tooltip';

export interface Props {
  className?: string;
  cleanCodeAttributeCategory: CleanCodeAttributeCategory;
}

export function CleanCodeAttributePill(props: Props) {
  const { className, cleanCodeAttributeCategory } = props;

  const docUrl = useDocUrl('/user-guide/clean-code');

  return (
    <Tooltip
      mouseLeaveDelay={0.25}
      overlay={
        <>
          <p className="sw-mb-4">
            {translate('issue.clean_code_attribute_category', cleanCodeAttributeCategory, 'title')}
          </p>
          <p>
            {translate('issue.clean_code_attribute_category', cleanCodeAttributeCategory, 'advice')}
          </p>
          <hr className="sw-w-full sw-mx-0 sw-my-4" />
          <FormattedMessage
            defaultMessage={translate('learn_more_x')}
            id="learn_more_x"
            values={{
              link: (
                <Link isExternal to={docUrl}>
                  {translate('issue.type.deprecation.documentation')}
                </Link>
              ),
            }}
          />
        </>
      }
    >
      <Pill variant="neutral" className={classNames('sw-mr-2', className)}>
        {translate('issue.clean_code_attribute_category', cleanCodeAttributeCategory, 'issue')}
      </Pill>
    </Tooltip>
  );
}
