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
import styled from '@emotion/styled';
import { IconX } from '@sonarsource/echoes-react';
import {
  ButtonPrimary,
  ButtonSecondary,
  InteractiveIcon,
  themeBorder,
  themeColor,
} from 'design-system';
import React, { useState } from 'react';
import { translate } from '../../../helpers/l10n';

interface Props {
  content: string;
  dismissed: boolean;
  onDismiss: () => void;
  onPrimaryButtonClick: () => void;
  primaryButtonLabel: string;
  secondaryButtonLabel: string;
  title: string;
}

export default function PromotedSection({
  content,
  primaryButtonLabel,
  secondaryButtonLabel,
  title,
  dismissed,
  onDismiss,
  onPrimaryButtonClick,
}: Readonly<Props>) {
  const [display, setDisplay] = useState(!dismissed);

  const handlePrimaryButtonClick = () => {
    setDisplay(false);
    onPrimaryButtonClick();
  };

  const handleDismiss = () => {
    setDisplay(false);
    onDismiss();
  };

  if (!display) {
    return null;
  }

  return (
    <StyledWrapper className="sw-p-4 sw-pl-6 sw-my-6 sw-rounded-2">
      <div className="sw-flex sw-justify-between sw-mb-2">
        <StyledTitle className="sw-body-md-highlight">{title}</StyledTitle>
        <InteractiveIcon
          Icon={IconX}
          aria-label={translate('dismiss')}
          onClick={handleDismiss}
          size="small"
        />
      </div>
      <p className="sw-body-sm sw-mb-4">{content}</p>
      <div>
        <ButtonPrimary className="sw-mr-2" onClick={handlePrimaryButtonClick}>
          {primaryButtonLabel}
        </ButtonPrimary>
        <ButtonSecondary onClick={handleDismiss}>{secondaryButtonLabel}</ButtonSecondary>
      </div>
    </StyledWrapper>
  );
}

const StyledWrapper = styled.div`
  background-color: ${themeColor('backgroundPromotedSection')};
  border: ${themeBorder('default')};
`;

const StyledTitle = styled.p`
  color: ${themeColor('primary')};
`;
