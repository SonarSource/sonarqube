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
import { GRADLE_SCANNER_VERSION } from '../../helpers/constants';
import { convertGithubApiUrlToLink, stripTrailingSlash } from '../../helpers/urls';
import { AlmSettingsInstance, ProjectAlmBindingResponse } from '../../types/alm-settings';
import { UserToken } from '../../types/token';
import { Arch, AutoConfig, BuildTools, GradleBuildDSL, OSs, TutorialConfig } from './types';

export const SONAR_SCANNER_CLI_LATEST_VERSION = '6.2.0.4584';

export function quote(os: string): (s: string) => string {
  return os === 'win' ? (s: string) => `"${s}"` : (s: string) => s;
}

export function buildGradleSnippet(key: string, name: string, build: GradleBuildDSL) {
  const map = {
    [GradleBuildDSL.Groovy]: `plugins {
  id "org.sonarqube" version "${GRADLE_SCANNER_VERSION}"
}

sonar {
  properties {
    property "sonar.projectKey", "${key}"
    property "sonar.projectName", "${name}"
  }
}`,
    [GradleBuildDSL.Kotlin]: `plugins {
  id("org.sonarqube") version "${GRADLE_SCANNER_VERSION}"
}
    
sonar {
  properties {
    property("sonar.projectKey", "${key}")
    property("sonar.projectName", "${name}")
  }
}`,
  };
  return map[build];
}

export function getUniqueTokenName(tokens: UserToken[], initialTokenName: string) {
  const hasToken = (name: string) => tokens.find((token) => token.name === name) !== undefined;

  if (!hasToken(initialTokenName)) {
    return initialTokenName;
  }

  let i = 1;
  while (hasToken(`${initialTokenName} ${i}`)) {
    i++;
  }
  return `${initialTokenName} ${i}`;
}

export function buildGithubLink(
  almBinding: AlmSettingsInstance,
  projectBinding: ProjectAlmBindingResponse,
) {
  if (almBinding.url === undefined) {
    return null;
  }

  // strip the api path:
  const urlRoot = convertGithubApiUrlToLink(almBinding.url);

  return `${stripTrailingSlash(urlRoot)}/${projectBinding.repository}`;
}

export function buildBitbucketCloudLink(
  almBinding: AlmSettingsInstance,
  projectBinding: ProjectAlmBindingResponse,
) {
  if (almBinding.url === undefined || projectBinding.repository === undefined) {
    return null;
  }

  return `${stripTrailingSlash(almBinding.url)}/${projectBinding.repository}`;
}

export function supportsAutoConfig(buildTool: BuildTools) {
  return buildTool === BuildTools.Cpp;
}

export function getBuildToolOptions(supportCFamily: boolean) {
  const list = [BuildTools.Maven, BuildTools.Gradle, BuildTools.DotNet];
  if (supportCFamily) {
    list.push(BuildTools.Cpp);
    list.push(BuildTools.ObjectiveC);
    // Both Dart and CFamily are available in Developer Edition and above
    list.push(BuildTools.Dart);
  }
  list.push(BuildTools.Other);
  return list;
}

export function isCFamily(buildTool?: BuildTools) {
  return buildTool === BuildTools.Cpp || buildTool === BuildTools.ObjectiveC;
}

export function shouldShowGithubCFamilyExampleRepositories(config: TutorialConfig) {
  if (config.buildTool === BuildTools.Cpp && config.autoConfig === AutoConfig.Manual) {
    return true;
  }
  if (config.buildTool === BuildTools.ObjectiveC) {
    return true;
  }
  return false;
}

export function shouldShowOsSelector(config: TutorialConfig) {
  return (
    config.buildTool === BuildTools.Cpp ||
    config.buildTool === BuildTools.ObjectiveC ||
    config.buildTool === BuildTools.Dart ||
    config.buildTool === BuildTools.Other
  );
}

export function shouldShowArchSelector(
  os: OSs | undefined,
  config: TutorialConfig,
  scannerDownloadExplicit = false,
) {
  if (!shouldShowOsSelector(config)) {
    return false;
  }
  if (os !== OSs.Linux && os !== OSs.MacOS) {
    return false;
  }
  if (scannerDownloadExplicit) {
    return true;
  }
  if (!isCFamily(config.buildTool)) {
    return false;
  }
  if (config.buildTool === BuildTools.Cpp && config.autoConfig === AutoConfig.Automatic) {
    return false;
  }
  return true;
}

export function getBuildWrapperFolder(os: OSs, arch?: Arch) {
  if (os === OSs.Linux) {
    return arch === Arch.X86_64 ? 'build-wrapper-linux-x86' : 'build-wrapper-linux-aarch64';
  }
  if (os === OSs.MacOS) {
    return 'build-wrapper-macosx-x86';
  }
  if (os === OSs.Windows) {
    return 'build-wrapper-win-x86';
  }
  throw new Error(`Unsupported OS: ${os}`);
}

export function getBuildWrapperExecutable(os: OSs, arch?: Arch) {
  if (os === OSs.Linux) {
    return arch === Arch.X86_64 ? 'build-wrapper-linux-x86-64' : 'build-wrapper-linux-aarch64';
  }
  if (os === OSs.MacOS) {
    return 'build-wrapper-macosx-x86';
  }
  if (os === OSs.Windows) {
    return 'build-wrapper-win-x86-64.exe';
  }
  throw new Error(`Unsupported OS: ${os}`);
}

export function getBuildWrapperFolderLinux(arch?: Arch) {
  return getBuildWrapperFolder(OSs.Linux, arch);
}
export function getBuildWrapperExecutableLinux(arch?: Arch) {
  return getBuildWrapperExecutable(OSs.Linux, arch);
}

export function getScannerUrlSuffix(os: OSs, arch?: Arch) {
  if (os === OSs.Windows) {
    return '-windows-x64';
  }
  if (os === OSs.MacOS) {
    return '-macosx-' + (arch === Arch.Arm64 ? 'aarch64' : 'x64');
  }
  if (os === OSs.Linux) {
    return '-linux-' + (arch === Arch.Arm64 ? 'aarch64' : 'x64');
  }
  return '';
}

export function shouldFetchBuildWrapper(buildTool: BuildTools, autoConfig?: AutoConfig) {
  return (
    (buildTool === BuildTools.Cpp && autoConfig === AutoConfig.Manual) ||
    buildTool === BuildTools.ObjectiveC
  );
}
