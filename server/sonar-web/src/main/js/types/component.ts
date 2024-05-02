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
import { isPortfolioLike } from '~sonar-aligned/helpers/component';
import { ComponentQualifier, LightComponent, Visibility } from '~sonar-aligned/types/component';
import { Task } from './tasks';
import { Component } from './types';

export enum ProjectKeyValidationResult {
  Valid = 'valid',
  Empty = 'empty',
  TooLong = 'too_long',
  InvalidChar = 'invalid_char',
  OnlyDigits = 'only_digits',
}

export interface TreeComponent extends LightComponent {
  id?: string;
  path?: string;
  refId?: string;
  refKey?: string;
  tags?: string[];
  visibility: Visibility;
}

export interface TreeComponentWithPath extends TreeComponent {
  path: string;
}

export function isApplication(
  componentQualifier?: string | ComponentQualifier,
): componentQualifier is ComponentQualifier.Application {
  return componentQualifier === ComponentQualifier.Application;
}

export function isProject(
  componentQualifier?: string | ComponentQualifier,
): componentQualifier is ComponentQualifier.Project {
  return componentQualifier === ComponentQualifier.Project;
}

export function isFile(
  componentQualifier?: string | ComponentQualifier,
): componentQualifier is ComponentQualifier.File {
  return [ComponentQualifier.File, ComponentQualifier.TestFile].includes(
    componentQualifier as ComponentQualifier,
  );
}

export function isView(
  componentQualifier?: string | ComponentQualifier,
): componentQualifier is
  | ComponentQualifier.Application
  | ComponentQualifier.Portfolio
  | ComponentQualifier.SubPortfolio {
  return isPortfolioLike(componentQualifier) || isApplication(componentQualifier);
}

export interface ComponentContextShape {
  component?: Component;
  currentTask?: Task;
  isInProgress?: boolean;
  isPending?: boolean;
  onComponentChange: (changes: Partial<Component>) => void;
  fetchComponent: () => Promise<void>;
}
