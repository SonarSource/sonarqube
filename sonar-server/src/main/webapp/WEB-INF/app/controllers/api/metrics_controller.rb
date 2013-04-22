#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

require "json"

class Api::MetricsController < Api::ApiController

  # GETs should be safe (see http://www.w3.org/2001/tag/doc/whenToUseGet.html)
  verify :method => :put, :only => [ :update ]
  verify :method => :post, :only => [ :create ]
  verify :method => :delete, :only => [ :destroy ]

  before_filter :admin_required, :only => [ :create, :update, :destroy ]

  # GET /api/metrics
  def index
    metrics = Metric.all
    respond_to do |format|
      format.json { render :json => jsonp(metrics_to_json(metrics)) }
      format.xml { render :xml => metrics_to_xml(metrics) }
    end
  end

  # GET /api/metrics/foo
  def show
    metric = Metric.by_key(params[:id])
    if !metric
      render_not_found('Metric [' + params[:id] + '] does not exist')
    else
      respond_to do |format|
        format.json { render :json => jsonp(metrics_to_json([metric])) }
        format.xml { render :xml => metrics_to_xml([metric]) }
      end
    end
  end

  # curl -u admin:admin -v -X POST http://localhost:9000/api/metrics/foo?name=bar&val_type=<type>[&description=<description>&domain=<domain>]
  def create
    metric_test = Metric.first(:conditions => ['name=?', params[:id]])

    exist_and_is_disable = !metric_test.nil? && !metric_test.enabled?
    if exist_and_is_disable
      metric = metric_test
    else
      metric = Metric.new
   end

    metric.attributes = params.merge({:name => params[:id], :short_name => params[:name]})
    metric.origin = Metric::ORIGIN_WS
    metric.user_managed = true
    metric.enabled = true
    metric.save!
    Metric.clear_cache

    respond_to do |format|
      format.json { render :json => jsonp(metrics_to_json([metric])) }
      format.xml { render :xml => metrics_to_xml([metric]) }
    end
  end

  # curl -u admin:admin -v -X PUT http://localhost:9000/api/metrics/foo?name=bar&val_type=<type>[&description=<description>&domain=<domain>]
  def update
    metric = Metric.first(:conditions => ['name=? AND enabled=? AND user_managed=?', params[:id], true, true])
    if metric
      metric.attributes = params.merge({:name => params[:id], :short_name => params[:name]})
      metric.save!
      Metric.clear_cache

      respond_to do |format|
        format.json { render :json => jsonp(metrics_to_json([metric])) }
        format.xml { render :xml => metrics_to_xml([metric]) }
      end
    else
      render_not_found('Unable to update manual metric: '+ params[:id])
    end
  end

  # curl -u admin:admin -v -X DELETE http://localhost:9000/api/metrics/foo
  def destroy
    metric = Metric.first(:conditions => ['(name=? OR id=?) AND enabled=? AND user_managed=?', params[:id], params[:id].to_i, true, true])
    if !metric
      render_not_found('Unable to delete manual metric which does not exist: '+ params[:id])
    else
      metric.enabled = false
      metric.save!
      Metric.clear_cache
      render_success('metric deleted')
    end
  end


  protected

  def metrics_to_json(metrics)
    json = []
    metrics.each do |metric|
      json << metric.to_hash_json
    end
    json
  end

  def metrics_to_xml(metrics)
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct! 
    xml.metrics do
      metrics.each do |metric|
        xml << metric.to_xml(params)
      end
    end
  end

end
