/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
const { createFilePath, createRemoteFileNode } = require('gatsby-source-filesystem');
const fs = require('fs-extra');
const path = require('path');

const documentationFileName = 'documentation.md';
const manifestFileName = 'manifest.mf';
const manifestIssueTrackerUrlSection = 'Plugin-IssueTrackerUrl';

let overrides;

function processPluginOverridesIfAvailable(content) {
  if (overrides === undefined) {
    const pluginFileList = getPluginFilesList();

    overrides = pluginFileList
      .map(fileList => processPluginFileList(fileList))
      .filter(res => !!res)
      .map(addIssueTrackerLink)
      .reduce((prev, cur) => {
        prev[cur.url] = cur.content;
        return prev;
      }, {});
  }

  const match = content.match(/^url\s*:\s*(.+)$/m);
  if (match && match[1] && overrides[match[1]]) {
    return overrides[match[1]];
  }

  return content;
}

function addIssueTrackerLink(page) {
  let issueTrackerLink = '## Issue Tracker';
  issueTrackerLink += '\r\n';
  issueTrackerLink += `Check the [issue tracker](${page.issueTrackerUrl}) for this language.`;

  page.content = `${page.content}\r\n${issueTrackerLink}`;

  return page;
}

function getPluginFilesList() {
  const dir = path.normalize(`${__dirname}/../../build/tmp/plugin-documentation/`);

  if (fs.pathExistsSync(dir)) {
    return fs.readdirSync(dir).map(subDir => {
      const pluginDir = path.normalize(`${dir}/${subDir}`);

      if (fs.pathExistsSync(pluginDir)) {
        return fs
          .readdirSync(pluginDir)
          .map(fileName => path.normalize(`${pluginDir}/${fileName}`));
      }

      return [];
    });
  }

  return [];
}

function processPluginFileList(pluginFileList) {
  let md;
  let mf;

  pluginFileList.forEach(fileFullName => {
    const fileName = path.basename(fileFullName);

    if (fileName.toLowerCase() === documentationFileName.toLowerCase()) {
      md = fileFullName;
    } else if (fileName.toLowerCase() === manifestFileName.toLowerCase()) {
      mf = fileFullName;
    }
  });

  if (!md) {
    return undefined;
  }

  return { ...parsePluginMarkdownFile(md), ...parsePluginManifestFile(mf) };
}

function parsePluginMarkdownFile(fileFullPath) {
  let mdContent = fs.readFileSync(fileFullPath, 'utf-8');
  const regex = /^key\s*:\s*(.+)$/m;
  const match = mdContent.match(regex);

  if (match && match[1]) {
    const url = `/analysis/languages/${match[1]}/`;
    mdContent = mdContent.replace(regex, `url: ${url}\n`);
    return { url, content: mdContent };
  }

  return undefined;
}

function parsePluginManifestFile(fileFullPath) {
  const mfContent = fs.readFileSync(fileFullPath, 'utf-8');
  const regex = /^Plugin-IssueTrackerUrl:\s*(.+\r?\n?\s?\w+)$/m;
  const match = mfContent.match(regex);

  if (match && match[1]) {
    // Manifest value might be split on many lines and contains space => get rid of it
    const issueTrackerUrl = match[1].replace(/\r\n\s/m, '');
    return { issueTrackerUrl };
  }

  return undefined;
}

function loadNodeContent(fileNode) {
  return Promise.resolve(loadNodeContentSync(fileNode));
}

function loadNodeContentSync(fileNode) {
  let content = processPluginOverridesIfAvailable(fs.readFileSync(fileNode.absolutePath, 'utf-8'));

  content = cleanContent(content);
  content = handleIncludes(content, fileNode);

  return content;
}

function cleanContent(content) {
  content = cutAdditionalContent(content, 'sonarcloud');
  content = cutAdditionalContent(content, 'embedded');
  content = removeRemainingContentTags(content);
  content = replaceInstanceTag(content);

  return content;
}

function removeRemainingContentTags(content) {
  const regexBase = '<!-- \\/?(sonarqube|sonarcloud|static|embedded) -->';
  return content
    .replace(new RegExp(`^${regexBase}(\n|\r|\r\n|$)`, 'gm'), '')
    .replace(new RegExp(`${regexBase}`, 'g'), '');
}

function cutAdditionalContent(content, tag) {
  const beginning = '<!-- ' + tag + ' -->';
  const ending = '<!-- /' + tag + ' -->';

  let newContent = content;
  let start = newContent.indexOf(beginning);
  let end = newContent.indexOf(ending);
  while (start !== -1 && end !== -1) {
    newContent = newContent.substring(0, start) + newContent.substring(end + ending.length);
    start = newContent.indexOf(beginning);
    end = newContent.indexOf(ending);
  }

  return newContent;
}

function handleIncludes(content, fileNode) {
  return content.replace(/@include (.*)/g, (_, path) => {
    const relativePath = `${path}.md`;
    const absolutePath = `${__dirname}/../../src/${relativePath}`;

    if (relativePath === fileNode.relativePath) {
      throw new Error(`Error in ${fileNode.relativePath}: The file is trying to include itself.`);
    } else if (!fs.existsSync(absolutePath)) {
      throw new Error(
        `Error in ${fileNode.relativePath}: Couldn't load "${relativePath}" for inclusion.`
      );
    } else {
      const fileContent = loadNodeContentSync({ absolutePath, relativePath });
      return fileContent.replace(/^---[\w\W]+?---$/m, '').trim();
    }
  });
}

function replaceInstanceTag(content) {
  return content.replace(/{instance}/gi, 'SonarQube');
}

exports.createFilePath = createFilePath;
exports.createRemoteFileNode = createRemoteFileNode;
exports.loadNodeContent = loadNodeContent;
exports.cleanContent = cleanContent;
exports.cutAdditionalContent = cutAdditionalContent;
