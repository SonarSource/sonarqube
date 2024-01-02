/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import classNames from 'classnames';
import { BasicSeparator, SubTitle } from 'design-system';
import React, { PropsWithChildren, useEffect, useState } from 'react';
import { getProjectLinks } from '../../../api/projectLinks';
import { translate } from '../../../helpers/l10n';
import { ComponentQualifier, Visibility } from '../../../types/component';
import { Component, Measure, ProjectLink } from '../../../types/types';
import MetaDescription from './components/MetaDescription';
import MetaKey from './components/MetaKey';
import MetaLinks from './components/MetaLinks';
import MetaQualityGate from './components/MetaQualityGate';
import MetaQualityProfiles from './components/MetaQualityProfiles';
import MetaSize from './components/MetaSize';
import MetaTags from './components/MetaTags';
import MetaVisibility from './components/MetaVisibility';

export interface AboutProjectProps {
  component: Component;
  measures?: Measure[];
  onComponentChange: (changes: {}) => void;
}

export default function AboutProject(props: AboutProjectProps) {
  const { component, measures = [] } = props;
  const isApp = component.qualifier === ComponentQualifier.Application;
  const [links, setLinks] = useState<ProjectLink[] | undefined>(undefined);

  useEffect(() => {
    if (!isApp) {
      getProjectLinks(component.key).then(
        (links) => setLinks(links),
        () => {},
      );
    }
  }, [component.key, isApp]);

  return (
    <>
      <div>
        <SubTitle>{translate(isApp ? 'application' : 'project', 'about.title')}</SubTitle>
      </div>

      {!isApp &&
        (component.qualityGate ||
          (component.qualityProfiles && component.qualityProfiles.length > 0)) && (
          <ProjectInformationSection className="sw-pt-0">
            {component.qualityGate && <MetaQualityGate qualityGate={component.qualityGate} />}

            {component.qualityProfiles && component.qualityProfiles.length > 0 && (
              <MetaQualityProfiles
                headerClassName={component.qualityGate ? 'big-spacer-top' : undefined}
                profiles={component.qualityProfiles}
              />
            )}
          </ProjectInformationSection>
        )}

      <ProjectInformationSection>
        <MetaKey componentKey={component.key} qualifier={component.qualifier} />
      </ProjectInformationSection>

      <ProjectInformationSection>
        <MetaVisibility
          qualifier={component.qualifier}
          visibility={component.visibility ?? Visibility.Public}
        />
      </ProjectInformationSection>

      <ProjectInformationSection>
        <MetaDescription description={component.description} isApp={isApp} />
      </ProjectInformationSection>

      <ProjectInformationSection>
        <MetaTags component={component} onComponentChange={props.onComponentChange} />
      </ProjectInformationSection>

      <ProjectInformationSection last={isApp || !links?.length}>
        <MetaSize component={component} measures={measures} />
      </ProjectInformationSection>

      {!isApp && links && links.length > 0 && (
        <ProjectInformationSection last>
          <MetaLinks links={links} />
        </ProjectInformationSection>
      )}
    </>
  );
}

interface ProjectInformationSectionProps {
  last?: boolean;
  className?: string;
}

function ProjectInformationSection(props: PropsWithChildren<ProjectInformationSectionProps>) {
  const { children, className, last = false } = props;
  return (
    <>
      <div className={classNames('sw-py-4', className)}>{children}</div>
      {!last && <BasicSeparator />}
    </>
  );
}
