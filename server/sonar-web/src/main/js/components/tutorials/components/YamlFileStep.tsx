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
import { NumberedList, NumberedListItem } from 'design-system';
import * as React from 'react';
import { withCLanguageFeature } from '../../hoc/withCLanguageFeature';
import { TutorialConfig, TutorialModes } from '../types';
import BuildConfigSelection from './BuildConfigSelection';

export interface YamlFileStepProps {
  children?: (config: TutorialConfig) => React.ReactElement<{}>;
  ci: TutorialModes;
  config: TutorialConfig;
  hasCLanguageFeature: boolean;
  setConfig: (config: TutorialConfig) => void;
}

export function YamlFileStep(props: YamlFileStepProps) {
  const { ci, config, setConfig, children, hasCLanguageFeature } = props;

  return (
    <NumberedList>
      <NumberedListItem>
        <BuildConfigSelection
          ci={ci}
          config={config}
          onSetConfig={setConfig}
          supportCFamily={hasCLanguageFeature}
        />
      </NumberedListItem>

      {children && config && children(config)}
    </NumberedList>
  );
}

export default withCLanguageFeature(YamlFileStep);
