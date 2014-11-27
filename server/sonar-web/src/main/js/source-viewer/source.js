define([
  'backbone'
], function (Backbone) {

  return Backbone.Model.extend({
    idAttribute: 'uuid',

    defaults: function () {
      return {
        hasSource: false,
        hasCoverage: false,
        hasDuplications: false,
        hasSCM: false
      };
    },

    key: function () {
      return this.get('key');
    },

    addMeta: function (meta) {
      var source = this.get('source'),
          metaIdx = 0,
          metaLine = meta[metaIdx];
      source.forEach(function (line) {
        while (metaLine != null && line.line > metaLine.line) {
          metaLine = meta[++metaIdx];
        }
        if (metaLine != null && line.line === metaLine.line) {
          _.extend(line, metaLine);
          metaLine = meta[++metaIdx];
        }
      });
      this.set({ source: source });
    },

    addDuplications: function (duplications) {
      var source = this.get('source');
      if (source != null) {
        source.forEach(function (line) {
          var lineDuplications = [];
          duplications.forEach(function (d, i) {
            var duplicated = false;
            d.blocks.forEach(function (b) {
              if (b._ref === '1') {
                var lineFrom = b.from,
                    lineTo = b.from + b.size;
                if (line.line >= lineFrom && line.line <= lineTo) {
                  duplicated = true;
                }
              }
            });
            lineDuplications.push(duplicated ? i + 1 : false);
          });
          line.duplications = lineDuplications;
        });
      }
      this.set({ source: source });
    }
  });

});
