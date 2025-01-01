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

export enum TutorialModes {
  Local = 'local',
  Jenkins = 'jenkins',
  BitbucketPipelines = 'bitbucket-pipelines',
  GitLabCI = 'gitlab-ci',
  GitHubActions = 'github-actions',
  AzurePipelines = 'azure-pipelines',
  OtherCI = 'other-ci',
}

export enum BuildTools {
  Maven = 'maven',
  Gradle = 'gradle',
  Cpp = 'cpp',
  ObjectiveC = 'objectivec',
  DotNet = 'dotnet',
  Dart = 'dart',
  Other = 'other',
}

export enum GradleBuildDSL {
  Groovy = 'build.gradle',
  Kotlin = 'build.gradle.kts',
}

export enum OSs {
  Linux = 'linux',
  Windows = 'win',
  MacOS = 'mac',
}

export enum Arch {
  X86_64 = 'x86_64',
  Arm64 = 'arm64',
}

export enum AutoConfig {
  Automatic = 'automatic',
  Manual = 'manual',
}

export type TutorialConfig = {
  autoConfig?: AutoConfig;
  buildTool?: BuildTools;
};
