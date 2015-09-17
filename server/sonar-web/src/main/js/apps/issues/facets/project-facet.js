import _ from 'underscore';
import CustomValuesFacet from './custom-values-facet';

export default CustomValuesFacet.extend({

  getUrl: function () {
    var q = this.options.app.state.get('contextComponentQualifier');
    if (q === 'VW' || q === 'SVW') {
      return baseUrl + '/api/components/search';
    } else {
      return baseUrl + '/api/resources/search?f=s2&q=TRK&display_uuid=true';
    }
  },

  prepareSearch: function () {
    var q = this.options.app.state.get('contextComponentQualifier');
    if (q === 'VW' || q === 'SVW') {
      return this.prepareSearchForViews();
    } else {
      return CustomValuesFacet.prototype.prepareSearch.apply(this, arguments);
    }
  },

  prepareSearchForViews: function () {
    var componentUuid = this.options.app.state.get('contextComponentUuid');
    return this.$('.js-custom-value').select2({
      placeholder: 'Search...',
      minimumInputLength: 2,
      allowClear: false,
      formatNoMatches: function () {
        return t('select2.noMatches');
      },
      formatSearching: function () {
        return t('select2.searching');
      },
      formatInputTooShort: function () {
        return tp('select2.tooShort', 2);
      },
      width: '100%',
      ajax: {
        quietMillis: 300,
        url: this.getUrl(),
        data: function (term, page) {
          return { q: term, componentUuid: componentUuid, p: page, ps: 25 };
        },
        results: function (data) {
          return {
            more: data.p * data.ps < data.total,
            results: data.components.map(function (c) {
              return { id: c.uuid, text: c.name };
            })
          };
        }
      }
    });
  },

  getValuesWithLabels: function () {
    var values = this.model.getValues(),
        projects = this.options.app.facets.components;
    values.forEach(function (v) {
      var uuid = v.val,
          label = '';
      if (uuid) {
        var project = _.findWhere(projects, { uuid: uuid });
        if (project != null) {
          label = project.longName;
        }
      }
      v.label = label;
    });
    return values;
  },

  serializeData: function () {
    return _.extend(CustomValuesFacet.prototype.serializeData.apply(this, arguments), {
      values: this.sortValues(this.getValuesWithLabels())
    });
  }
});


