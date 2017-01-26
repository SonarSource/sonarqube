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
import { translate } from '../../../helpers/l10n';
import BugIcon from '../../../components/ui/BugIcon';
import VulnerabilityIcon from '../../../components/ui/VulnerabilityIcon';
import CodeSmellIcon from '../../../components/ui/CodeSmellIcon';

export default class AboutIssues extends React.Component {
  render () {
    return (
        <div className="boxed-group">
          <h2>{translate('about_page.quality_model')}</h2>

          <div className="boxed-group-inner clearfix">
            <h3 className="spacer-bottom">
              <span className="little-spacer-right"><BugIcon/></span>
              {translate('issue.type.BUG.plural')}
            </h3>
            <p className="about-page-text">
              {translate('about_page.quality_model.bugs')}
            </p>

            <h3 className="big-spacer-top spacer-bottom">
              <span className="little-spacer-right"><VulnerabilityIcon/></span>
              {translate('issue.type.VULNERABILITY.plural')}
            </h3>
            <p className="about-page-text">
              {translate('about_page.quality_model.vulnerabilities')}
            </p>

            <h3 className="big-spacer-top spacer-bottom">
              <span className="little-spacer-right"><CodeSmellIcon/></span>
              {translate('issue.type.CODE_SMELL.plural')}
            </h3>
            <p className="about-page-text">
              {translate('about_page.quality_model.code_smells')}
            </p>
          </div>
        </div>
    );
  }
}
