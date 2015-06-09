define(function () {

  return Backbone.Model.extend({
    idAttribute: 'key',

    url: function () {
      return baseUrl + '/api/issues/show?key=' + this.get('key');
    },

    urlRoot: function () {
      return baseUrl + '/api/issues';
    },

    parse: function (r) {
      return r.issue ? r.issue : r;
    },

    sync: function (method, model, options) {
      var opts = options || {};
      opts.contentType = 'application/x-www-form-urlencoded';
      if (method === 'read') {
        _.extend(opts, {
          type: 'GET',
          url: this.urlRoot() + '/show',
          data: { key: model.id }
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
      var opts = _.extend({
        url: this.urlRoot() + '/do_transition',
        data: { issue: this.id, transition: transition }
      }, options);
      return this._action(opts);
    }
  });

});
