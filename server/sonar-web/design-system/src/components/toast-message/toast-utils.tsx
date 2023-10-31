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

import { ReactNode } from 'react';
import { ToastOptions, toast } from 'react-toastify';
import { FlagErrorIcon, FlagSuccessIcon } from '../icons';

export interface Message {
  level: MessageLevel;
  message: string;
}

export enum MessageLevel {
  Error = 'ERROR',
  Success = 'SUCCESS',
}

export function addGlobalErrorMessage(message: ReactNode, overrides?: ToastOptions) {
  return createToast(message, MessageLevel.Error, overrides);
}

export function addGlobalSuccessMessage(message: ReactNode, overrides?: ToastOptions) {
  return createToast(message, MessageLevel.Success, overrides);
}

export function dismissAllGlobalMessages() {
  toast.dismiss();
}

function createToast(message: ReactNode, level: MessageLevel, overrides?: ToastOptions) {
  return toast(
    <div className="fs-mask sw-body-sm sw-p-3 sw-pb-4" data-test={`global-message__${level}`}>
      {message}
    </div>,
    {
      icon: level === MessageLevel.Error ? <FlagErrorIcon /> : <FlagSuccessIcon />,
      type: level === MessageLevel.Error ? 'error' : 'success',
      ...overrides,
    },
  );
}
