/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import DateFormatter from '../../../components/intl/DateFormatter';
import DateFromNowHourPrecision from '../../../components/intl/DateFromNowHourPrecision';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import Tooltip from '../../../components/controls/Tooltip';
import { Button } from '../../../components/ui/buttons';
import { limitComponentName } from '../../../helpers/path';
import { revokeToken } from '../../../api/user-tokens';
import { translate } from '../../../helpers/l10n';

interface Props {
  login: string;
  onRevokeToken: (token: T.UserToken) => void;
  token: T.UserToken;
}

interface State {
  deleting: boolean;
  loading: boolean;
}

export default class TokensFormItem extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { deleting: false, loading: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleRevoke = () => {
    if (this.state.deleting) {
      this.setState({ loading: true });
      revokeToken({ login: this.props.login, name: this.props.token.name }).then(
        () => this.props.onRevokeToken(this.props.token),
        () => {
          if (this.mounted) {
            this.setState({ loading: false, deleting: false });
          }
        }
      );
    } else {
      this.setState({ deleting: true });
    }
  };

  render() {
    const { token } = this.props;
    const { loading } = this.state;
    return (
      <tr>
        <td>
          <Tooltip overlay={token.name}>
            <span>{limitComponentName(token.name)}</span>
          </Tooltip>
        </td>
        <td className="nowrap">
          <DateFromNowHourPrecision date={token.lastConnectionDate} />
        </td>
        <td className="thin nowrap text-right">
          <DateFormatter date={token.createdAt} long={true} />
        </td>
        <td className="thin nowrap text-right">
          <DeferredSpinner loading={loading}>
            <i className="spinner-placeholder" />
          </DeferredSpinner>
          <Button
            className="button-red input-small spacer-left"
            disabled={loading}
            onClick={this.handleRevoke}>
            {this.state.deleting
              ? translate('users.tokens.sure')
              : translate('users.tokens.revoke')}
          </Button>
        </td>
      </tr>
    );
  }
}
