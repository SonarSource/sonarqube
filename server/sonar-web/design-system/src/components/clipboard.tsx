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
import classNames from 'classnames';
import Clipboard from 'clipboard';
import React from 'react';
import { INTERACTIVE_TOOLTIP_DELAY } from '../helpers/constants';
import { DiscreetInteractiveIcon, InteractiveIcon, InteractiveIconSize } from './InteractiveIcon';
import { Tooltip } from './Tooltip';
import { ButtonSecondary } from './buttons';
import { CopyIcon } from './icons/CopyIcon';
import { IconProps } from './icons/Icon';

const COPY_SUCCESS_NOTIFICATION_LIFESPAN = 1000;

export interface State {
  copySuccess: boolean;
}

interface RenderProps {
  copySuccess: boolean;
  setCopyButton: (node: HTMLElement | null) => void;
}

interface BaseProps {
  children: (props: RenderProps) => React.ReactNode;
}

export class ClipboardBase extends React.PureComponent<BaseProps, State> {
  private clipboard?: Clipboard;
  private copyButton?: HTMLElement | null;
  mounted = false;
  state: State = { copySuccess: false };

  componentDidMount() {
    this.mounted = true;
    if (this.copyButton) {
      this.clipboard = new Clipboard(this.copyButton);
      this.clipboard.on('success', this.handleSuccessCopy);
    }
  }

  componentDidUpdate() {
    if (this.clipboard) {
      this.clipboard.destroy();
    }
    if (this.copyButton) {
      this.clipboard = new Clipboard(this.copyButton);
      this.clipboard.on('success', this.handleSuccessCopy);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    if (this.clipboard) {
      this.clipboard.destroy();
    }
  }

  setCopyButton = (node: HTMLElement | null) => {
    this.copyButton = node;
  };

  handleSuccessCopy = () => {
    if (this.mounted) {
      this.setState({ copySuccess: true });
      setTimeout(() => {
        if (this.mounted) {
          this.setState({ copySuccess: false });
        }
      }, COPY_SUCCESS_NOTIFICATION_LIFESPAN);
    }
  };

  render() {
    return this.props.children({
      setCopyButton: this.setCopyButton,
      copySuccess: this.state.copySuccess,
    });
  }
}

interface ButtonProps {
  children?: React.ReactNode;
  className?: string;
  copiedLabel?: string;
  copyLabel?: string;
  copyValue: string;
  icon?: React.ReactNode;
}

export function ClipboardButton({
  icon = <CopyIcon />,
  className,
  children,
  copyValue,
  copiedLabel = 'Copied',
  copyLabel = 'Copy',
}: ButtonProps) {
  return (
    <ClipboardBase>
      {({ setCopyButton, copySuccess }) => (
        <Tooltip overlay={copiedLabel} visible={copySuccess}>
          <ButtonSecondary
            className={classNames('sw-select-none', className)}
            data-clipboard-text={copyValue}
            icon={icon}
            innerRef={setCopyButton}
          >
            {children ?? copyLabel}
          </ButtonSecondary>
        </Tooltip>
      )}
    </ClipboardBase>
  );
}

interface IconButtonProps {
  Icon?: React.ComponentType<React.PropsWithChildren<IconProps>>;
  'aria-label'?: string;
  className?: string;
  copiedLabel?: string;
  copyLabel?: string;
  copyValue: string;
  discreet?: boolean;
  size?: InteractiveIconSize;
}

export function ClipboardIconButton(props: IconButtonProps) {
  const {
    className,
    copyValue,
    discreet,
    size = 'small',
    Icon = CopyIcon,
    copiedLabel = 'Copied',
    copyLabel = 'Copy to clipboard',
  } = props;
  const InteractiveIconComponent = discreet ? DiscreetInteractiveIcon : InteractiveIcon;

  return (
    <ClipboardBase>
      {({ setCopyButton, copySuccess }) => {
        return (
          <Tooltip
            mouseEnterDelay={INTERACTIVE_TOOLTIP_DELAY}
            overlay={
              <div className="sw-w-abs-150 sw-text-center">
                {copySuccess ? copiedLabel : copyLabel}
              </div>
            }
            {...(copySuccess ? { visible: copySuccess } : undefined)}
          >
            <InteractiveIconComponent
              Icon={Icon}
              aria-label={props['aria-label'] ?? copyLabel}
              className={className}
              data-clipboard-text={copyValue}
              innerRef={setCopyButton}
              size={size}
            />
          </Tooltip>
        );
      }}
    </ClipboardBase>
  );
}
