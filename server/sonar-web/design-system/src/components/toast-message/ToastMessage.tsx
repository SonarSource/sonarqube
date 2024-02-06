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
import { Slide, ToastContainer, ToastContainerProps } from 'react-toastify';
import tw from 'twin.macro';
import { TOAST_AUTOCLOSE_DELAY } from '../../helpers/constants';
import { themeBorder, themeColor } from '../../helpers/theme';
import { ToastMessageGlobalStyles } from './ToastMessageGlobalStyles';

/** This wrapper function is required to remove the react-toastify theme prop as we use our own Themes */
function WrappedToastContainer(
  props: Omit<ToastContainerProps, 'theme'> & React.RefAttributes<HTMLDivElement>,
) {
  return <ToastContainer {...props} />;
}

export function ToastMessageContainer() {
  return (
    <>
      <ToastMessageGlobalStyles />

      <StyledToastContainer
        autoClose={TOAST_AUTOCLOSE_DELAY}
        closeOnClick
        draggable
        hideProgressBar
        limit={0}
        newestOnTop={false}
        pauseOnFocusLoss
        pauseOnHover
        position="top-right"
        rtl={false}
        transition={Slide}
      />
    </>
  );
}

const StyledToastContainer = styled(WrappedToastContainer)`
  .Toastify__toast {
    ${tw`sw-p-0`}
  }

  .Toastify__toast-body {
    ${tw`sw-p-0 sw-m-0`}
  }

  .Toastify__close-button {
    ${tw`sw-pt-3 sw-pr-3`}
    color: ${themeColor('toastCloseIcon')};
    opacity: 1;
  }

  .Toastify__toast-theme--light {
    ${tw`sw-inline-flex`}
    ${tw`sw-min-h-10`}
    ${tw`sw-rounded-1`}
    ${tw`sw-mb-2`}

    background-color: ${themeColor('toast')};
    border: ${themeBorder('default')};

    .Toastify__toast-icon {
      align-items: center;
      justify-content: center;
      width: 38px;
      height: calc(100%);
    }

    &.Toastify__toast--default,
    &.Toastify__toast--info,
    &.Toastify__toast--success,
    &.Toastify__toast--warning,
    &.Toastify__toast--error {
      color: ${themeColor('toastText')};
    }

    &.Toastify__toast--default,
    &.Toastify__toast--info {
      border-color: ${themeColor('toastInfoBorder')};

      .Toastify__toast-icon {
        background-color: ${themeColor('toastInfoIconBackground')};
      }
    }

    .Toastify__progress-bar--info {
      background-color: ${themeColor('toastInfoBorder')};
    }

    &.Toastify__toast--success {
      border-color: ${themeColor('toastSuccessBorder')};

      .Toastify__toast-icon {
        background-color: ${themeColor('toastSuccessIconBackground')};
      }
    }

    .Toastify__progress-bar--success {
      background-color: ${themeColor('toastSuccessBorder')};
    }

    &.Toastify__toast--warning {
      border-color: ${themeColor('toastWarningBorder')};

      .Toastify__toast-icon {
        background-color: ${themeColor('toastWarningIconBackground')};
      }
    }

    .Toastify__progress-bar--warning {
      background-color: ${themeColor('toastWarningBorder')};
    }

    &.Toastify__toast--error {
      border-color: ${themeColor('toastErrorBorder')};

      .Toastify__toast-icon {
        background-color: ${themeColor('toastErrorIconBackground')};
      }
    }

    .Toastify__progress-bar--error {
      background-color: ${themeColor('toastErrorBorder')};
    }
  }

  .Toastify__toast-theme--colored {
    .Toastify__progress-bar--default {
      background-color: ${themeColor('toastInfoBorder')};
    }

    &.Toastify__toast--info {
      background-color: ${themeColor('toastInfoBorder')};
      border-color: ${themeColor('toastInfoBorder')};

      .Toastify__toast-icon {
        background-color: ${themeColor('toastInfoBorder')};
      }
    }

    .Toastify__progress-bar--info {
      background-color: ${themeColor('toastInfoIconBackground')};
    }

    &.Toastify__toast--success {
      background-color: ${themeColor('toastSuccessBorder')};
      border-color: ${themeColor('toastSuccessBorder')};

      .Toastify__toast-icon {
        background-color: ${themeColor('toastSuccessBorder')};
      }
    }

    .Toastify__progress-bar--success {
      background-color: ${themeColor('toastSuccessIconBackground')};
    }

    &.Toastify__toast--warning {
      background-color: ${themeColor('toastWarningBorder')};
      border-color: ${themeColor('toastWarningBorder')};

      .Toastify__toast-icon {
        background-color: ${themeColor('toastWarningBorder')};
      }
    }

    .Toastify__progress-bar--warning {
      background-color: ${themeColor('toastWarningIconBackground')};
    }

    &.Toastify__toast--error {
      background-color: ${themeColor('toastErrorBorder')};
      border-color: ${themeColor('toastErrorBorder')};

      .Toastify__toast-icon {
        background-color: ${themeColor('toastErrorBorder')};
      }
    }

    .Toastify__progress-bar--error {
      background-color: ${themeColor('toastErrorIconBackground')};
    }
  }
`;
