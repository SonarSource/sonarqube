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
  const content = fs.readFileSync(fileNode.absolutePath, 'utf-8');
  return new Promise((resolve, reject) => {
    let newContent = cutSonarCloudContent(content);
    newContent = removeRemainingContentTags(newContent);
    resolve(newContent);
  });
}

function removeRemainingContentTags(content) {
  const regexBase = '<!-- \\/?(sonarqube|sonarcloud|static) -->';
  return content
    .replace(new RegExp(`^${regexBase}(\n|\r|\r\n|$)`, 'gm'), '') // First, remove single-line ones, including ending carriage-returns.
    .replace(new RegExp(`${regexBase}`, 'g'), ''); // Now remove all remaining ones.
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

exports.createFilePath = createFilePath;
exports.createRemoteFileNode = createRemoteFileNode;
exports.loadNodeContent = loadNodeContent;
