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
import Clipboard from 'clipboard';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import CopyIcon from '../icons/CopyIcon';
import Tooltip from './Tooltip';
import { Button, ButtonIcon } from './buttons';

export interface State {
  copySuccess: boolean;
}

interface RenderProps {
  setCopyButton: (node: HTMLElement | null) => void;
  copySuccess: boolean;
}

interface Props {
  children: (props: RenderProps) => React.ReactNode;
}

export class ClipboardBase extends React.PureComponent<Props, State> {
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
      this.setState({ copySuccess: true }, () => {
        if (this.copyButton) {
          this.copyButton.focus();
        }
        setTimeout(() => {
          if (this.mounted) {
            this.setState({ copySuccess: false });
          }
        }, 1000);
      });
    }
  };

  render() {
    return this.props.children({
      setCopyButton: this.setCopyButton,
      copySuccess: this.state.copySuccess,
    });
  }
}

export interface ClipboardButtonProps {
  'aria-label'?: string;
  className?: string;
  copyValue: string;
  children?: React.ReactNode;
}

export function ClipboardButton({
  className,
  children,
  copyValue,
  'aria-label': ariaLabel,
}: ClipboardButtonProps) {
  return (
    <ClipboardBase>
      {({ setCopyButton, copySuccess }) => (
        <Tooltip overlay={translate('copied_action')} visible={copySuccess}>
          <Button
            className={classNames('no-select', className)}
            data-clipboard-text={copyValue}
            innerRef={setCopyButton}
            aria-label={
              copySuccess ? translate('copied_action') : ariaLabel ?? translate('copy_to_clipboard')
            }
          >
            {children ?? (
              <>
                <CopyIcon className="little-spacer-right" />
                {translate('copy')}
              </>
            )}
          </Button>
        </Tooltip>
      )}
    </ClipboardBase>
  );
}

export interface ClipboardIconButtonProps {
  'aria-label'?: string;
  className?: string;
  copyValue: string;
}

export function ClipboardIconButton(props: ClipboardIconButtonProps) {
  const { 'aria-label': ariaLabel, className, copyValue } = props;
  return (
    <ClipboardBase>
      {({ setCopyButton, copySuccess }) => {
        return (
          <ButtonIcon
            aria-label={
              copySuccess ? translate('copied_action') : ariaLabel ?? translate('copy_to_clipboard')
            }
            className={classNames('no-select', className)}
            data-clipboard-text={copyValue}
            innerRef={setCopyButton}
            tooltip={copySuccess ? translate('copied_action') : undefined}
            tooltipProps={copySuccess ? { visible: true } : undefined}
          >
            <CopyIcon />
          </ButtonIcon>
        );
      }}
    </ClipboardBase>
  );
}
