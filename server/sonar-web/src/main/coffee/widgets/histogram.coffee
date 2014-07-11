class Histogram extends window.SonarWidgets.BaseWidget
  barHeight: 16
  barFill: '#1f77b4'

  constructor: ->
    @addField 'width', 0
    @addField 'height', window.SonarWidgets.Histogram.defaults.height
    @addField 'margin', window.SonarWidgets.Histogram.defaults.margin
    @addField 'legendWidth', window.SonarWidgets.Histogram.defaults.legendWidth
    @addField 'maxResultsReached', false
    super


  isDataValid: ->
    @components().reduce ((p, c) => p && !! c.measures[@mainMetric.key]), true


  truncate: (text, type) ->
    maxLength = 40
    switch type
      when "FIL", "CLA"
        n = text.length
        if n > maxLength
          shortText = text.substr(n - maxLength + 2, n - 1)
          dotIndex = shortText.indexOf(".")
          return "..." + shortText.substr(dotIndex + 1)
        else
          return text
      else
        (if text.length > maxLength then text.substr(0, maxLength - 3) + "..." else text)


  render: (container) ->
    box = d3.select container

    # Configure the main metric
    @addMetric 'mainMetric', 0

    unless @isDataValid()
      box.text @options().noMainMetric
      return

    @width box.property 'offsetWidth'

    # Create skeleton
    @svg = box.append('svg').classed 'sonar-d3', true
    @gWrap = @svg.append 'g'
    @gWrap.attr 'transform', @trans(@margin().left, @margin().top)
    @plotWrap = @gWrap.append('g').classed 'plot', true

    # Configure scales
    @x = d3.scale.linear()
    @y = d3.scale.ordinal()

    @metricLabel = @gWrap.append('text').text @mainMetric.name
    @metricLabel.attr('dy', '9px').style 'font-size', '12px'

    if @maxResultsReached()
      @maxResultsReachedLabel = @gWrap.append('text').classed 'max-results-reached-message', true
      @maxResultsReachedLabel.text @options().maxItemsReachedMessage

    super


  update: (container) ->
    box = d3.select(container)

    @width box.property 'offsetWidth'
    availableWidth = @width() - @margin().left - @margin().right - @legendWidth()
    availableHeight = @barHeight * @components().length + @lineHeight
    totalHeight = availableHeight + @margin().top + @margin().bottom
    totalHeight += @lineHeight if @maxResultsReached()
    @height totalHeight

    @svg.attr('width', @width()).attr 'height', @height()
    @plotWrap.attr 'transform', @trans 0, @lineHeight

    # Configure scales
    xDomain = d3.extent @components(), (d) => @mainMetric.value d
    unless @options().relativeScale
      if @mainMetric.type == 'PERCENT'
        xDomain = [0, 100]
      else
        xDomain[0] = 0

    @x.domain xDomain
      .range [0, availableWidth]

    @y.domain @components().map (d, i) -> i
      .rangeRoundBands [0, availableHeight], 0

    # Configure bars
    @bars = @plotWrap.selectAll('.bar').data @components()
    @barsEnter = @bars.enter().append 'g'
      .classed 'bar', true
      .attr 'transform', (d, i) => @trans 0, i * @barHeight
    @barsEnter.append 'rect'
      .style 'fill', @barFill
    @barsEnter.append 'text'
      .classed 'legend-text component', true
      .style 'text-anchor', 'end'
      .attr 'dy', '-0.35em'
      .text (d) => @truncate d.longName, d.qualifier
      .attr 'transform', => @trans @legendWidth() - 10, @barHeight
    @barsEnter.append 'text'
      .classed 'legend-text value', true
      .attr 'dy', '-0.35em'
      .text (d) => @mainMetric.formattedValue d
      .attr 'transform', (d) => @trans @legendWidth() + @x(@mainMetric.value d) + 5, @barHeight
    @bars.selectAll 'rect'
      .transition()
      .attr 'x', @legendWidth()
      .attr 'y', 0
      .attr 'width', (d) => Math.max 2, @x(@mainMetric.value d)
      .attr 'height', @barHeight
    @bars.selectAll '.component'
      .transition()
      .attr 'transform', => @trans @legendWidth() - 10, @barHeight
    @bars.selectAll '.value'
      .transition()
      .attr 'transform', (d) => @trans @legendWidth() + @x(@mainMetric.value d) + 5, @barHeight
    @bars.exit().remove()
    @bars.on 'click', (d) =>
      url = @options().baseUrl + encodeURIComponent d.key
      if d.qualifier == 'CLA' || d.qualifier == 'FIL'
        url += '?metric=' + encodeURIComponent @mainMetric.key
      window.location = url

    @metricLabel.attr 'transform', @trans @legendWidth(), 0

    if @maxResultsReached()
      @maxResultsReachedLabel.attr 'transform', (@trans @legendWidth(), @height() - @margin().bottom - 3)

    super



window.SonarWidgets.Histogram = Histogram
window.SonarWidgets.Histogram.defaults =
  height: 300
  margin: { top: 4, right: 50, bottom: 4, left: 10 }
  legendWidth: 220