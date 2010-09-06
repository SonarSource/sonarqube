/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.charts.jruby;

import org.apache.commons.lang.LocaleUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;
import org.sonar.server.charts.deprecated.BaseChart;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.TreeMap;

public class TrendsChart extends BaseChart {

  private XYPlot plot;
  private TreeMap<Long, TimeSeries> seriesById;
  private int percentAxisId = -1;
  private boolean displayLegend;

  public TrendsChart(int width, int height, String localeKey, boolean displayLegend) {
    super(width, height);
    this.displayLegend = displayLegend;
    seriesById = new TreeMap<Long, TimeSeries>();
    plot = new XYPlot();
    DateAxis dateAxis = new DateAxis();
    dateAxis.setDateFormatOverride(DateFormat.getDateInstance(DateFormat.SHORT, LocaleUtils.toLocale(localeKey)));
    plot.setDomainAxis(dateAxis);
  }

  public void initSerie(Long serieId, String legend, boolean isPercent) {
    TimeSeries series = new TimeSeries(legend);

    int index=seriesById.size();
    seriesById.put(serieId, series);

    TimeSeriesCollection timeSeriesColl = new TimeSeriesCollection();
    timeSeriesColl.addSeries(series);
    plot.setDataset(index, timeSeriesColl);

    if (isPercent) {
      if (percentAxisId == -1) {
        NumberAxis rangeAxis = new NumberAxis();
        rangeAxis.setNumberFormatOverride(new DecimalFormat("0'%'"));
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setUpperBound(100.0);
        rangeAxis.setLowerBound(0.0);
        plot.setRangeAxisLocation(index, AxisLocation.TOP_OR_LEFT);
        plot.setRangeAxis(index, rangeAxis);
        plot.mapDatasetToRangeAxis(index, index);
        percentAxisId = index;

      } else {
        plot.mapDatasetToRangeAxis(index, percentAxisId);
      }
    } else {
      NumberAxis rangeAxis = new NumberAxis(displayLegend ? legend : null);
      rangeAxis.setAutoRangeIncludesZero(false);
      rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
      rangeAxis.setAutoRangeMinimumSize(2.0);
      plot.setRangeAxisLocation(index, AxisLocation.TOP_OR_RIGHT);
      plot.setRangeAxis(index, rangeAxis);
      plot.mapDatasetToRangeAxis(index, index);
    }

    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
    renderer.setBaseShapesVisible(false);
    renderer.setSeriesStroke(0, new BasicStroke(2.0f));
    renderer.setSeriesPaint(0, COLORS[index % COLORS.length]);
    plot.setRenderer(index, renderer);
  }

  public void addMeasure(Double value, Date date, Long serieId) {
    seriesById.get(serieId).addOrUpdate(new Day(date), value);
  }

  public void addLabel(Date date, String label) throws ParseException {
    addLabel(date, label, false);
  }

  public void addLabel(Date date, String label, boolean lower) throws ParseException {
    Day d = new Day(date);
    double millis = d.getFirstMillisecond();
    Marker marker = new ValueMarker(millis);
    marker.setLabel(label);
    marker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
    marker.setLabelTextAnchor(TextAnchor.TOP_LEFT);
    Color c = new Color(17, 40, 95);
    marker.setLabelPaint(c);
    marker.setPaint(c);
    marker.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 3.0f, new float[]{5f, 5f, 5f, 5f}, 2.0f));
    if (lower) {
      marker.setLabelOffset(new RectangleInsets(18, 0, 0, 5));
    }
    plot.addDomainMarker(marker);
  }

  @Override
  protected BufferedImage getChartImage() throws IOException {
    JFreeChart chart = new JFreeChart(null, TextTitle.DEFAULT_FONT, plot, true);
    configureChart(chart, displayLegend ? RectangleEdge.BOTTOM : null);
    return super.getBufferedImage(chart);
  }
}