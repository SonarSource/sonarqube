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

export default function Footer() {
  return (
    <div className="page-footer">
      <a
        href="https://creativecommons.org/licenses/by-nc/3.0/us/"
        rel="noopener noreferrer"
        target="_blank"
        title="Creative Commons License">
        <img
          alt="Creative Commons License"
          src="https://licensebuttons.net/l/by-nc/3.0/us/88x31.png"
        />
      </a>
      © 2008-2019, SonarSource S.A, Switzerland. Except where otherwise noted, content in this space
      is licensed under a{' '}
      <a
        href="https://creativecommons.org/licenses/by-nc/3.0/us/"
        rel="noopener noreferrer"
        target="_blank">
        Creative Commons Attribution-NonCommercial 3.0 United States License.
      </a>{' '}
      SONARQUBE is a trademark of SonarSource SA. All other trademarks and copyrights are the
      property of their respective owners.
    </div>
  );
}
