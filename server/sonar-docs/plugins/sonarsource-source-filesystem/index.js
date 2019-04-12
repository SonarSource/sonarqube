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
const { createFilePath, createRemoteFileNode } = require('gatsby-source-filesystem');
const fs = require('fs-extra');

function loadNodeContent(fileNode) {
  return Promise.resolve(loadNodeContentSync(fileNode));
}

function loadNodeContentSync(fileNode) {
  const content = fs.readFileSync(fileNode.absolutePath, 'utf-8');
  let newContent = cutSonarCloudContent(content);
  newContent = removeRemainingContentTags(newContent);
  newContent = handleIncludes(newContent, fileNode);
  newContent = replaceInstanceTag(newContent);
  return newContent;
}

function removeRemainingContentTags(content) {
  const regexBase = '<!-- \\/?(sonarqube|sonarcloud|static) -->';
  return content
    .replace(new RegExp(`^${regexBase}(\n|\r|\r\n|$)`, 'gm'), '')
    .replace(new RegExp(`${regexBase}`, 'g'), '');
}

function cutSonarCloudContent(content) {
  const beginning = '<!-- sonarcloud -->';
  const ending = '<!-- /sonarcloud -->';

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
