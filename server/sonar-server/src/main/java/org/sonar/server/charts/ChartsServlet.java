/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.charts;

import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import org.jfree.chart.encoders.KeypointPNGEncoderAdapter;
import org.sonar.api.charts.Chart;
import org.sonar.api.charts.ChartParameters;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.charts.deprecated.BarChart;
import org.sonar.server.charts.deprecated.BaseChartWeb;
import org.sonar.server.charts.deprecated.CustomBarChart;
import org.sonar.server.charts.deprecated.DeprecatedChart;
import org.sonar.server.charts.deprecated.PieChart;
import org.sonar.server.charts.deprecated.SparkLinesChart;
import org.sonar.server.platform.Platform;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Map;

public class ChartsServlet extends HttpServlet {

  private static final Logger LOG = Loggers.get(ChartsServlet.class);
  private static final long serialVersionUID = 669857447198433893L;

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    if (isDeprecatedChart(request)) {
      deprecatedDoGet(request, response);

    } else {
      ChartFactory chartFactory = Platform.getInstance().getContainer().getComponentByType(ChartFactory.class);
      Chart chart = chartFactory.getChart(request.getParameter("ck"));
      if (chart != null) {
        BufferedImage image = chart.generateImage(getParams(request));
        OutputStream out = response.getOutputStream();
        try {
          response.setContentType("image/png");
          exportAsPNG(image, out);

        } catch (Exception e) {
          LOG.error("Generating chart " + chart.getClass().getName(), e);

        } finally {
          out.close();
        }
      }
    }
  }

  private ChartParameters getParams(HttpServletRequest request) {
    Map<String, String> map = Maps.newHashMap();
    Enumeration keys = request.getParameterNames();
    while (keys.hasMoreElements()) {
      String key = (String) keys.nextElement();
      String value = request.getParameter(key);
      map.put(key, value);
    }
    return new ChartParameters(map);
  }

  private void exportAsPNG(BufferedImage image, OutputStream out) throws IOException {
    KeypointPNGEncoderAdapter encoder = new KeypointPNGEncoderAdapter();
    encoder.setEncodingAlpha(true);
    encoder.encode(image, out);
  }

  public boolean isDeprecatedChart(HttpServletRequest request) {
    String chartType = request.getParameter(BaseChartWeb.CHART_PARAM_TYPE);
    if (BaseChartWeb.BAR_CHART_HORIZONTAL.equals(chartType) || BaseChartWeb.BAR_CHART_VERTICAL.equals(chartType) || BaseChartWeb.STACKED_BAR_CHART.equals(chartType)) {
      return true;
    }
    if (BaseChartWeb.BAR_CHART_VERTICAL_CUSTOM.equals(chartType)) {
      return true;
    }
    if (BaseChartWeb.PIE_CHART.equals(chartType)) {
      return true;
    }
    if (BaseChartWeb.SPARKLINES_CHART.equals(chartType)) {
      return true;
    }
    return false;
  }

  public void deprecatedDoGet(HttpServletRequest request, HttpServletResponse response) {
    Map<String, String> params = Maps.newHashMap();
    params.put(BaseChartWeb.CHART_PARAM_TYPE, request.getParameter(BaseChartWeb.CHART_PARAM_TYPE));
    params.put(BaseChartWeb.CHART_PARAM_VALUES, request.getParameter(BaseChartWeb.CHART_PARAM_VALUES));
    params.put(BaseChartWeb.CHART_PARAM_COLORS, request.getParameter(BaseChartWeb.CHART_PARAM_COLORS));
    params.put(BaseChartWeb.CHART_PARAM_RANGEMAX, request.getParameter(BaseChartWeb.CHART_PARAM_RANGEMAX));
    params.put(BaseChartWeb.CHART_PARAM_TITLE, request.getParameter(BaseChartWeb.CHART_PARAM_TITLE));
    params.put(BaseChartWeb.CHART_PARAM_DIMENSIONS, request.getParameter(BaseChartWeb.CHART_PARAM_DIMENSIONS));
    params.put(BaseChartWeb.CHART_PARAM_CATEGORIES_AXISMARGIN_VISIBLE, request.getParameter(BaseChartWeb.CHART_PARAM_CATEGORIES_AXISMARGIN_VISIBLE));
    params.put(BaseChartWeb.CHART_PARAM_RANGEAXIS_VISIBLE, request.getParameter(BaseChartWeb.CHART_PARAM_RANGEAXIS_VISIBLE));
    params.put(BaseChartWeb.CHART_PARAM_SERIES, request.getParameter(BaseChartWeb.CHART_PARAM_SERIES));
    params.put(BaseChartWeb.CHART_PARAM_CATEGORIES, request.getParameter(BaseChartWeb.CHART_PARAM_CATEGORIES));
    params.put(BaseChartWeb.CHART_PARAM_CATEGORIES_AXISMARGIN_LOWER, request.getParameter(BaseChartWeb.CHART_PARAM_CATEGORIES_AXISMARGIN_LOWER));
    params.put(BaseChartWeb.CHART_PARAM_CATEGORIES_AXISMARGIN_UPPER, request.getParameter(BaseChartWeb.CHART_PARAM_CATEGORIES_AXISMARGIN_UPPER));
    params.put(BaseChartWeb.CHART_PARAM_SERIES_AXISMARGIN_LOWER, request.getParameter(BaseChartWeb.CHART_PARAM_SERIES_AXISMARGIN_LOWER));
    params.put(BaseChartWeb.CHART_PARAM_SERIES_AXISMARGIN_UPPER, request.getParameter(BaseChartWeb.CHART_PARAM_SERIES_AXISMARGIN_UPPER));
    params.put(BaseChartWeb.CHART_PARAM_SERIES_AXISMARGIN_TICKUNIT, request.getParameter(BaseChartWeb.CHART_PARAM_SERIES_AXISMARGIN_TICKUNIT));
    params.put(BaseChartWeb.CHART_PARAM_INSETS, request.getParameter(BaseChartWeb.CHART_PARAM_INSETS));
    params.put(BaseChartWeb.CHART_PARAM_OUTLINE_RANGEGRIDLINES_VISIBLE, request.getParameter(BaseChartWeb.CHART_PARAM_OUTLINE_RANGEGRIDLINES_VISIBLE));
    params.put(BaseChartWeb.CHART_PARAM_OUTLINE_VISIBLE, request.getParameter(BaseChartWeb.CHART_PARAM_OUTLINE_VISIBLE));

    String chartType = params.get(BaseChartWeb.CHART_PARAM_TYPE);

    DeprecatedChart chart = null;

    if (BaseChartWeb.BAR_CHART_HORIZONTAL.equals(chartType) || BaseChartWeb.BAR_CHART_VERTICAL.equals(chartType) || BaseChartWeb.STACKED_BAR_CHART.equals(chartType)) {
      chart = new BarChart(params);
    } else if (BaseChartWeb.BAR_CHART_VERTICAL_CUSTOM.equals(chartType)) {
      chart = new CustomBarChart(params);
    } else if (BaseChartWeb.PIE_CHART.equals(chartType)) {
      chart = new PieChart(params);
    } else if (BaseChartWeb.SPARKLINES_CHART.equals(chartType)) {
      chart = new SparkLinesChart(params);
    }

    if (chart != null) {
      OutputStream out = null;
      try {
        out = response.getOutputStream();
        response.setContentType("image/png");
        chart.exportChartAsPNG(out);
      } catch (Exception e) {
        LOG.error("Generating chart " + chart.getClass().getName(), e);
      } finally {
        Closeables.closeQuietly(out);
      }
    }
  }
}
