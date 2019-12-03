/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as classNames from 'classnames';
import { groupBy } from 'lodash';
import * as React from 'react';
import SecurityHotspotIcon from 'sonar-ui-common/components/icons/SecurityHotspotIcon';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { RawHotspot, RiskExposure } from '../../../types/securityHotspots';
import { groupByCategory, RISK_EXPOSURE_LEVELS } from '../utils';
import HotspotCategory from './HotspotCategory';
import './HotspotList.css';

export interface HotspotListProps {
  hotspots: RawHotspot[];
  onHotspotClick: (key: string) => void;
  securityCategories: T.Dict<{ title: string; description?: string }>;
  selectedHotspotKey: string | undefined;
}

export default function HotspotList(props: HotspotListProps) {
  const { hotspots, securityCategories, selectedHotspotKey } = props;

  const groupedHotspots: Array<{
    risk: RiskExposure;
    categories: Array<{ key: string; hotspots: RawHotspot[]; title: string }>;
  }> = React.useMemo(() => {
    const risks = groupBy(hotspots, h => h.vulnerabilityProbability);

    return RISK_EXPOSURE_LEVELS.map(risk => ({
      risk,
      categories: groupByCategory(risks[risk], securityCategories)
    })).filter(risk => risk.categories.length > 0);
  }, [hotspots, securityCategories]);

  return (
    <>
      <h1 className="hotspot-list-header bordered-bottom">
        <SecurityHotspotIcon className="spacer-right" />
        {translateWithParameters(`hotspots.list_title.TO_REVIEW`, hotspots.length)}
      </h1>
      <ul className="huge-spacer-bottom">
        {groupedHotspots.map(riskGroup => (
          <li className="big-spacer-bottom" key={riskGroup.risk}>
            <div className="hotspot-risk-header little-spacer-left">
              <span>{translate('hotspots.risk_exposure')}</span>
              <div className={classNames('hotspot-risk-badge', 'spacer-left', riskGroup.risk)}>
                {translate('risk_exposure', riskGroup.risk)}
              </div>
            </div>
            <ul>
              {riskGroup.categories.map(cat => (
                <li className="spacer-bottom" key={cat.key}>
                  <HotspotCategory
                    category={{ key: cat.key, title: cat.title }}
                    hotspots={cat.hotspots}
                    onHotspotClick={props.onHotspotClick}
                    selectedHotspotKey={selectedHotspotKey}
                  />
                </li>
              ))}
            </ul>
          </li>
        ))}
      </ul>
    </>
  );
}
