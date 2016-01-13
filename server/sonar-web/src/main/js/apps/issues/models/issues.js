/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import _ from 'underscore';
import Backbone from 'backbone';
import Issue from './issue';

export default Backbone.Collection.extend({
  model: Issue,

  url: function () {
    return baseUrl + '/api/issues/search';
  },

  _injectRelational: function (issue, source, baseField, lookupField) {
    var baseValue = issue[baseField];
    if (baseValue != null && _.size(source)) {
      var lookupValue = _.find(source, function (candidate) {
        return candidate[lookupField] === baseValue;
      });
      if (lookupValue != null) {
        Object.keys(lookupValue).forEach(function (key) {
          var newKey = baseField + key.charAt(0).toUpperCase() + key.slice(1);
          issue[newKey] = lookupValue[key];
        });
      }
    }
    return issue;
  },

  _injectCommentsRelational: function (issue, users) {
    if (issue.comments) {
      var that = this;
      var newComments = issue.comments.map(function (comment) {
        var newComment = _.extend({}, comment, { author: comment.login });
        delete newComment.login;
        newComment = that._injectRelational(newComment, users, 'author', 'login');
        return newComment;
      });
      issue = _.extend({}, issue, { comments: newComments });
    }
    return issue;
  },

  _prepareClosed: function (issue) {
    if (issue.status === 'CLOSED') {
      issue.flows = [];
      delete issue.textRange;
    }
    return issue;
  },

  ensureTextRange: function (issue) {
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

  parseIssues: function (r) {
    var that = this;
    return r.issues.map(function (issue, index) {
      _.extend(issue, { index: index });
      issue = that._injectRelational(issue, r.components, 'component', 'key');
      issue = that._injectRelational(issue, r.components, 'project', 'key');
      issue = that._injectRelational(issue, r.components, 'subProject', 'key');
      issue = that._injectRelational(issue, r.rules, 'rule', 'key');
      issue = that._injectRelational(issue, r.users, 'assignee', 'login');
      issue = that._injectRelational(issue, r.users, 'reporter', 'login');
      issue = that._injectRelational(issue, r.actionPlans, 'actionPlan', 'key');
      issue = that._injectCommentsRelational(issue, r.users);
      issue = that._prepareClosed(issue);
      issue = that.ensureTextRange(issue);
      return issue;
    });
  },

  setIndex: function () {
    return this.forEach(function (issue, index) {
      return issue.set({ index: index });
    });
  },

  selectByKeys: function (keys) {
    var that = this;
    keys.forEach(function (key) {
      var issue = that.get(key);
      if (issue) {
        issue.set({ selected: true });
      }
    });
  }
});


