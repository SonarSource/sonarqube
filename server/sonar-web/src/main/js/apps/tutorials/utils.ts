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
export interface LanguageConfig {
  language?: string;
  javaBuild?: string;
  cFamilyCompiler?: string;
  os?: string;
  projectKey?: string;
}

export interface StepProps {
  component?: T.Component;
  finished?: boolean;
  hasStepAfter?: (hasStepAfter: boolean) => void;
  onContinue: VoidFunction;
  onOpen: VoidFunction;
  open: boolean;
  organization?: string;
  stepNumber: number;
  token?: string;
}

export function isLanguageConfigured(config?: LanguageConfig) {
  if (!config) {
    return false;
  }
  const { language, javaBuild, cFamilyCompiler, os, projectKey } = config;
  const isJavaConfigured = language === 'java' && javaBuild != null;
  const isDotNetConfigured = language === 'dotnet' && projectKey != null;
  const isCFamilyConfigured =
    language === 'c-family' && (cFamilyCompiler === 'msvc' || os != null) && projectKey != null;
  const isOtherConfigured = language === 'other' && projectKey != null;

  return isJavaConfigured || isDotNetConfigured || isCFamilyConfigured || isOtherConfigured;
}

export function quote(os: string): (s: string) => string {
  return os === 'win' ? (s: string) => `"${s}"` : (s: string) => s;
}

export function getUniqueTokenName(tokens: T.UserToken[], initialTokenName = '') {
  const hasToken = (name: string) => tokens.find(token => token.name === name) !== undefined;

  if (!hasToken(initialTokenName)) {
    return initialTokenName;
  }

  let i = 1;
  while (hasToken(`${initialTokenName} ${i}`)) {
    i++;
  }
  return `${initialTokenName} ${i}`;
}
