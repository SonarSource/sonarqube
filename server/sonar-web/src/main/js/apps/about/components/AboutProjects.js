/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import { Link } from 'react-router';
import { formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';

export default class AboutProjects extends React.Component {
  static propTypes = {
    count: React.PropTypes.number.isRequired
  };

  render() {
    return (
      <div className="about-page-projects">
        <div>
          <div>
            <Link to="/projects" className="about-page-projects-link">
              {formatMeasure(this.props.count, 'INT')}
            </Link>
          </div>
          <div>
            {translate('about_page.projects_analyzed')}
          </div>
        </div>
      </div>
    );
  }
}
