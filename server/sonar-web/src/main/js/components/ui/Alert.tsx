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
import { css } from '@emotion/react';
import styled from '@emotion/styled';
import classNames from 'classnames';
import * as React from 'react';
import { colors, sizes } from '../../app/theme';
import { translate } from '../../helpers/l10n';
import AlertErrorIcon from '../icons/AlertErrorIcon';
import AlertSuccessIcon from '../icons/AlertSuccessIcon';
import AlertWarnIcon from '../icons/AlertWarnIcon';
import InfoIcon from '../icons/InfoIcon';
import Spinner from './Spinner';

type AlertDisplay = 'banner' | 'inline' | 'block';
export type AlertVariant = 'error' | 'warning' | 'success' | 'info' | 'loading';

export interface AlertProps {
  display?: AlertDisplay;
  variant: AlertVariant;
  live?: boolean;
}

const DOUBLE = 2;
const QUADRUPLE = 4;

const alertInnerIsBannerMixin = () => css`
  min-width: ${sizes.minPageWidth};
  max-width: ${sizes.maxPageWidth};
  margin-left: auto;
  margin-right: auto;
  padding-left: ${sizes.pagePadding};
  padding-right: ${sizes.pagePadding};
  box-sizing: border-box;
`;

const StyledAlert = styled.div<{
  isInline: boolean;
  color: string;
  backGroundColor: string;
  borderColor: string;
  isBanner: boolean;
}>`
  border: 1px solid;
  border-radius: 2px;
  margin-bottom: ${sizes.gridSize};
  border-color: ${({ borderColor }) => borderColor};
  background-color: ${({ backGroundColor }) => backGroundColor};
  color: ${({ color }) => color};
  display: ${({ isInline }) => (isInline ? 'inline-block' : 'block')};

  :empty {
    display: none;
  }

  a,
  .button-link {
    border-color: ${colors.primarya40};
  }

  a: hover,
  .button-link:hover {
    border-color: ${colors.darkBlue};
  }

  & .alert-inner {
    display: flex;
    align-items: stretch;
    ${({ isBanner }) => (isBanner ? alertInnerIsBannerMixin : null)}
  }

  & .alert-icon {
    flex: 0 0 auto;
    display: flex;
    justify-content: center;
    align-items: center;
    width: calc(${({ isBanner }) => (isBanner ? DOUBLE : QUADRUPLE)} * ${sizes.gridSize});
    border-right: ${({ isBanner }) => (!isBanner ? '1px solid' : 'none')};
    border-color: ${({ borderColor }) => borderColor};
  }

  & .alert-content {
    flex: 1 1 auto;
    overflow: auto;
    text-align: left;
    padding: ${sizes.gridSize} calc(2 * ${sizes.gridSize});
  }
`;

function getAlertVariantInfo(variant: AlertVariant) {
  const variantList = {
    error: {
      icon: (
        <AlertErrorIcon label={translate('alert.tooltip.error')} fill={colors.alertIconError} />
      ),
      color: colors.alertTextError,
      borderColor: colors.alertBorderError,
      backGroundColor: colors.alertBackgroundError,
    },
    warning: {
      icon: (
        <AlertWarnIcon label={translate('alert.tooltip.warning')} fill={colors.alertIconWarning} />
      ),
      color: colors.alertTextWarning,
      borderColor: colors.alertBorderWarning,
      backGroundColor: colors.alertBackgroundWarning,
    },
    success: {
      icon: (
        <AlertSuccessIcon
          label={translate('alert.tooltip.success')}
          fill={colors.alertIconSuccess}
        />
      ),
      color: colors.alertTextSuccess,
      borderColor: colors.alertBorderSuccess,
      backGroundColor: colors.alertBackgroundSuccess,
    },
    info: {
      icon: <InfoIcon label={translate('alert.tooltip.info')} fill={colors.alertIconInfo} />,
      color: colors.alertTextInfo,
      borderColor: colors.alertBorderInfo,
      backGroundColor: colors.alertBackgroundInfo,
    },
    loading: {
      icon: <Spinner />,
      color: colors.alertTextInfo,
      borderColor: colors.alertBorderInfo,
      backGroundColor: colors.alertBackgroundInfo,
    },
  } as const;

  return variantList[variant];
}

export function Alert(props: AlertProps & React.HTMLAttributes<HTMLDivElement>) {
  const { className, display, variant, children, live, ...domProps } = props;
  const isInline = display === 'inline';
  const isBanner = display === 'banner';
  const variantInfo = getAlertVariantInfo(variant);

  return (
    <StyledAlert
      className={classNames('alert', className)}
      isBanner={isBanner}
      isInline={isInline}
      color={variantInfo.color}
      borderColor={variantInfo.borderColor}
      backGroundColor={variantInfo.backGroundColor}
      {...domProps}
    >
      {children && (
        <div className="alert-inner">
          <div className="alert-icon">{variantInfo.icon}</div>
          <div className="alert-content">{children}</div>
        </div>
      )}
    </StyledAlert>
  );
}
