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

import MetaKey from './MetaKey';
import MetaLinks from './MetaLinks';
import MetaQualityGate from './MetaQualityGate';
import MetaQualityProfiles from './MetaQualityProfiles';
import EventsList from './../events/EventsList';

const Meta = ({ component }) => {
  const { qualifier, description, profiles, gate } = component;

  const isProject = qualifier === 'TRK';
  const isView = qualifier === 'VW' || qualifier === 'SVW';
  const isDeveloper = qualifier === 'DEV';

  const hasDescription = !!description;
  const hasQualityProfiles = Array.isArray(profiles) && profiles.length > 0;
  const hasQualityGate = !!gate;

  const shouldShowQualityProfiles = !isView && !isDeveloper && hasQualityProfiles;
  const shouldShowQualityGate = !isView && !isDeveloper && hasQualityGate;

  const showShowEvents = isProject || isView || isDeveloper;

  return (
      <div className="overview-meta">
        <div className="overview-meta-card">
          {hasDescription && (
              <div className="overview-meta-description big-spacer-bottom">
                {description}
              </div>
          )}

          <MetaLinks component={component}/>

          <MetaKey component={component}/>
        </div>

        {(shouldShowQualityProfiles || shouldShowQualityGate) && (
            <div className="overview-meta-card">
              {hasQualityGate && (
                  <MetaQualityGate gate={gate}/>
              )}

              {shouldShowQualityProfiles && (
                  <MetaQualityProfiles profiles={profiles}/>
              )}
            </div>
        )}

        {showShowEvents && (
            <EventsList component={component}/>
        )}
      </div>
  );
};

export default Meta;
