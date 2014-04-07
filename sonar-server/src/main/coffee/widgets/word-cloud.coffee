# Some helper functions

# Gets or sets parameter
param = (name, value) ->
  unless value?
    return this[name]
  else
    this[name] = value
    return @


window.SonarWidgets ?= {}

window.SonarWidgets.WordCloud = ->
  @_components = []
  @_metrics = []
  @_metricsPriority = []
  @_width = window.SonarWidgets.WordCloud.defaults.width
  @_height = window.SonarWidgets.WordCloud.defaults.height
  @_margin = window.SonarWidgets.WordCloud.defaults.margin
  @_legendWidth = window.SonarWidgets.WordCloud.defaults.legendWidth
  @_legendMargin = window.SonarWidgets.WordCloud.defaults.legendMargin
  @_detailsWidth = window.SonarWidgets.WordCloud.defaults.detailsWidth
  @_options = {}

  @_lineHeight = 20

  # Export global variables
  @metrics = (_) -> param.call(this, '_metrics', _)
  @metricsPriority = (_) -> param.call(this, '_metricsPriority', _)
  @components = (_) -> param.call(this, '_components', _)
  @width = (_) -> param.call(this, '_width', _)
  @height = (_) -> param.call(this, '_height', _)
  @margin = (_) -> param.call(this, '_margin', _)
  @legendWidth = (_) -> param.call(this, '_legendWidth', _)
  @legendMargin = (_) -> param.call(this, '_legendMargin', _)
  @detailsWidth = (_) -> param.call(this, '_detailsWidth', _)
  @options = (_) -> param.call(this, '_options', _)
  @


window.SonarWidgets.WordCloud.prototype.render = (container) ->
  @box = d3.select(container).append('div').classed 'sonar-d3', true

  # Configure metrics
  @colorMetric = @metricsPriority()[0]
  @getColorMetric = (d) => d.measures[@colorMetric].val
  @sizeMetric = @metricsPriority()[1]
  @getSizeMetric = (d) => d.measures[@sizeMetric].val

  # Configure scales
  @color = d3.scale.linear().domain([0, 100]).range(['#d62728', '#1f77b4'])
  sizeDomain = d3.extent @components(), (d) => @getSizeMetric d
  @size = d3.scale.linear().domain(sizeDomain).range([10, 24])

  @update container
  @


window.SonarWidgets.WordCloud.prototype.update = ->
  # Configure words
  @words = @box.selectAll('a').data @components()
  @words.enter().append('a').classed('cloud-word', true).text (d) -> d.name
  @words.style 'color', (d) => @color @getColorMetric d
  @words.style 'font-size', (d) => "#{@size @getSizeMetric d}px"
  @words.exit().remove()

  



window.SonarWidgets.WordCloud.defaults =
  width: 350
  height: 300
  margin: { top: 10, right: 10, bottom: 10, left: 10 }
  legendWidth: 160
  legendMargin: 30