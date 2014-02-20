define(['backbone', 'navigator/filters/base-filters', 'navigator/filters/select-filters'], function (Backbone, BaseFilters, SelectFilters) {

  var PAGE_SIZE = 100,
      UNASSIGNED = '<unassigned>';



  var Suggestions = Backbone.Collection.extend({

    initialize: function() {
      this.more = false;
      this.page = 0;
    },


    parse: function(r) {
      this.more = r.more;
      return r.results;
    },


    fetch: function(options) {
      this.data = _.extend({
            p: 1,
            ps: PAGE_SIZE
          }, options.data || {});

      var settings = _.extend({}, options, { data: this.data });
      Backbone.Collection.prototype.fetch.call(this, settings);
    },


    fetchNextPage: function(options) {
      if (this.more) {
        this.data.p += 1;
        var settings = _.extend({ remove: false }, options, { data: this.data });
        this.fetch(settings);
      }
    }

  });



  var UserSuggestions = Suggestions.extend({

    url: function() {
      return baseUrl + '/api/users/search?f=s2';
    }

  });



  var ProjectSuggestions = Suggestions.extend({

    url: function() {
      return baseUrl + '/api/resources/search?f=s2&q=TRK&display_key=true';
    }

  });



  var ComponentSuggestions = Suggestions.extend({

    url: function() {
      return baseUrl + '/api/resources/search?f=s2&qp=supportsGlobalDashboards&display_key=true';
    },

    parse: function(r) {
      this.more = r.more;

      // If results are divided into categories
      if (r.results.length > 0 && r.results[0].children) {
        var results = [];
        _.each(r.results, function(category) {
          _.each(category.children, function(child) {
            child.category = category.text;
            results.push(child);
          });
        });
        return results;
      } else {
        return r.results;
      }
    }

  });



  var AjaxSelectDetailsFilterView = SelectFilters.DetailsSelectFilterView.extend({
    template: '#ajaxSelectFilterTemplate',


    onRender: function() {
      this.resetChoices();

      var that = this,
          keyup = function(e) {
            if (e.keyCode !== 38 && e.keyCode !== 40) {
              that.search();
            }
          },
          debouncedKeyup = _.debounce(keyup, 250),
          scroll = function() { that.scroll(); },
          throttledScroll = _.throttle(scroll, 1000);

      this.$('.navigator-filter-search input')
          .off('keyup keydown')
          .on('keyup', debouncedKeyup)
          .on('keydown', this.keydown);

      this.$('.choices')
          .off('scroll')
          .on('scroll', throttledScroll);
    },


    search: function() {
      var that = this;
      this.query = this.$('.navigator-filter-search input').val();
      if (this.query.length > 1) {
        this.$el.addClass('fetching');
        this.options.filterView.choices.fetch({
          data: {
            s: this.query,
            ps: PAGE_SIZE
          },
          success: function() {
            var choices = that.options.filterView.choices.reject(function(item) {
              return that.options.filterView.selection.findWhere({ id: item.get('id') });
            });
            that.options.filterView.choices.reset(choices);
            that.updateLists();
            that.$el.removeClass('fetching');
          }
        });
      } else {
        this.resetChoices();
        this.updateLists();
      }
    },


    scroll: function() {
      var el = this.$('.choices'),
          scrollBottom = el.scrollTop() >=
          el[0].scrollHeight - el.outerHeight();

      if (scrollBottom) {
        this.options.filterView.selection.fetchNextPage();
      }
    },


    keydown: function(e) {
      if (_([37, 38, 39, 40, 13]).indexOf(e.keyCode) !== -1) {
        e.preventDefault();
      }
    },


    resetChoices: function() {
      this.options.filterView.choices.reset([]);
    },


    onShow: function() {
      SelectFilters.DetailsSelectFilterView.prototype.onShow.apply(this, arguments);
      this.$('.navigator-filter-search input').focus();
    }

  });



  var AjaxSelectFilterView = SelectFilters.SelectFilterView.extend({

    isDefaultValue: function() {
      return this.selection.length === 0;
    },


    renderInput: function() {
      var value = this.model.get('value') || [],
          input = $j('<input>')
          .prop('name', this.model.get('property'))
          .prop('type', 'hidden')
          .css('display', 'none')
          .val(value.join());
      input.appendTo(this.$el);
    },


    restore: function(value, param) {
      if (_.isString(value)) {
        value = value.split(',');
      }

      if (this.choices && this.selection && value.length > 0) {
        this.selection.reset([]);
        this.model.set({
          value: value,
          enabled: true
        });

        if (_.isArray(param.text) && param.text.length === value.length) {
          this.restoreFromText(value, param.text);
        } else {
          this.restoreByRequests(value);
        }
      } else {
        this.clear();
      }
    },


    restoreFromText: function(value, text) {
      var that = this;
      _.each(value, function(v, i) {
        that.selection.add(new Backbone.Model({
          id: v,
          text: text[i]
        }));
      });
      this.onRestore(value);
    },


    restoreByRequests: function(value) {
      var that = this,
          requests = _.map(value, function(v) {
            return that.createRequest(v);
          });

      $j.when.apply($j, requests).done(function () {
        that.onRestore(value);
      });
    },


    onRestore: function(value) {
      this.detailsView.updateLists();
      this.renderBase();
    },


    clear: function() {
      this.model.unset('value');
      if (this.selection && this.choices) {
        this.choices.reset([]);
        this.selection.reset([]);
      }
      this.renderBase();
    },


    createRequest: function() {}

  });



  var ComponentFilterView = AjaxSelectFilterView.extend({

    initialize: function() {
      BaseFilters.BaseFilterView.prototype.initialize.call(this, {
        detailsView: AjaxSelectDetailsFilterView
      });

      this.selection = new ComponentSuggestions();
      this.choices = new ComponentSuggestions();
    },


    createRequest: function(v) {
      var that = this;
      return $j
          .ajax({
            url: baseUrl + '/api/resources',
            type: 'GET',
            data: { resource: v }
          })
          .done(function (r) {
            that.selection.add(new Backbone.Model({
              id: r[0].key,
              text: r[0].name
            }));
          });
    }

  });



  var ProjectFilterView = AjaxSelectFilterView.extend({

    initialize: function() {
      BaseFilters.BaseFilterView.prototype.initialize.call(this, {
        detailsView: AjaxSelectDetailsFilterView
      });

      this.selection = new ProjectSuggestions();
      this.choices = new ProjectSuggestions();
    },


    createRequest: function(v) {
      var that = this;
      return $j
          .ajax({
            url: baseUrl + '/api/resources',
            type: 'GET',
            data: { resource: v }
          })
          .done(function (r) {
            that.selection.add(new Backbone.Model({
              id: r[0].key,
              text: r[0].name
            }));
          });
    }

  });



  var AssigneeDetailsFilterView = AjaxSelectDetailsFilterView.extend({

    addToSelection: function(e) {
      var id = $j(e.target).val(),
          model = this.options.filterView.choices.findWhere({ id: id });

      if (this.model.get('multiple') && id !== UNASSIGNED) {
        this.options.filterView.selection.add(model);
        this.options.filterView.choices.remove(model);

        var unresolved = this.options.filterView.selection.findWhere({ id: UNASSIGNED });
        if (unresolved) {
          this.options.filterView.choices.add(unresolved);
          this.options.filterView.selection.remove(unresolved);
        }
      } else {
        this.options.filterView.choices.add(this.options.filterView.selection.models);
        this.options.filterView.choices.remove(model);
        this.options.filterView.selection.reset([model]);
      }

      this.updateValue();
      this.updateLists();
    },


    resetChoices: function() {
      if (this.options.filterView.selection.findWhere({ id: UNASSIGNED })) {
        this.options.filterView.choices.reset([]);
      } else {
        this.options.filterView.choices.reset([{
          id: UNASSIGNED,
          text: window.SS.phrases.unassigned,
          special: true
        }]);
      }
    },


    onShow: function() {
      AjaxSelectDetailsFilterView.prototype.onShow.apply(this, arguments);
      this.$('.navigator-filter-search input').focus();
    }

  });



  var AssigneeFilterView = AjaxSelectFilterView.extend({

    initialize: function() {
      BaseFilters.BaseFilterView.prototype.initialize.call(this, {
        detailsView: AssigneeDetailsFilterView
      });

      this.selection = new UserSuggestions();
      this.choices = new UserSuggestions();
    },


    restoreFromQuery: function(q) {
      var param = _.findWhere(q, { key: this.model.get('property') }),
          assigned = _.findWhere(q, { key: 'assigned' });

      if (!!assigned) {
        if (!param) {
          param = { value: UNASSIGNED };
        } else {
          param.value += ',' + UNASSIGNED;
        }
      }

      if (param && param.value) {
        this.model.set('enabled', true);
        this.restore(param.value, param);
      } else {
        this.clear();
      }
    },


    restoreFromText: function(value) {
      if (_.indexOf(value, UNASSIGNED) !== -1) {
        this.choices.reset([]);
      }

      AjaxSelectFilterView.prototype.restoreFromText.apply(this, arguments);
    },


    restoreByRequests: function(value) {
      if (_.indexOf(value, UNASSIGNED) !== -1) {
        this.selection.add(new Backbone.Model({
          id: UNASSIGNED,
          text: window.SS.phrases.unassigned,
          special: true
        }));
        this.choices.reset([]);
        value = _.reject(value, function(k) { return k === UNASSIGNED; });
      }

      AjaxSelectFilterView.prototype.restoreByRequests.call(this, value);
    },


    createRequest: function(v) {
      var that = this;
      return $j
          .ajax({
            url: baseUrl + '/api/users/search',
            type: 'GET',
            data: { logins: v }
          })
          .done(function (r) {
            that.selection.add(new Backbone.Model({
              id: r.users[0].login,
              text: r.users[0].name + ' (' + r.users[0].login + ')'
            }));
          });
    },


    clear: function() {
      this.model.unset('value');
      if (this.selection && this.choices) {
        this.detailsView.resetChoices();
        this.selection.reset([]);
      }
      this.renderBase();
      this.detailsView.updateLists();
    },


    formatValue: function() {
      var q = {};
      if (this.model.has('property') && this.model.has('value') && this.model.get('value').length > 0) {
        var assignees = _.without(this.model.get('value'), UNASSIGNED);
        if (assignees.length > 0) {
          q[this.model.get('property')] = assignees.join(',');
        }
        if (this.model.get('value').length > assignees.length) {
          q.assigned = false;
        }
      }
      return q;
    }

  });



  var ReporterFilterView = AjaxSelectFilterView.extend({

    initialize: function() {
      BaseFilters.BaseFilterView.prototype.initialize.call(this, {
        detailsView: AjaxSelectDetailsFilterView
      });

      this.selection = new UserSuggestions();
      this.choices = new UserSuggestions();
    },


    createRequest: function(v) {
      var that = this;
      return $j
          .ajax({
            url: baseUrl + '/api/users/search',
            type: 'GET',
            data: { logins: v }
          })
          .done(function (r) {
            that.selection.add(new Backbone.Model({
              id: r.users[0].login,
              text: r.users[0].name + ' (' + r.users[0].login + ')'
            }));
          });
    }

  });



  /*
   * Export public classes
   */

  return {
    Suggestions: Suggestions,
    AjaxSelectDetailsFilterView: AjaxSelectDetailsFilterView,
    AjaxSelectFilterView: AjaxSelectFilterView,
    ProjectFilterView: ProjectFilterView,
    ComponentFilterView: ComponentFilterView,
    AssigneeFilterView: AssigneeFilterView,
    ReporterFilterView: ReporterFilterView
  };

});
