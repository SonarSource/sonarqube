window.SonarWidgets ?= {}

class BaseWidget
  lineHeight: 20

  colors4: ['#ee0000', '#f77700', '#80cc00', '#00aa00']
  colors4r: ['#00aa00', '#80cc00', '#f77700', '#ee0000']
  colors5: ['#ee0000', '#f77700', '#ffee00', '#80cc00', '#00aa00']
  colors5r: ['#00aa00', '#80cc00', '#ffee00', '#f77700', '#ee0000']
  colorUnknown: '#777'


  constructor: ->
    @addField 'components', []
    @addField 'metrics', []
    @addField 'metricsPriority', []
    @addField 'options', []
    @


  addField: (name, defaultValue) ->
    privateName = "_#{name}"
    @[privateName] = defaultValue
    @[name] = (d) -> @param.call @, privateName, d
    @


  param: (name, value) ->
    return @[name] unless value?
    @[name] = value
    @


  addMetric: (property, index) ->
    key = @metricsPriority()[index]
    @[property] = _.extend @metrics()[key],
      key: key
      value: (d) -> d.measures[key]?.val
      formattedValue: (d) -> d.measures[key]?.fval
    @


  trans: (left, top) ->
    "translate(#{left},#{top})"


  render: (container) ->
    @update container
    @


  update: ->
    @


  tooltip: (d) ->
    title = d.longName
    title += "\n#{@colorMetric.name}: #{@colorMetric.formattedValue d}" if @colorMetric.value(d)?
    title += "\n#{@sizeMetric.name}: #{@sizeMetric.formattedValue d}" if @sizeMetric.value(d)?
    title


window.SonarWidgets.BaseWidget = BaseWidget
