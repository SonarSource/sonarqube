/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import PrivacyBadgeContainer from '../../../components/common/PrivacyBadgeContainer';
import { translate } from '../../../helpers/l10n';
import { ComponentQualifier } from '../../../types/component';
import { Component, Measure } from '../../../types/types';
import MetaKey from '../meta/MetaKey';
import MetaLinks from '../meta/MetaLinks';
import MetaQualityGate from '../meta/MetaQualityGate';
import MetaQualityProfiles from '../meta/MetaQualityProfiles';
import MetaSize from '../meta/MetaSize';
import MetaTags from '../meta/MetaTags';

export interface AboutProjectProps {
  component: Component;
  measures?: Measure[];
  onComponentChange: (changes: {}) => void;
}

export function AboutProject(props: AboutProjectProps) {
  const { component, measures = [] } = props;

  const heading = React.useRef<HTMLHeadingElement>(null);
  const isApp = component.qualifier === ComponentQualifier.Application;

  React.useEffect(() => {
    if (heading.current) {
      // a11y: provide focus to the heading when the Project Information is opened.
      heading.current.focus();
    }
  }, [heading]);

  return (
    <>
      <div>
        <h2 className="big-padded bordered-bottom" tabIndex={-1} ref={heading}>
          {translate(isApp ? 'application' : 'project', 'info.title')}
        </h2>
      </div>

      <div className="overflow-y-auto">
        <div className="big-padded bordered-bottom">
          <div className="display-flex-center">
            <h3 className="spacer-right">{translate('project.info.description')}</h3>
            {component.visibility && (
              <PrivacyBadgeContainer
                qualifier={component.qualifier}
                visibility={component.visibility}
              />
            )}
          </div>

          {component.description && (
            <p className="it__project-description">{component.description}</p>
          )}

          <MetaTags component={component} onComponentChange={props.onComponentChange} />
        </div>

        <div className="big-padded bordered-bottom it__project-loc-value">
          <MetaSize component={component} measures={measures} />
        </div>

        {!isApp &&
          (component.qualityGate ||
            (component.qualityProfiles && component.qualityProfiles.length > 0)) && (
            <div className="big-padded bordered-bottom">
              {component.qualityGate && <MetaQualityGate qualityGate={component.qualityGate} />}

              {component.qualityProfiles && component.qualityProfiles.length > 0 && (
                <MetaQualityProfiles
                  headerClassName={component.qualityGate ? 'big-spacer-top' : undefined}
                  profiles={component.qualityProfiles}
                />
              )}
            </div>
          )}

        {!isApp && <MetaLinks component={component} />}

        <div className="big-padded bordered-bottom">
          <MetaKey componentKey={component.key} qualifier={component.qualifier} />
        </div>
      </div>
    </>
  );
}

export default AboutProject;
