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
import { Link } from 'react-router';
import { getBaseUrl } from '../../../../helpers/urls';
import './Footer.css';

export default function Footer() {
  return (
    <footer className="sc-footer">
      <div className="sc-footer-limited">
        <nav className="sc-footer-nav">
          <div className="sc-footer-nav-column">
            <h4 className="sc-footer-nav-column-title">Need Help</h4>
            <ul>
              <li className="spacer-top">
                <a
                  className="sc-footer-link"
                  href="https://community.sonarsource.com/c/help/sc"
                  rel="noopener noreferrer"
                  target="_blank">
                  Support Forum
                </a>
              </li>
              <li className="spacer-top">
                <Link className="sc-footer-link" to="/about/contact">
                  Contact Us
                </Link>
              </li>
              <li className="spacer-top">
                <a
                  className="sc-footer-link"
                  href="https://sonarcloud.statuspage.io/"
                  rel="noopener noreferrer"
                  target="_blank">
                  Status
                </a>
              </li>
            </ul>
          </div>
          <div className="sc-footer-nav-column">
            <h4 className="sc-footer-nav-column-title">News</h4>
            <ul>
              <li className="spacer-top">
                <a
                  className="sc-footer-link"
                  href="https://blog.sonarsource.com/product/SonarCloud"
                  rel="noopener noreferrer"
                  target="_blank">
                  SonarCloud News
                </a>
              </li>
              <li className="spacer-top">
                <a
                  className="sc-footer-link"
                  href="https://twitter.com/sonarcloud"
                  rel="noopener noreferrer"
                  target="_blank">
                  Twitter
                </a>
              </li>
            </ul>
          </div>
          <div className="sc-footer-nav-column">
            <h4 className="sc-footer-nav-column-title">About</h4>
            <ul>
              <li className="spacer-top">
                <Link
                  className="sc-footer-link"
                  rel="noopener noreferrer"
                  target="_blank"
                  to="/terms.pdf">
                  Terms
                </Link>
              </li>
              <li className="spacer-top">
                <Link className="sc-footer-link" to="/about/pricing/">
                  Pricing
                </Link>
              </li>
              <li className="spacer-top">
                <Link className="sc-footer-link" to="/documentation/privacy/">
                  Privacy
                </Link>
              </li>
              <li className="spacer-top">
                <Link className="sc-footer-link" to="/documentation/security/">
                  Security
                </Link>
              </li>
            </ul>
          </div>
        </nav>

        <div className="sc-footer-logo">
          <Link className="display-inline-block link-no-underline" to="/">
            <img alt="SonarCloud" height="45" src={`${getBaseUrl()}/images/sonarcloud-logo.svg`} />
          </Link>
          <div>
            <a
              className="sc-footer-link"
              href="https://www.sonarsource.com"
              rel="noopener noreferrer"
              target="_blank">
              A SonarSource™ product
            </a>
          </div>
        </div>
      </div>

      <div className="sc-footer-copy">
        <div className="sc-footer-limited">
          © 2008-2019, SonarCloud by{' '}
          <a
            className="sc-footer-link sc-footer-copy-link"
            href="https://www.sonarsource.com"
            rel="noopener noreferrer"
            target="_blank">
            SonarSource SA
          </a>
          . All rights reserved. SonarCloud is a service operated by{' '}
          <a
            className="sc-footer-link sc-footer-copy-link"
            href="https://www.sonarsource.com"
            rel="noopener noreferrer"
            target="_blank">
            SonarSource
          </a>
          , the company that develops and promotes open source{' '}
          <a
            className="sc-footer-link sc-footer-copy-link"
            href="http://sonarqube.org"
            rel="noopener noreferrer"
            target="_blank">
            SonarQube
          </a>{' '}
          and{' '}
          <a
            className="sc-footer-link sc-footer-copy-link"
            href="http://sonarlint.org"
            rel="noopener noreferrer"
            target="_blank">
            SonarLint
          </a>
          .
        </div>
      </div>
    </footer>
  );
}
