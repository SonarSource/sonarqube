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
import { CodeSnippet, NumberedListItem } from 'design-system';
import * as React from 'react';
import { Component } from '../../../types/types';
import SentenceWithFilename from './SentenceWithFilename';

export interface DefaultProjectKeyProps {
  component: Component;
  monorepo?: boolean;
}

const sonarProjectSnippet = (key: string) => `sonar.projectKey=${key}`;

export default function DefaultProjectKey(props: DefaultProjectKeyProps) {
  const { component, monorepo } = props;

  return (
    <NumberedListItem>
      <SentenceWithFilename
        filename="sonar-project.properties"
        translationKey={`onboarding.tutorial.other.project_key${monorepo ? '.monorepo' : ''}`}
      />
      <CodeSnippet snippet={sonarProjectSnippet(component.key)} isOneLine className="sw-p-6" />
    </NumberedListItem>
  );
}
