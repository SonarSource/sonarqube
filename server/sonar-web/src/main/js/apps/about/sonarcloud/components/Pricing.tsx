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

export default function Pricing() {
  return (
    <div className="sc-pricing sc-narrow-container">
      <div className="sc-pricing-block">
        <h3 className="sc-pricing-title">Open Source Projects</h3>
        <span className="sc-pricing-small">&nbsp;</span>
        <span className="sc-pricing-price">Free</span>
      </div>

      <div className="sc-pricing-block">
        <h3 className="sc-pricing-title">Private Projects</h3>
        <span className="sc-pricing-small">14 days free trial</span>
        <strong>
          From <span className="sc-pricing-price">10€</span>
          /mo
        </strong>
        <Link className="sc-news-link" to="/about/pricing/">
          see prices
        </Link>
      </div>
    </div>
  );
}
