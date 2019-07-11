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
import BugIcon from 'sonar-ui-common/components/icons/BugIcon';
import CodeSmellIcon from 'sonar-ui-common/components/icons/CodeSmellIcon';
import SecurityHotspotIcon from 'sonar-ui-common/components/icons/SecurityHotspotIcon';
import VulnerabilityIcon from 'sonar-ui-common/components/icons/VulnerabilityIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';

export default function AboutQualityModel() {
  return (
    <div className="boxed-group about-quality-model">
      <h2>{translate('about_page.quality_model')}</h2>

      <div className="boxed-group-inner clearfix">
        <div className="flex-columns">
          <div className="flex-column flex-column-third">
            <h3>{translate('metric_domain.Reliability')}</h3>
            <div className="pull-left little-spacer-right">
              <BugIcon />
            </div>
            <p className="about-page-text overflow-hidden">
              <strong>{translate('issue.type.BUG.plural')}</strong>{' '}
              {translate('about_page.quality_model.bugs')}
            </p>
          </div>

          <div className="flex-column flex-column-third">
            <h3>{translate('metric_domain.Security')}</h3>
            <div className="pull-left little-spacer-right">
              <VulnerabilityIcon />
            </div>
            <p className="about-page-text overflow-hidden">
              <strong>{translate('issue.type.VULNERABILITY.plural')}</strong>{' '}
              {translate('about_page.quality_model.vulnerabilities')}
            </p>
            <br />
            <div className="pull-left little-spacer-right">
              <SecurityHotspotIcon />
            </div>
            <p className="about-page-text overflow-hidden">
              <strong>{translate('issue.type.SECURITY_HOTSPOT.plural')}</strong>{' '}
              {translate('about_page.quality_model.security_hotspots')}
            </p>
          </div>

          <div className="flex-column flex-column-third">
            <h3>{translate('metric_domain.Maintainability')}</h3>
            <div className="pull-left little-spacer-right">
              <CodeSmellIcon />
            </div>
            <p className="about-page-text overflow-hidden">
              <strong>{translate('issue.type.CODE_SMELL.plural')}</strong>{' '}
              {translate('about_page.quality_model.code_smells')}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
