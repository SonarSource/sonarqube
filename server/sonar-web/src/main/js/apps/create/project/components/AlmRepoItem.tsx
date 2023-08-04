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

import styled from '@emotion/styled';
import classNames from 'classnames';
import {
  ButtonSecondary,
  Card,
  CheckIcon,
  DiscreetLink,
  LightLabel,
  LightPrimary,
  Link,
  themeColor,
} from 'design-system';
import React from 'react';
import { translate } from '../../../../helpers/l10n';
import { getProjectUrl } from '../../../../helpers/urls';

interface AlmRepoItemProps {
  primaryTextNode: React.ReactNode;
  secondaryTextNode?: React.ReactNode;
  sqProjectKey?: string;
  almKey: string;
  almUrl: string;
  almUrlText: string;
  onImport: (key: string) => void;
  almIconSrc: string;
}

export default function AlmRepoItem({
  almKey,
  primaryTextNode,
  secondaryTextNode,
  sqProjectKey,
  almUrl,
  almUrlText,
  almIconSrc,
  onImport,
}: AlmRepoItemProps) {
  return (
    <StyledCard
      key={almKey}
      className={classNames('sw-flex sw-mb-2 sw-px-4', {
        'sw-py-4': sqProjectKey,
        'sw-py-2': !sqProjectKey,
      })}
    >
      <div className="sw-w-[70%] sw-flex sw-mr-1">
        <div className="sw-max-w-[50%] sw-flex sw-items-center">
          <img
            alt="" // Should be ignored by screen readers
            className="sw-h-4 sw-w-4 sw-mr-2"
            src={almIconSrc}
          />
          {sqProjectKey ? (
            <DiscreetLink className="sw-truncate" to={getProjectUrl(sqProjectKey)}>
              <LightPrimary className="sw-body-sm-highlight sw-truncate">
                {primaryTextNode}
              </LightPrimary>
            </DiscreetLink>
          ) : (
            <LightPrimary className="sw-body-sm-highlight sw-truncate">
              {primaryTextNode}
            </LightPrimary>
          )}
        </div>
        <div className="sw-ml-2 sw-flex sw-items-center sw-truncate">
          <LightLabel className="sw-body-sm">{secondaryTextNode}</LightLabel>
        </div>
      </div>
      <div className="sw-flex sw-justify-between sw-items-center sw-flex-1">
        {almUrl && (
          <div className="sw-flex sw-items-center">
            <Link
              className="sw-body-sm-highlight"
              onClick={(e) => e.stopPropagation()}
              target="_blank"
              to={almUrl}
              rel="noopener noreferrer"
            >
              {almUrlText}
            </Link>
          </div>
        )}
        {sqProjectKey ? (
          <div className="sw-flex sw-items-center">
            <CheckIcon />
            <LightPrimary className="sw-ml-2 sw-body-sm">
              {translate('onboarding.create_project.repository_imported')}
            </LightPrimary>
          </div>
        ) : (
          <ButtonSecondary
            onClick={() => {
              onImport(almKey);
            }}
          >
            {translate('onboarding.create_project.import')}
          </ButtonSecondary>
        )}
      </div>
    </StyledCard>
  );
}

const StyledCard = styled(Card)`
  border-color: ${themeColor('almCardBorder')};
`;
