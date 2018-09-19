/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
const remark = require('remark');
const fs = require('fs');
const path = require('path');
const glob = require('glob-promise');
const visit = require('unist-util-visit');

it('should not have any broken link', async () => {
  const root = path.resolve(__dirname + '/..');
  const files = await glob(root + '/pages/**/*.md')
    .then(files => files.map(file => file.substr(root.length + 1)))
    .then(files =>
      files.map(file => ({
        path: file.slice(0, -3),
        content: handleIncludes(fs.readFileSync(root + '/' + file, 'utf8'), root)
      }))
    );

  const parsedFiles = files.map(file => {
    return { ...separateFrontMatter(file.content), path: file.path };
  });

  const trees = [
    'SonarCloudNavigationTree.json',
    'SonarQubeNavigationTree.json',
    'StaticNavigationTree.json'
  ];
  trees.forEach(file => {
    const tree = JSON.parse(fs.readFileSync(root + '/../static/' + file, 'utf8'));
    tree.forEach(leaf => {
      if (typeof leaf === 'object') {
        if (leaf.children) {
          leaf.children.forEach(child => {
            // Check children markdown file path validity
            const result = urlExists(parsedFiles, child);
            if (!result) {
              // Display custom error message
              console.log('[', child, '] is not a valid link, in ', file);
            }
            expect(result).toBeTruthy();
          });
        }
      } else {
        // Check markdown file path validity
        const result = urlExists(parsedFiles, leaf);
        if (!result) {
          console.log('[', leaf, '] is not a valid link, in ', file);
        }
        expect(result).toBeTruthy();
      }
    });
  });

  // Check if all url tag in frontmatter are valid and uniques
  let urlLists = [];
  parsedFiles.map(file => {
    let result = file.frontmatter.url;
    if (!result) {
      console.log('[', file.path, '] has no url metadata');
    }
    expect(result).toBeTruthy();

    result = file.frontmatter.url.startsWith('/');
    if (!result) {
      console.log('[', file.path, '] should starts with a slash  ', file.frontmatter.url);
    }
    expect(result).toBeTruthy();

    result = file.frontmatter.url.endsWith('/');
    if (!result) {
      console.log('[', file.path, '] should ends with a slash  ', file.frontmatter.url);
    }
    expect(result).toBeTruthy();

    result = !urlLists.includes(file.frontmatter.url);
    if (!result) {
      console.log('[', file.path, '] has an url that is not unique  ', file.frontmatter.url);
    }
    expect(result).toBeTruthy();

    urlLists = [...urlLists, file.frontmatter.url];
  });

  parsedFiles.map(file => {
    const ast = remark().parse(file.content);
    visit(ast, node => {
      if (node.type === 'image' && !node.url.startsWith('http')) {
        // Check image path validity
        const result = fs.existsSync(root + '/' + node.url);
        if (!result) {
          console.log('[', node.url, '] is not a valid image path, in ', file.path + '.md');
        }
        expect(result).toBeTruthy();
      } else if (
        node.type === 'link' &&
        !node.url.startsWith('http') &&
        !node.url.startsWith('/#')
      ) {
        // Check markdown file path validity
        const result = urlExists(parsedFiles, node.url);
        if (!result) {
          console.log('[', node.url, '] is not a valid link, in ', file.path + '.md');
        }
        expect(result).toBeTruthy();
      }
    });
  });
  expect(true).toBeTruthy();
});

function urlExists(files, url) {
  return files.find(f => f.frontmatter.url === url) !== undefined;
}

function handleIncludes(content, root) {
  return content.replace(/@include (.+)/, (match, p) => {
    const filePath = path.join(root, '..', `${p}.md`);
    return fs.readFileSync(filePath, 'utf8');
  });
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
