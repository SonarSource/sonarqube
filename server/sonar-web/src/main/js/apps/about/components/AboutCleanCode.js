/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import DropImage from './DropImage';

export default class AboutCleanCode extends React.Component {
  render () {
    return (
        <div className="about-page-section">
          <div className="about-page-center-container">
            <h2 className="about-page-header">Keep your code clean by fixing the leak</h2>
            <p className="about-page-text about-page-text-center">
              By fixing new issues as they appear in code, you create and maintain a clean code base.
              <br/>
              Even on legacy projects, focusing on keeping new code clean will eventually yield a code base you can be
              proud of.
            </p>
            <div className="about-page-section-image">
              <DropImage/>
            </div>
          </div>
        </div>
    );
  }
}
