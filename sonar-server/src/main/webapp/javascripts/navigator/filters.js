/* global _:false, $j:false, Backbone:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var Filter = Backbone.Model.extend({

  });

  var Filters = Backbone.Collection.extend({
    model: Filter
  });


  var BaseFilterView = Backbone.Marionette.ItemView.extend({
    template: '#filterTemplate',
    className: 'navigator-filter',

    events: function() {
      return {};
    },

    renderBody: function() {
      return '';
    },

    serializeData: function() {
      return _.extend({}, this.model.toJSON(), {
        body: this.renderBody()
      });
    },

    restore: function() {

    }
  });


  var SelectFilterView = BaseFilterView.extend({

    modelEvents: {
      "change": "render"
    },

    renderBody: function() {
      var template = _.template($j('#selectFilterTemplate').html());
      return template(this.model.toJSON());
    },

    onRender: function() {
      var that = this;

      this.$('.navigator-filter-label').hide();

      this.$(':input').select2({
        allowClear: false,
        placeholder: this.model.get('name'),
        width: '150px'
      }).on('change', function(e) {
            that.model.set('value', e.val);
          });

      this.restore();
    },

    restore: function() {
      if (this.model.get('value')) {
        this.$(':input').select2('val', this.model.get('value'));
      }
    }
  });


  var AjaxSelectFilterView = BaseFilterView.extend({

    renderBody: function() {
      var template = _.template($j('#ajaxSelectFilterTemplate').html());
      return template(this.model.toJSON());
    },

    onRender: function() {
      var that = this;

      this.$('.navigator-filter-label').hide();

      this.$(':input').select2(_.extend({
        allowClear: false,
        placeholder: this.model.get('name'),
        width: '150px',
        minimumInputLength: 2
      }, this.model.get('select2')))
          .on('change', function(e) {
            that.model.set('value', e.val);
          });

      this.restore();
    },

    restore: function() {
      if (this.model.get('value')) {
        this.$(':input').select2('data', this.model.get('value'));
      }
    }
  });


  var RangeFilterView = BaseFilterView.extend({

    events: function() {
      return _.extend(BaseFilterView.prototype.events.call(), {
        'change input': 'changeInput'
      });
    },

    renderBody: function() {
      var template = _.template($j('#rangeFilterTemplate').html());
      return template(this.model.toJSON());
    },

    onRender: function() {
      this.restore();
    },

    changeInput: function() {
      this.model.set('value', {
        from: '',
        to: ''
      });
    },

    restore: function() {
      if (this.model.get('value')) {
        this.$('[name="' + this.model.get('propertyFrom') + '"]').val(this.model.get('value').from);
        this.$('[name="' + this.model.get('propertyTo') + '"]').val(this.model.get('value').to);
      }
    }

  });


  var FilterBarView = Backbone.Marionette.CompositeView.extend({
    template: '#filterBarTemplate',
    itemViewContainer: '.navigator-filters-list',

    collectionEvents: {
      'change': 'changeFilters'
    },

    getItemView: function(item) {
      return item.get('type') || BaseFilterView;
    },


    itemViewOptions: function() {
      return {
        filterBarView: this
      };
    },

    changeFilters: function() {
      var query = {};
      this.collection.each(function(item) {
        if (item.get('value')) {
          query[item.get('property')] = item.get('value');
        }
      });
      this.applyQuery($j.param(query));
    },

    applyQuery: function(query) {
      $j.ajax({
        url: '/dev/issues/search',
        type: 'get',
        data: query
      }).done(function(r) {
            $j('.navigator-results').html(r);
          });
    }
  });



  /*
   * Export public classes
   */

  _.extend(window.SS, {
    Filter: Filter,
    Filters: Filters,
    BaseFilterView: BaseFilterView,
    FilterBarView: FilterBarView,
    SelectFilterView: SelectFilterView,
    AjaxSelectFilterView: AjaxSelectFilterView,
    RangeFilterView: RangeFilterView
  });

})();
