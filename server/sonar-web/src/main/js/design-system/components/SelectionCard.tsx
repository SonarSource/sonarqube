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
import classNames from 'classnames';
import { useIntl } from 'react-intl';
import tw from 'twin.macro';
import { themeBorder, themeColor, themeContrast, themeShadow } from '../helpers/theme';
import { LightLabel } from './Text';
import { RecommendedIcon } from './icons/RecommendedIcon';
import { RadioButtonStyled } from './input/RadioButton';

export interface SelectionCardProps {
  children?: React.ReactNode;
  className?: string;
  disabled?: boolean;
  onClick?: VoidFunction;
  recommended?: boolean;
  recommendedReason?: string;
  selected?: boolean;
  title: string;
  titleInfo?: React.ReactNode;
  vertical?: boolean;
}

export function SelectionCard(props: SelectionCardProps) {
  const {
    children,
    className,
    disabled,
    onClick,
    recommended,
    recommendedReason,
    selected = false,
    title,
    titleInfo,
    vertical = false,
  } = props;
  const isActionable = Boolean(onClick);

  const intl = useIntl();

  return (
    <StyledButton
      aria-checked={selected}
      aria-disabled={disabled}
      className={classNames(
        'js-radio-card',
        {
          'card-actionable': isActionable && !disabled,
          'card-vertical': vertical,
          disabled,
          selected,
        },
        className,
      )}
      onClick={isActionable && !disabled && !selected ? onClick : undefined}
      role={isActionable ? 'radio' : 'presentation'}
      tabIndex={disabled ? -1 : 0}
      type="button"
    >
      <StyledContent>
        {isActionable && (
          <div className="sw-items-start sw-mt-1/2 sw-mr-2">
            <RadioButtonStyled
              as="i"
              className={classNames({ 'is-checked': selected, 'is-disabled': disabled })}
            />
          </div>
        )}
        <div>
          <StyledLabel>
            {title}
            <LightLabel>{titleInfo}</LightLabel>
          </StyledLabel>
          <StyledBody>{children}</StyledBody>
        </div>
      </StyledContent>
      {recommended && (
        <StyledRecommended>
          <StyledRecommendedIcon className="sw-mr-1" />
          <span className="sw-align-middle">
            <strong>{intl.formatMessage({ id: 'recommended' })}</strong> {recommendedReason}
          </span>
        </StyledRecommended>
      )}
    </StyledButton>
  );
}

const StyledButton = styled.button`
  ${tw`sw-relative sw-flex sw-flex-col`}
  ${tw`sw-rounded-2`}
  ${tw`sw-box-border`}

  background-color: ${themeColor('backgroundSecondary')};
  border: ${themeBorder('default', 'selectionCardBorder')};
  color: inherit;

  &:focus {
    outline: ${themeBorder('focus', 'selectionCardBorderSelected')};
    box-shadow: ${themeShadow('sm')};
  }

  &.card-vertical {
    ${tw`sw-w-full`}
    min-height: auto;
  }

  &.card-actionable {
    ${tw`sw-cursor-pointer`}

    &:hover {
      border: ${themeBorder('default', 'selectionCardBorderHover')};
      box-shadow: ${themeShadow('sm')};
    }

    &.selected {
      border: ${themeBorder('default', 'selectionCardBorderSelected')};
    }
  }

  &.disabled {
    ${tw`sw-cursor-not-allowed`}

    background-color: ${themeColor('selectionCardDisabled')};
    color: var(--echoes-color-text-disabled);
    border: ${themeBorder('default', 'selectionCardBorderDisabled')};
  }
`;

const StyledContent = styled.div`
  ${tw`sw-my-4 sw-mx-3`}
  ${tw`sw-flex sw-grow`}
  ${tw`sw-text-left`}
`;

const StyledRecommended = styled.div`
  ${tw`sw-typo-default`}
  ${tw`sw-py-2 sw-px-4`}
  ${tw`sw-box-border`}
  ${tw`sw-rounded-b-2`}
  ${tw`sw-w-full`}
  ${tw`sw-text-left`}

  color: ${themeContrast('infoBackground')};
  background-color: ${themeColor('infoBackground')};
`;

const StyledRecommendedIcon = styled(RecommendedIcon)`
  color: ${themeColor('iconInfo')};
  ${tw`sw-align-middle`}
`;

const StyledLabel = styled.label`
  ${tw`sw-flex`}
  ${tw`sw-mb-3 sw-gap-2`}
  ${tw`sw-typo-semibold`}

  color: ${themeColor('selectionCardHeader')};
  cursor: inherit;

  .disabled & {
    color: var(--echoes-color-text-disabled);
  }
`;

const StyledBody = styled.div`
  ${tw`sw-flex sw-grow`}
  ${tw`sw-flex-col sw-justify-between`}
`;
