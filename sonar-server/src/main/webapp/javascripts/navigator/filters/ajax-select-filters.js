/* global _:false, $j:false, Backbone:false, baseUrl:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var PAGE_SIZE = 100;



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



  var AjaxSelectDetailsFilterView = window.SS.DetailsSelectFilterView.extend({
    template: '#ajaxSelectFilterTemplate',


    initialize: function() {
      window.SS.DetailsSelectFilterView.prototype.initialize.apply(this, arguments);
    },


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
      if (_([37, 38, 39, 40, 32, 13]).indexOf(e.keyCode) !== -1) {
        e.preventDefault();
      }
    },


    resetChoices: function() {
      this.options.filterView.choices.reset([]);
    },


    onShow: function() {
      window.SS.DetailsSelectFilterView.prototype.onShow.apply(this, arguments);
      this.$('.navigator-filter-search input').focus();
    }

  });



  var AjaxSelectFilterView = window.SS.SelectFilterView.extend({

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


    restore: function(value) {
      if (this.choices && this.selection && value.length > 0) {
        var that = this;
        this.selection.reset([]);

        var requests = _.map(value, function(v) {
          return that.createRequest(v);
        });

        $j.when.apply($j, requests).done(function () {
          that.detailsView.updateLists();
          that.model.set({
            value: value,
            enabled: true
          });
        });
      }
    },


    createRequest: function() {}

  });



  var ProjectFilterView = AjaxSelectFilterView.extend({

    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
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

    resetChoices: function() {
      this.options.filterView.choices.reset([{
        id: '<unassigned>',
        text: 'Unassigned'
      }]);
    },


    onShow: function() {
      this.$('.navigator-filter-search input').focus();
    }

  });



  var AssigneeFilterView = AjaxSelectFilterView.extend({

    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
        detailsView: AssigneeDetailsFilterView
      });

      this.selection = new UserSuggestions();
      this.choices = new UserSuggestions();
    },


    restore: function(value) {
      if (this.choices && this.selection && value.length > 0) {
        var that = this;
        this.selection.reset([]);

        if (_.indexOf(value, '<unassigned>') !== -1) {
          this.selection.add(new Backbone.Model({
            id: '<unassigned>',
            text: 'Unassigned'
          }));
          this.choices.reset([]);
          value = _.reject(value, function(k) { return k === '<unassigned>'; });
        }


        var requests = _.map(value, function(v) {
          return that.createRequest(v);
        });

        $j.when.apply($j, requests).done(function () {
          that.detailsView.updateLists();
          that.model.set({
            value: value,
            enabled: true
          });
        });
      }
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



  var ReporterFilterView = AjaxSelectFilterView.extend({

    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
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

  _.extend(window.SS, {
    ProjectFilterView: ProjectFilterView,
    AssigneeFilterView: AssigneeFilterView,
    ReporterFilterView: ReporterFilterView
  });

})();
