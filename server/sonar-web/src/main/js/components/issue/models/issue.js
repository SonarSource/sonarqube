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

export default Backbone.Model.extend({
  idAttribute: 'key',

  defaults () {
    return {
      flows: []
    };
  },

  url () {
    return window.baseUrl + '/api/issues';
  },

  urlRoot () {
    return window.baseUrl + '/api/issues';
  },

  parse (r) {
    let issue = Array.isArray(r.issues) && r.issues.length > 0 ? r.issues[0] : r.issue;
    if (issue) {
      issue = this._injectRelational(issue, r.components, 'component', 'key');
      issue = this._injectRelational(issue, r.components, 'project', 'key');
      issue = this._injectRelational(issue, r.components, 'subProject', 'key');
      issue = this._injectRelational(issue, r.rules, 'rule', 'key');
      issue = this._injectRelational(issue, r.users, 'assignee', 'login');
      issue = this._injectCommentsRelational(issue, r.users);
      issue = this._prepareClosed(issue);
      issue = this.ensureTextRange(issue);
      return issue;
    } else {
      return r;
    }
  },

  _injectRelational (issue, source, baseField, lookupField) {
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

  _injectCommentsRelational (issue, users) {
    if (issue.comments) {
      const newComments = issue.comments.map(comment => {
        let newComment = { ...comment, author: comment.login };
        delete newComment.login;
        newComment = this._injectRelational(newComment, users, 'author', 'login');
        return newComment;
      });
      return { ...issue, comments: newComments };
    }
    return issue;
  },

  _prepareClosed (issue) {
    if (issue.status === 'CLOSED') {
      issue.flows = [];
      delete issue.textRange;
    }
    return issue;
  },

  ensureTextRange (issue) {
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

  sync (method, model, options) {
    const opts = options || {};
    opts.contentType = 'application/x-www-form-urlencoded';
    if (method === 'read') {
      Object.assign(opts, {
        type: 'GET',
        url: this.urlRoot() + '/search',
        data: {
          issues: model.id,
          additionalFields: '_all'
        }
      });
    }
    if (method === 'create') {
      Object.assign(opts, {
        type: 'POST',
        url: this.urlRoot() + '/create',
        data: {
          component: model.get('component'),
          line: model.get('line'),
          message: model.get('message'),
          rule: model.get('rule'),
          severity: model.get('severity')
        }
      });
    }
    const xhr = options.xhr = Backbone.ajax(opts);
    model.trigger('request', model, xhr, opts);
    return xhr;
  },

  /**
   * Reset issue attributes (delete old, replace with new)
   * @param attrs
   * @param options
   * @returns {Object}
   */
  reset (attrs, options) {
    for (const key in this.attributes) {
      if (this.attributes.hasOwnProperty(key) && !(key in attrs)) {
        attrs[key] = void 0;
      }
    }
    return this.set(attrs, options);
  },

  /**
   * Do an action over an issue
   * @param {Object|null} options Options for jQuery ajax
   * @returns {jqXHR}
   * @private
   */
  _action (options) {
    const that = this;
    const success = function (r) {
      const attrs = that.parse(r);
      that.reset(attrs);
      if (options.success) {
        options.success(that, r, options);
      }
    };
    const opts = { type: 'POST', ...options, success };
    const xhr = options.xhr = Backbone.ajax(opts);
    this.trigger('request', this, xhr, opts);
    return xhr;
  },

  /**
   * Assign issue
   * @param {String|null} assignee Assignee, can be null to unassign issue
   * @param {Object|null} options Options for jQuery ajax
   * @returns {jqXHR}
   */
  assign (assignee, options) {
    const opts = {
      url: this.urlRoot() + '/assign',
      data: { issue: this.id, assignee },
      ...options
    };
    return this._action(opts);
  },

  /**
   * Plan issue
   * @param {String|null} plan Action Plan, can be null to unplan issue
   * @param {Object|null} options Options for jQuery ajax
   * @returns {jqXHR}
   */
  plan (plan, options) {
    const opts = {
      url: this.urlRoot() + '/plan',
      data: { issue: this.id, plan },
      ...options
    };
    return this._action(opts);
  },

  /**
   * Set severity of issue
   * @param {String|null} severity Severity
   * @param {Object|null} options Options for jQuery ajax
   * @returns {jqXHR}
   */
  setSeverity (severity, options) {
    const opts = {
      url: this.urlRoot() + '/set_severity',
      data: { issue: this.id, severity },
      ...options
    };
    return this._action(opts);
  },

  /**
   * Do transition on issue
   * @param {String|null} transition Transition
   * @param {Object|null} options Options for jQuery ajax
   * @returns {jqXHR}
   */
  transition (transition, options) {
    const that = this;
    const opts = {
      url: this.urlRoot() + '/do_transition',
      data: { issue: this.id, transition },
      ...options
    };
    return this._action(opts).done(() => {
      that.trigger('transition', transition);
    });
  },

  /**
   * Set type of issue
   * @param {String|null} issueType Issue type
   * @param {Object|null} options Options for jQuery ajax
   * @returns {jqXHR}
   */
  setType (issueType, options) {
    const opts = {
      url: this.urlRoot() + '/set_type',
      data: { issue: this.id, type: issueType },
      ...options
    };
    return this._action(opts);
  },

  /**
   * Do a custom (plugin) action
   * @param {String} actionKey Action Key
   * @param {Object|null} options Options for jQuery ajax
   * @returns {jqXHR}
   */
  customAction (actionKey, options) {
    const opts = {
      type: 'POST',
      url: this.urlRoot() + '/do_action',
      data: { issue: this.id, actionKey },
      ...options
    };
    const xhr = Backbone.ajax(opts);
    this.trigger('request', this, xhr, opts);
    return xhr;
  },

  getLinearLocations () {
    const textRange = this.get('textRange');
    if (!textRange) {
      return [];
    }
    const locations = [];
    for (let line = textRange.startLine; line <= textRange.endLine; line++) {
      // TODO fix 999999
      const from = line === textRange.startLine ? textRange.startOffset : 0;
      const to = line === textRange.endLine ? textRange.endOffset : 999999;
      locations.push({ line, from, to });
    }
    return locations;
  }
});
