define(function () {

  return Backbone.Model.extend({
    idAttribute: 'uuid',

    defaults: function () {
      return {
        exist: true,

        hasSource: false,
        hasCoverage: false,
        hasITCoverage: false,
        hasDuplications: false,
        hasSCM: false,

        canSeeCode: true
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
                    lineTo = b.from + b.size - 1;
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
    },

    checkIfHasDuplications: function () {
      var hasDuplications = false,
          source = this.get('source');
      if (source != null) {
        source.forEach(function (line) {
          if (line.duplicated) {
            hasDuplications = true;
          }
        });
      }
      this.set({ hasDuplications: hasDuplications });
    },

    hasUTCoverage: function (source) {
      return _.some(source, function (line) {
        return line.utCoverageStatus != null;
      });
    },

    hasITCoverage: function (source) {
      return _.some(source, function (line) {
        return line.itCoverageStatus != null;
      });
    }
  });

});
