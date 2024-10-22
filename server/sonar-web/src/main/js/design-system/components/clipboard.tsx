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

import {
  Button,
  ButtonIcon,
  ButtonSize,
  ButtonVariety,
  IconCopy,
  Tooltip,
  TooltipProvider,
} from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { copy } from 'clipboard';
import React, { ComponentProps, useCallback, useState } from 'react';

const COPY_SUCCESS_NOTIFICATION_LIFESPAN = 1000;

interface ButtonProps {
  children?: React.ReactNode;
  className?: string;
  copiedLabel?: string;
  copyLabel?: string;
  copyValue: string;
  icon?: React.ReactNode;
}

export function ClipboardButton(props: ButtonProps) {
  const {
    icon = <IconCopy />,
    className,
    children,
    copyValue,
    copiedLabel = 'Copied',
    copyLabel = 'Copy',
  } = props;
  const [copySuccess, handleCopy] = useCopyClipboardEffect(copyValue);

  return (
    <TooltipProvider>
      {/* TODO ^ Remove TooltipProvider after design-system is reintegrated into sonar-web */}
      <Tooltip content={copiedLabel} isOpen={copySuccess}>
        <Button
          className={classNames('sw-select-none', className)}
          onClick={handleCopy}
          prefix={icon}
        >
          {children ?? copyLabel}
        </Button>
      </Tooltip>
    </TooltipProvider>
  );
}

interface IconButtonProps {
  Icon?: ComponentProps<typeof ButtonIcon>['Icon'];
  'aria-label'?: string;
  className?: string;
  copiedLabel?: string;
  copyLabel?: string;
  copyValue: string;
  discreet?: boolean;
  size?: ButtonSize;
}

export function ClipboardIconButton(props: IconButtonProps) {
  const {
    className,
    copyValue,
    discreet,
    size = ButtonSize.Medium,
    Icon = IconCopy,
    copiedLabel = 'Copied',
    copyLabel = 'Copy to clipboard',
  } = props;

  const [copySuccess, handleCopy] = useCopyClipboardEffect(copyValue);

  return (
    <TooltipProvider>
      {/* TODO ^ Remove TooltipProvider after design-system is reintegrated into sonar-web */}
      <ButtonIcon
        Icon={Icon}
        ariaLabel={props['aria-label'] ?? copyLabel}
        className={className}
        onClick={handleCopy}
        size={size}
        tooltipContent={copySuccess ? copiedLabel : copyLabel}
        tooltipOptions={copySuccess ? { isOpen: copySuccess } : undefined}
        variety={discreet ? ButtonVariety.DefaultGhost : ButtonVariety.Default}
      />
    </TooltipProvider>
  );
}

export function useCopyClipboardEffect(copyValue: string) {
  const [copySuccess, setCopySuccess] = useState(false);

  const handleCopy = useCallback(
    ({ currentTarget }: React.MouseEvent<HTMLButtonElement>) => {
      const isSuccess = copy(copyValue) === copyValue;
      setCopySuccess(isSuccess);

      if (isSuccess) {
        setTimeout(() => {
          setCopySuccess(false);
        }, COPY_SUCCESS_NOTIFICATION_LIFESPAN);
      }

      currentTarget.focus();
    },
    [copyValue],
  );

  return [copySuccess, handleCopy] as const;
}
