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
import Button, { ButtonAppearances } from '@atlaskit/button';
import { getBaseUrl } from '@sqcore/helpers/urls';

interface Props {
  appearance: ButtonAppearances;
  children: React.ReactNode;
  icon?: JSX.Element;
  onReload: () => void;
  style?: React.CSSProperties;
  sessionUrl: string;
}

export default class LoginButton extends React.PureComponent<Props> {
  handleLoginClick = (event: React.SyntheticEvent<MouseEvent>) => {
    event.preventDefault();
    event.stopPropagation();

    (window as any).authenticationDone = () => {
      this.props.onReload();
    };

    const returnTo = encodeURIComponent('/integration/bitbucketcloud/after_login');
    window.open(
      `${getBaseUrl()}/${this.props.sessionUrl}?return_to=${returnTo}`,
      'Login on SonarCloud',
      'toolbar=0,status=0,width=1100,height=640'
    );
  };

  render() {
    return (
      <Button
        appearance={this.props.appearance}
        iconBefore={this.props.icon}
        onClick={this.handleLoginClick}>
        {this.props.children}
      </Button>
    );
  }
}
