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
import { throttle } from 'lodash';
import { getBaseUrl } from '../../../../helpers/urls';
import NavBar from '../../../../components/nav/NavBar';
import './NavBars.css';

interface State {
  top: number;
}

export class FixedNavBar extends React.PureComponent<{}, State> {
  constructor(props: {}) {
    super(props);
    this.state = { top: -100 };
    this.handleScroll = throttle(this.handleScroll, 10);
  }

  componentDidMount() {
    document.addEventListener('scroll', this.handleScroll, true);
  }

  componentWillUnmount() {
    document.removeEventListener('scroll', this.handleScroll, true);
  }

  handleScroll = () => {
    if (document.body.scrollTop > 100 || document.documentElement.scrollTop > 100) {
      this.setState({ top: 0 });
    } else {
      this.setState({ top: -100 });
    }
  };

  render() {
    return (
      <NavBar height={60} top={this.state.top}>
        <NavBarLinks />
      </NavBar>
    );
  }
}

interface TopNavBarProps {
  whiteLogo?: boolean;
}

export function TopNavBar({ whiteLogo }: TopNavBarProps) {
  return (
    <div className="top-navbar">
      <div className="navbar-limited">
        <NavBarLinks whiteLogo={whiteLogo} />
      </div>
    </div>
  );
}

interface NavBarLinksProps {
  whiteLogo?: boolean;
}

function NavBarLinks({ whiteLogo }: NavBarLinksProps) {
  return (
    <>
      <a href={`${getBaseUrl()}/`}>
        <img
          alt="SonarCloud"
          src={`${getBaseUrl()}/images/sonarcloud-logo-${whiteLogo ? 'white' : 'black'}.svg`}
        />
      </a>
      <ul>
        <li>
          <a href={`${getBaseUrl()}/about/pricing`}>Pricing</a>
        </li>
        <li>
          <a href={`${getBaseUrl()}/explore/projects`}>Explore</a>
        </li>
        <li className="outline">
          <a href={`${getBaseUrl()}/sessions/new`}>Log in</a>
        </li>
      </ul>
    </>
  );
}
