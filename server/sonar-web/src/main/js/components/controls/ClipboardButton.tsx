/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import * as classNames from 'classnames';
import * as Clipboard from 'clipboard';
import Tooltip from './Tooltip';
import { translate } from '../../helpers/l10n';

interface Props {
  className?: string;
  copyValue: string;
  tooltipPlacement?: string;
}

interface State {
  tooltipShown: boolean;
}

export default class ClipboardButton extends React.PureComponent<Props, State> {
  clipboard?: Clipboard;
  copyButton?: HTMLButtonElement | null;
  mounted = false;
  state: State = { tooltipShown: false };

  componentDidMount() {
    this.mounted = true;
    if (this.copyButton) {
      this.clipboard = new Clipboard(this.copyButton);
      this.clipboard.on('success', this.showTooltip);
    }
  }

  componentDidUpdate() {
    if (this.clipboard) {
      this.clipboard.destroy();
    }
    if (this.copyButton) {
      this.clipboard = new Clipboard(this.copyButton);
      this.clipboard.on('success', this.showTooltip);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    if (this.clipboard) {
      this.clipboard.destroy();
    }
  }

  showTooltip = () => {
    if (this.mounted) {
      this.setState({ tooltipShown: true });
      setTimeout(() => {
        if (this.mounted) {
          this.setState({ tooltipShown: false });
        }
      }, 1000);
    }
  };

  render() {
    const button = (
      <button
        className={classNames('js-copy-to-clipboard no-select', this.props.className)}
        data-clipboard-text={this.props.copyValue}
        ref={node => (this.copyButton = node)}>
        {translate('copy')}
      </button>
    );
    if (this.state.tooltipShown) {
      return (
        <Tooltip
          defaultVisible={true}
          placement={this.props.tooltipPlacement || 'bottom'}
          overlay={translate('copied_action')}
          trigger="manual">
          {button}
        </Tooltip>
      );
    }
    return button;
  }
}
