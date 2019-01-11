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
const fs = require('fs');
const path = require('path');
const remark = require('remark');
const glob = require('glob-promise');
const visit = require('unist-util-visit');

const rootPath = path.resolve(path.join(__dirname, '/..'));
let files;
let parsedFiles;

beforeAll(async () => {
  files = await loadGlobFiles('/pages/**/*.md');
  parsedFiles = files.map(file => {
    return { ...separateFrontMatter(file.content), path: file.path };
  });
});

it('should have at least one instance of all possible frontmatter fields', () => {
  const pageWithTitle = parsedFiles.find(file => file.frontmatter.title !== undefined);
  const pageWithNav = parsedFiles.find(file => file.frontmatter.nav !== undefined);
  const pageWithUrl = parsedFiles.find(file => file.frontmatter.url !== undefined);
  expect(pageWithTitle).toBeDefined();
  expect(pageWithNav).toBeDefined();
  expect(pageWithUrl).toBeDefined();
});

/* eslint-disable no-console */
it('should have valid links in trees files', () => {
  const trees = [
    'SonarCloudNavigationTree.json',
    'SonarQubeNavigationTree.json',
    'StaticNavigationTree.json'
  ];
  let hasErrors = false;
  trees.forEach(file => {
    const tree = JSON.parse(fs.readFileSync(path.join(rootPath, '..', 'static', file), 'utf8'));
    tree.forEach(leaf => {
      if (typeof leaf === 'object') {
        if (leaf.children) {
          leaf.children.forEach(child => {
            // Check children markdown file path validity
            if (!urlExists(parsedFiles, child)) {
              console.log(`[${child}] is not a valid link, in ${file}`);
              hasErrors = true;
            }
          });
        }
      } else if (!urlExists(parsedFiles, leaf)) {
        // Check markdown file path validity
        console.log(`[${leaf}] is not a valid link, in ${file}`);
        hasErrors = true;
      }
    });
  });
  expect(hasErrors).toBeFalsy();
});

it('should have valid links in suggestions file', () => {
  const file = 'EmbedDocsSuggestions.json';
  const suggestions = JSON.parse(fs.readFileSync(path.join(rootPath, file), 'utf8'));
  let hasErrors = false;
  Object.keys(suggestions).forEach(key => {
    suggestions[key].forEach(suggestion => {
      if (!suggestion.link.startsWith('/documentation/')) {
        console.log(`[${suggestion.link}] should starts with "/documentation/", in ${file}`);
        hasErrors = true;
      } else if (!urlExists(parsedFiles, suggestion.link.replace('/documentation', ''))) {
        console.log(`[${suggestion.link}] is not a valid link, in ${file}`);
        hasErrors = true;
      }
    });
  });
  expect(hasErrors).toBeFalsy();
});

it('should have valid and uniq links in url metadata field', () => {
  let urlLists = [];
  let hasErrors = false;
  parsedFiles.forEach(file => {
    if (!file.frontmatter.url) {
      console.log(`[${file.path}] has no url metadata`);
      hasErrors = true;
    } else if (!checkUrlFormat(file.frontmatter.url, file.path)) {
      hasErrors = true;
    } else if (urlLists.includes(file.frontmatter.url)) {
      console.log(`[${file.path}] has an url that is not unique ${file.frontmatter.url}`);
      hasErrors = true;
    }

    urlLists = [...urlLists, file.frontmatter.url];
  });
  expect(hasErrors).toBeFalsy();
});

it('should have valid links pointing to documentation inside pages', () => {
  checkContentUrl(parsedFiles);
});

it('should have valid links inside tooltips', async () => {
  const files = await loadGlobFiles('/tooltips/**/*.md');
  checkContentUrl(files);
});

function handleIncludes(content, rootPath) {
  return content.replace(/@include (.+)/, (match, p) => {
    const filePath = path.join(rootPath, '..', `${p}.md`);
    return fs.readFileSync(filePath, 'utf8');
  });
}

function checkContentUrl(files) {
  let hasErrors = false;
  files.forEach(file => {
    visit(remark().parse(file.content), node => {
      if (node.type === 'image' && !node.url.startsWith('http')) {
        // Check image path validity
        if (!fs.existsSync(path.join(rootPath, node.url))) {
          console.log('[', node.url, '] is not a valid image path, in', file.path + '.md');
          hasErrors = true;
        }
      } else if (
        node.type === 'link' &&
        !node.url.startsWith('http') &&
        !node.url.startsWith('/#')
      ) {
        // Check markdown file path validity
        if (!urlExists(parsedFiles, node.url)) {
          console.log('[', node.url, '] is not a valid link, in', file.path + '.md');
          hasErrors = true;
        }
      }
    });
  });
  expect(hasErrors).toBeFalsy();
}

function urlExists(files, url) {
  return files.find(f => f.frontmatter.url === url) !== undefined;
}

function checkUrlFormat(url, file) {
  let noError = true;

  if (!url.startsWith('/')) {
    console.log('[', file, '] should starts with a slash', url);
    noError = false;
  }
  if (!url.endsWith('/')) {
    console.log('[', file, '] should ends with a slash', url);
    noError = false;
  }
  return noError;
}

function loadGlobFiles(globPath) {
  return glob(path.join(rootPath, globPath))
    .then(files => files.map(file => file.substr(rootPath.length + 1)))
    .then(files =>
      files.map(file => ({
        path: file.slice(0, -3),
        content: handleIncludes(fs.readFileSync(path.join(rootPath, file), 'utf8'), rootPath)
      }))
    );
}

function getFrontMatterPosition(lines) {
  let firstLine;
  let lastLine;
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (line.trim() === '---') {
      if (firstLine === undefined) {
        firstLine = i;
      } else {
        lastLine = i;
        break;
      }
    }
  }
  return lastLine !== undefined ? { firstLine, lastLine } : undefined;
}

function parseFrontMatter(lines) {
  const data = {};
  for (let i = 0; i < lines.length; i++) {
    const tokens = lines[i].split(':').map(x => x.trim());
    if (tokens.length === 2) {
      data[tokens[0]] = tokens[1];
    }
  }
  return data;
}

function separateFrontMatter(content) {
  const lines = content.split('\n');
  const position = getFrontMatterPosition(lines);
  if (position) {
    const frontmatter = parseFrontMatter(lines.slice(position.firstLine + 1, position.lastLine));
    const content = lines.slice(position.lastLine + 1).join('\n');
    return { frontmatter, content };
  } else {
    return { frontmatter: {}, content };
  }
}
