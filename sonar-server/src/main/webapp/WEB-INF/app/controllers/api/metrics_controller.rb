#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#

require "json"

class Api::MetricsController < Api::RestController

  # GETs should be safe (see http://www.w3.org/2001/tag/doc/whenToUseGet.html)
  verify :method => :put, :only => [ :update ]
  verify :method => :post, :only => [ :create ]
  verify :method => :delete, :only => [ :destroy ]

  before_filter :admin_required, :only => [ :create, :update, :destroy ]

  def index
    metrics = Metric.all
    rest_render(metrics)
  end

  def show
    metric = Metric.by_key(params[:id])
    if !metric
      rest_status_ko('Metric [' + params[:id] + '] does not exist', 404)
    else
      rest_render([metric])
    end
  end

  def create
    metric_test = Metric.find(:first, :conditions => ['name=? OR id=?', params[:id], params[:id].to_i])

    exist_and_is_disable = !metric_test.nil? && !metric_test.enabled?
    if exist_and_is_disable
      metric = metric_test
    else
      metric = Metric.new
    end

    begin
      metric.attributes = params.merge({:name => params[:id], :short_name => params[:name], :enabled => true})
      metric.origin = 'WS'
      metric.save!
      Metric.clear_cache
      rest_status_ok
    rescue
      rest_status_ko(metric.errors.full_messages.join("."), 400)
    end
  end

  def update
    metric = Metric.find(:first, :conditions => ['(name=? OR id=?) AND enabled=? AND origin<>?', params[:id], params[:id].to_i, true, Metric::ORIGIN_JAVA])
    if metric
      begin
        metric.attributes = params.merge({:name => params[:id], :short_name => params[:name], :enabled => true})
        metric.save!
        Metric.clear_cache
        rest_status_ok
      rescue
        rest_status_ko(metric.errors.full_messages.join("."), 400)
      end
    else
      rest_status_ko('Unable to update : Metric [' + params[:id] + '] does not exist', 404)
    end
  end

  def destroy
    metric = Metric.find(:first, :conditions => ['(name=? OR id=?) AND enabled=? AND origin<>?', params[:id], params[:id].to_i, true, Metric::ORIGIN_JAVA])
    if !metric
      rest_status_ko('Unable to delete : Metric [' + params[:id] + '] does not exist', 404)
    else
      metric.enabled = false
      begin
        metric.save!
        Metric.clear_cache
        rest_status_ok
      rescue
        rest_status_ko(metric.errors.full_messages.join("."), 400)
      end
    end
  end


  protected

  def rest_to_json(metrics)
    JSON(metrics.collect{|metric| metric.to_hash_json(params)})
  end

  def rest_to_xml(metrics)
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct! 
    xml.metrics do
      metrics.each do |metric|
        xml << metric.to_xml(params)
      end
    end
  end

end
