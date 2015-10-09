import _ from 'underscore';
import Backbone from 'backbone';

export default Backbone.Model.extend({
  idAttribute: 'key',

  defaults: function () {
    return {
      flows: []
    };
  },

  url: function () {
    return window.baseUrl + '/api/issues';
  },

  urlRoot: function () {
    return window.baseUrl + '/api/issues';
  },

  parse: function (r) {
    var issue = _.size(r.issues) > 0 ? r.issues[0] : r.issue;
    if (issue) {
      issue = this._injectRelational(issue, r.components, 'component', 'key');
      issue = this._injectRelational(issue, r.components, 'project', 'key');
      issue = this._injectRelational(issue, r.components, 'subProject', 'key');
      issue = this._injectRelational(issue, r.rules, 'rule', 'key');
      issue = this._injectRelational(issue, r.users, 'assignee', 'login');
      issue = this._injectRelational(issue, r.users, 'reporter', 'login');
      issue = this._injectRelational(issue, r.actionPlans, 'actionPlan', 'key');
      issue = this._injectCommentsRelational(issue, r.users);
      issue = this._prepareClosed(issue);
      issue = this.ensureTextRange(issue);
      return issue;
    } else {
      return r;
    }
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

  sync: function (method, model, options) {
    var opts = options || {};
    opts.contentType = 'application/x-www-form-urlencoded';
    if (method === 'read') {
      _.extend(opts, {
        type: 'GET',
        url: this.urlRoot() + '/search',
        data: {
          issues: model.id,
          additionalFields: '_all'
        }
      });
    }
    if (method === 'create') {
      _.extend(opts, {
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
    var xhr = options.xhr = Backbone.ajax(opts);
    model.trigger('request', model, xhr, opts);
    return xhr;
  },

  /**
   * Reset issue attributes (delete old, replace with new)
   * @param attrs
   * @param options
   * @returns {Object}
   */
  reset: function (attrs, options) {
    for (var key in this.attributes) {
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
  _action: function (options) {
    var model = this;
    var success = function (r) {
      var attrs = model.parse(r);
      model.reset(attrs);
      if (options.success) {
        options.success(model, r, options);
      }
    };
    var opts = _.extend({ type: 'POST' }, options, { success: success });
    var xhr = options.xhr = Backbone.ajax(opts);
    model.trigger('request', model, xhr, opts);
    return xhr;
  },

  /**
   * Assign issue
   * @param {String|null} assignee Assignee, can be null to unassign issue
   * @param {Object|null} options Options for jQuery ajax
   * @returns {jqXHR}
   */
  assign: function (assignee, options) {
    var opts = _.extend({
      url: this.urlRoot() + '/assign',
      data: { issue: this.id, assignee: assignee }
    }, options);
    return this._action(opts);
  },

  /**
   * Plan issue
   * @param {String|null} plan Action Plan, can be null to unplan issue
   * @param {Object|null} options Options for jQuery ajax
   * @returns {jqXHR}
   */
  plan: function (plan, options) {
    var opts = _.extend({
      url: this.urlRoot() + '/plan',
      data: { issue: this.id, plan: plan }
    }, options);
    return this._action(opts);
  },

  /**
   * Set severity of issue
   * @param {String|null} severity Severity
   * @param {Object|null} options Options for jQuery ajax
   * @returns {jqXHR}
   */
  setSeverity: function (severity, options) {
    var opts = _.extend({
      url: this.urlRoot() + '/set_severity',
      data: { issue: this.id, severity: severity }
    }, options);
    return this._action(opts);
  },

  /**
   * Do transition on issue
   * @param {String|null} transition Transition
   * @param {Object|null} options Options for jQuery ajax
   * @returns {jqXHR}
   */
  transition: function (transition, options) {
    var that = this;
    var opts = _.extend({
      url: this.urlRoot() + '/do_transition',
      data: { issue: this.id, transition: transition }
    }, options);
    return this._action(opts).done(function () {
      that.trigger('transition', transition);
    });
  },


  /**
   * Do a custom (plugin) action
   * @param {String} actionKey Action Key
   * @param {Object|null} options Options for jQuery ajax
   * @returns {jqXHR}
   */
  customAction: function (actionKey, options) {
    var opts = _.extend({
      type: 'POST',
      url: this.urlRoot() + '/do_action',
      data: { issue: this.id, actionKey: actionKey }
    }, options);
    var xhr = Backbone.ajax(opts);
    this.trigger('request', this, xhr, opts);
    return xhr;
  },

  getLinearLocations: function () {
    var textRange = this.get('textRange');
    if (!textRange) {
      return [];
    }
    var locations = [];
    for (var line = textRange.startLine; line <= textRange.endLine; line++) {
      // TODO fix 999999
      var from = line === textRange.startLine ? textRange.startOffset : 0,
          to = line === textRange.endLine ? textRange.endOffset : 999999;
      locations.push({ line: line, from: from, to: to });
    }
    return locations;
  }
});


