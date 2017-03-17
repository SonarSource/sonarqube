/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import Backbone from 'backbone';
import Issue from './issue';

export default Backbone.Collection.extend({
  model: Issue,

  url() {
    return window.baseUrl + '/api/issues/search';
  },

  _injectRelational(issue, source, baseField, lookupField) {
    const baseValue = issue[baseField];
    if (baseValue != null && Array.isArray(source) && source.length > 0) {
      const lookupValue = source.find(candidate => candidate[lookupField] === baseValue);
      if (lookupValue != null) {
        Object.keys(lookupValue).forEach(key => {
          const newKey = baseField + key.charAt(0).toUpperCase() + key.slice(1);
          issue[newKey] = lookupValue[key];
        });
      }
    }
    return issue;
  },

  _injectCommentsRelational(issue, users) {
    if (issue.comments) {
      const that = this;
      const newComments = issue.comments.map(comment => {
        let newComment = { ...comment, author: comment.login };
        delete newComment.login;
        newComment = that._injectRelational(newComment, users, 'author', 'login');
        return newComment;
      });
      issue = { ...issue, comments: newComments };
    }
    return issue;
  },

  _prepareClosed(issue) {
    if (issue.status === 'CLOSED') {
      issue.flows = [];
      delete issue.textRange;
    }
    return issue;
  },

  ensureTextRange(issue) {
    if (issue.line && !issue.textRange) {
      // FIXME 999999
      issue.textRange = {
        startLine: issue.line,
        endLine: issue.line,
        startOffset: 0,
        endOffset: 999999
      };
    }
    return issue;
  },

  parseIssues(r, startIndex = 0) {
    const that = this;
    return r.issues.map((issue, index) => {
      Object.assign(issue, { index: startIndex + index });
      issue = that._injectRelational(issue, r.components, 'component', 'key');
      issue = that._injectRelational(issue, r.components, 'project', 'key');
      issue = that._injectRelational(issue, r.components, 'subProject', 'key');
      issue = that._injectRelational(issue, r.rules, 'rule', 'key');
      issue = that._injectRelational(issue, r.users, 'assignee', 'login');
      issue = that._injectCommentsRelational(issue, r.users);
      issue = that._prepareClosed(issue);
      issue = that.ensureTextRange(issue);
      return issue;
    });
  },

  setIndex() {
    return this.forEach((issue, index) => issue.set({ index }));
  },

  selectByKeys(keys) {
    const that = this;
    keys.forEach(key => {
      const issue = that.get(key);
      if (issue) {
        issue.set({ selected: true });
      }
    });
  }
});
