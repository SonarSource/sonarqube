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
import { ProjectAlmBindingResponse } from './alm-settings';
import { BranchLike } from './branch-like';
import { Component, LightComponent, Organization } from './types';

export enum Visibility {
  Public = 'public',
  Private = 'private',
}

export enum ComponentQualifier {
  Application = 'APP',
  Directory = 'DIR',
  Developper = 'DEV',
  File = 'FIL',
  Portfolio = 'VW',
  Project = 'TRK',
  SubPortfolio = 'SVW',
  TestFile = 'UTS',
}

export enum ProjectKeyValidationResult {
  Valid = 'valid',
  Empty = 'empty',
  TooLong = 'too_long',
  InvalidChar = 'invalid_char',
  OnlyDigits = 'only_digits',
}

export interface TreeComponent extends LightComponent {
  id?: string;
  name: string;
  path?: string;
  refId?: string;
  refKey?: string;
  tags?: string[];
  visibility: Visibility;
}

export interface TreeComponentWithPath extends TreeComponent {
  path: string;
}

export function isPortfolioLike(
  componentQualifier?: string | ComponentQualifier
): componentQualifier is ComponentQualifier.Portfolio | ComponentQualifier.SubPortfolio {
  return Boolean(
    componentQualifier &&
      [
        ComponentQualifier.Portfolio.toString(),
        ComponentQualifier.SubPortfolio.toString(),
      ].includes(componentQualifier)
  );
}

export function isApplication(
  componentQualifier?: string | ComponentQualifier
): componentQualifier is ComponentQualifier.Application {
  return componentQualifier === ComponentQualifier.Application;
}

export function isProject(
  componentQualifier?: string | ComponentQualifier
): componentQualifier is ComponentQualifier.Project {
  return componentQualifier === ComponentQualifier.Project;
}

export function isFile(componentQualifier?: string | ComponentQualifier): boolean {
  return [ComponentQualifier.File, ComponentQualifier.TestFile].includes(
    componentQualifier as ComponentQualifier
  );
}

export function isView(componentQualifier?: string | ComponentQualifier): boolean {
  return [
    ComponentQualifier.Portfolio,
    ComponentQualifier.SubPortfolio,
    ComponentQualifier.Application,
  ].includes(componentQualifier as ComponentQualifier);
}

export interface ComponentContextShape {
  branchLike?: BranchLike;
  branchLikes: BranchLike[];
  component?: Component;
  isInProgress?: boolean;
  isPending?: boolean;
  onBranchesChange: (updateBranches?: boolean, updatePRs?: boolean) => void;
  onComponentChange: (changes: Partial<Component>) => void;
  projectBinding?: ProjectAlmBindingResponse;
  organization?: Organization;
}
