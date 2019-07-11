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
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';

interface Props {
  className?: string;
  children: React.ReactNode;
  onReload: () => void;
  style?: React.CSSProperties;
  sessionUrl: string;
}

export default class LoginLink extends React.PureComponent<Props> {
  handleLoginClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.stopPropagation();
    event.currentTarget.blur();

    (window as any).authenticationDone = () => {
      this.props.onReload();
    };

    const returnTo = encodeURIComponent(window.location.pathname + '?type=authenticated');
    window.open(
      `${getBaseUrl()}/${this.props.sessionUrl}?return_to=${returnTo}`,
      'Login on SonarCloud',
      'toolbar=0,status=0,width=377,height=380'
    );
  };

  render() {
    return (
      <a
        className={this.props.className}
        href="#"
        onClick={this.handleLoginClick}
        style={this.props.style}>
        {this.props.children}
      </a>
    );
  }
}
