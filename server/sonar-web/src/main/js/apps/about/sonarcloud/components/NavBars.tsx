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
import { throttle } from 'lodash';
import * as React from 'react';
import NavBar from 'sonar-ui-common/components/ui/NavBar';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import './NavBars.css';

interface Props {
  onPricingPage?: boolean;
}

interface State {
  top: number;
}

export class FixedNavBar extends React.PureComponent<Props, State> {
  constructor(props: Props) {
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
    const scrollTop =
      document.body.scrollTop ||
      (document.documentElement ? document.documentElement.scrollTop : 0);
    if (scrollTop > 100) {
      this.setState({ top: 0 });
    } else {
      this.setState({ top: -100 });
    }
  };

  render() {
    return (
      <NavBar height={60} top={this.state.top}>
        <NavBarLinks onPricingPage={this.props.onPricingPage} />
      </NavBar>
    );
  }
}

interface TopNavBarProps {
  onPricingPage?: boolean;
  whiteLogo?: boolean;
}

export function TopNavBar({ onPricingPage, whiteLogo }: TopNavBarProps) {
  return (
    <div className="top-navbar">
      <div className="navbar-limited">
        <NavBarLinks onPricingPage={onPricingPage} whiteLogo={whiteLogo} />
      </div>
    </div>
  );
}

interface NavBarLinksProps {
  onPricingPage?: boolean;
  whiteLogo?: boolean;
}

function NavBarLinks({ onPricingPage, whiteLogo }: NavBarLinksProps) {
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
          <a className={onPricingPage ? 'active' : ''} href={`${getBaseUrl()}/about/pricing`}>
            Pricing
          </a>
        </li>
        <li className="outline">
          <a href={`${getBaseUrl()}/sessions/new`}>Log in</a>
        </li>
      </ul>
    </>
  );
}
