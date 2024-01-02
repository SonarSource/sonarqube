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
import { Dict } from '../../types/types';
import AlertErrorIcon from '../icons/AlertErrorIcon';
import AlertSuccessIcon from '../icons/AlertSuccessIcon';
import AlertWarnIcon from '../icons/AlertWarnIcon';
import InfoIcon from '../icons/InfoIcon';
import DeferredSpinner from './DeferredSpinner';

type AlertDisplay = 'banner' | 'inline' | 'block';
export type AlertVariant = 'error' | 'warning' | 'success' | 'info' | 'loading';

export interface AlertProps {
  display?: AlertDisplay;
  variant: AlertVariant;
}

interface AlertVariantInformation {
  icon: JSX.Element;
  color: string;
  borderColor: string;
  backGroundColor: string;
  role: string;
}

const DOUBLE = 2;
const QUADRUPLE = 4;

const StyledAlertIcon = styled.div<{ isBanner: boolean; variantInfo: AlertVariantInformation }>`
  flex: 0 0 auto;
  display: flex;
  justify-content: center;
  align-items: center;
  width: calc(${({ isBanner }) => (isBanner ? DOUBLE : QUADRUPLE)} * ${sizes.gridSize});
  border-right: ${({ isBanner }) => (!isBanner ? '1px solid' : 'none')};
  border-color: ${({ variantInfo }) => variantInfo.borderColor};
`;

const StyledAlertContent = styled.div`
  flex: 1 1 auto;
  overflow: auto;
  text-align: left;
  padding: ${sizes.gridSize} calc(2 * ${sizes.gridSize});
`;

const alertInnerIsBannerMixin = () => css`
  min-width: ${sizes.minPageWidth};
  max-width: ${sizes.maxPageWidth};
  margin-left: auto;
  margin-right: auto;
  padding-left: ${sizes.pagePadding};
  padding-right: ${sizes.pagePadding};
  box-sizing: border-box;
`;

const StyledAlertInner = styled.div<{ isBanner: boolean }>`
  display: flex;
  align-items: stretch;
  ${({ isBanner }) => (isBanner ? alertInnerIsBannerMixin : null)}
`;

const StyledAlert = styled.div<{ isInline: boolean; variantInfo: AlertVariantInformation }>`
  border: 1px solid;
  border-radius: 2px;
  margin-bottom: ${sizes.gridSize};
  border-color: ${({ variantInfo }) => variantInfo.borderColor};
  background-color: ${({ variantInfo }) => variantInfo.backGroundColor};
  color: ${({ variantInfo }) => variantInfo.color};
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
`;

function getAlertVariantInfo(variant: AlertVariant): AlertVariantInformation {
  const variantList: Dict<AlertVariantInformation> = {
    error: {
      icon: <AlertErrorIcon fill={colors.alertIconError} />,
      color: colors.alertTextError,
      borderColor: colors.alertBorderError,
      backGroundColor: colors.alertBackgroundError,
      role: 'alert',
    },
    warning: {
      icon: <AlertWarnIcon fill={colors.alertIconWarning} />,
      color: colors.alertTextWarning,
      borderColor: colors.alertBorderWarning,
      backGroundColor: colors.alertBackgroundWarning,
      role: 'alert',
    },
    success: {
      icon: <AlertSuccessIcon fill={colors.alertIconSuccess} />,
      color: colors.alertTextSuccess,
      borderColor: colors.alertBorderSuccess,
      backGroundColor: colors.alertBackgroundSuccess,
      role: 'status',
    },
    info: {
      icon: <InfoIcon fill={colors.alertIconInfo} />,
      color: colors.alertTextInfo,
      borderColor: colors.alertBorderInfo,
      backGroundColor: colors.alertBackgroundInfo,
      role: 'status',
    },
    loading: {
      icon: <DeferredSpinner timeout={0} />,
      color: colors.alertTextInfo,
      borderColor: colors.alertBorderInfo,
      backGroundColor: colors.alertBackgroundInfo,
      role: 'status',
    },
  };

  return variantList[variant];
}

export function Alert(props: AlertProps & React.HTMLAttributes<HTMLDivElement>) {
  const { className, display, variant, ...domProps } = props;
  const isInline = display === 'inline';
  const isBanner = display === 'banner';
  const variantInfo = getAlertVariantInfo(variant);

  return (
    <StyledAlert
      className={classNames('alert', className)}
      isInline={isInline}
      role={variantInfo.role}
      aria-label={translate('alert.tooltip', variant)}
      variantInfo={variantInfo}
      {...domProps}
    >
      <StyledAlertInner isBanner={isBanner}>
        <StyledAlertIcon isBanner={isBanner} variantInfo={variantInfo}>
          {variantInfo.icon}
        </StyledAlertIcon>
        <StyledAlertContent className="alert-content">{props.children}</StyledAlertContent>
      </StyledAlertInner>
    </StyledAlert>
  );
}
