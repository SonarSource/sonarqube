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
import * as Clipboard from 'clipboard';
import Tooltip from '../../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  token: { name: string; token: string };
}

interface State {
  tooltipShown: boolean;
}

export default class TokensFormNewToken extends React.PureComponent<Props, State> {
  clipboard: Clipboard;
  copyButton: HTMLButtonElement | null;
  mounted: boolean;
  state: State = { tooltipShown: false };

  componentDidMount() {
    this.mounted = true;
    if (this.copyButton) {
      this.clipboard = new Clipboard(this.copyButton);
      this.clipboard.on('success', this.showTooltip);
    }
  }

  componentDidUpdate() {
    this.clipboard.destroy();
    if (this.copyButton) {
      this.clipboard = new Clipboard(this.copyButton);
      this.clipboard.on('success', this.showTooltip);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    this.clipboard.destroy();
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
    const { name, token } = this.props.token;
    const button = (
      <button
        className="js-copy-to-clipboard no-select"
        data-clipboard-text={token}
        ref={node => (this.copyButton = node)}>
        {translate('copy')}
      </button>
    );
    return (
      <div className="panel panel-white big-spacer-top">
        <p className="alert alert-warning">
          {translateWithParameters('users.tokens.new_token_created', name)}
        </p>
        {this.state.tooltipShown ? (
          <Tooltip
            defaultVisible={true}
            placement="bottom"
            overlay={translate('users.tokens.copied')}
            trigger="manual">
            {button}
          </Tooltip>
        ) : (
          button
        )}
        <code className="big-spacer-left text-success">{token}</code>
      </div>
    );
  }
}
