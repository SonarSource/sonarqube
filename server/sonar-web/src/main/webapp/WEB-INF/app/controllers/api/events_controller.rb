#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
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
class Api::EventsController < Api::ApiController

  # GET /api/events?resource=xxx&category=xxx
  # Examples :
  #   - global events :   curl -v http://localhost:9000/api/events -u admin:admin
  #   - resource events : curl -v http://localhost:9000/api/events?resource=org.apache.struts:struts-parent
  #
  def index
    begin
      load_resource(:user, params[:resource])

      conditions=[]
      values={}

      if params[:categories]
        conditions<<'category IN (:categs)'
        values[:categs]=params[:categories].split(',')
      end
      
      if @resource
        conditions<<'component_uuid=:component_uuid'
        values[:component_uuid]=@resource.uuid
      else
        conditions<<'component_uuid IS NULL'
      end

      from=nil
      if params[:fromDateTime]
        from=parse_datetime(params[:fromDateTime], false)
      elsif params[:fromDate]
        from=Date::strptime(params[:fromDate])
      end
      if from
        conditions<<'event_date>=:from'
        values[:from]=from.to_i*1000
      end

      to=nil
      if params[:toDateTime]
        to=parse_datetime(params[:toDateTime], false)
      elsif params[:toDate]
        to=Date::strptime(params[:toDate])
      end
      if to
        conditions<<'event_date<=:to'
        values[:to]=to.to_i*1000
      end

      events=Event.find(:all, :conditions => [conditions.join(' AND '), values], :order => 'event_date DESC')

      respond_to do |format|
        format.json { render :json => jsonp(events_to_json(events)) }
        format.xml  { render :xml => events_to_xml(events) }
        format.text { render :text => text_not_supported }
      end

    rescue ApiException => e
      render_error(e.msg, e.code)
      
    rescue Exception => e
      logger.error("Fails to execute #{request.url} : #{e.message}")
      render_error(e.message)
    end
  end

  # GET /api/events/:id
  # Example : curl -v http://localhost:9000/api/events/3
  def show
    begin
      event=Event.find(params[:id])
      load_resource_by_uuid(:user, event.component_uuid)

      respond_to do |format|
        format.json { render :json => jsonp(events_to_json([event])) }
        format.xml  { render :xml => events_to_xml([event]) }
        format.text { render :text => text_not_supported }
      end

    rescue ActiveRecord::RecordNotFound => e
      render_error(e.message, 404)

    rescue ApiException => e
      render_error(e.msg, e.code)
      
    rescue Exception => e
      logger.error("Fails to execute #{request.url} : #{e.message}")
      render_error(e.message)
    end
  end

  #
  # POST /api/events
  # Required parameters :
  # - resource (id or key) - must be a root project.
  # - name
  # - category
  #
  # Optional parameters :
  # - description
  # - dateTime (ISO format : 2010-12-25T23:59:59+0100)
  #
  # Example :
  # curl -d "name=foo&category=bar&dateTime=2010-12-25T23%3A59%3A59%2B0100" http://localhost:9000/api/events -v -u admin:admin
  #
  def create
    begin
      load_resource(:admin, params[:resource])
      raise "Resource must be a root project" unless @resource.scope=='PRJ'
      
      root_snapshot=nil
      if (params[:dateTime])
        # try to find a snapshot on that day
        date = parse_datetime(params[:dateTime], true)
        root_snapshot = Snapshot.find(:last, :conditions => ["project_id = ? AND created_at >= ? AND created_at <= ?", @resource.id, date.to_i*1000, (date + 1.day).to_i*1000], :order => :created_at)
        raise "No snapshot exists for given date" unless root_snapshot
      else
        root_snapshot = Snapshot.find(:last, :conditions => ["project_id = ?", @resource.id], :order => :created_at)
      end
      
      raise "A version already exists on this resource." if params[:category]==EventCategory::KEY_VERSION && root_snapshot.event(EventCategory::KEY_VERSION)
      raise "An event '#{params[:name]}' (category '#{params[:category]}') already exists on this resource." if Event.already_exists(@resource.last_snapshot.id, params[:name], params[:category])
      
      # Create events for the root project and every submodule
      event_to_return = nil
      name = params[:name]
      desc = params[:description]
      category = params[:category]
      snapshots = Snapshot.find(:all, :include => ['events', 'project'], :conditions => ["(root_snapshot_id = ? OR id = ?) AND scope = 'PRJ'", root_snapshot.id, root_snapshot.id])
      snapshots.each do |snapshot|
        # if this is a 'Version' event, must propagate the version number to the snapshot
        if category==EventCategory::KEY_VERSION
          snapshot.version = name
          snapshot.save!
        end
        # and then create the event linked to the updated snapshot        
        event=Event.new(
          :name => name,
          :description => desc,
          :category => category,
          :snapshot => snapshot,
          :component_uuid => snapshot.project.uuid,
          :event_date => snapshot.created_at
        )
        event.save!
        event_to_return = event if snapshot.project_id = @resource.id
      end
      
      
      respond_to do |format|
        format.json { render :json => jsonp(events_to_json([event_to_return])) }
        format.xml  { render :xml => events_to_xml([event_to_return]) }
        format.text { render :text => text_not_supported }
      end

    rescue ApiException => e
      render_error(e.msg, e.code)

    rescue Exception => e
      render_error(e.message, 400)
    end
  end


  # DELETE /api/events/1
  # Example : curl -X DELETE  http://localhost:9000/api/events/9 -v
  def destroy
    begin
      event=Event.find(params[:id])
      load_resource_by_uuid(:admin, event.component_uuid)
      
      events = []
      name = event.name
      category = event.category
      snapshots = Snapshot.find(:all, :include => 'events', :conditions => ["(root_snapshot_id = ? OR id = ?) AND scope = 'PRJ'", event.snapshot_id, event.snapshot_id])
      snapshots.each do |snapshot|
        snapshot.events.reject {|e| e.name!=name || e.category!=category}.each do |event|
          events << event
        end
      end

      Event.transaction do
        events.map { |e| e.id }.each_slice(999) do |safe_for_oracle_ids|
          Event.delete(safe_for_oracle_ids)
        end
      end
      
      render_success("Event deleted")
      
    rescue ActiveRecord::RecordNotFound => e
      render_error(e.message, 404)
      
    rescue ApiException => e
      render_error(e.msg, e.code)

    rescue Exception => e
      logger.error("Fails to execute #{request.url} : #{e.message}")
      render_error(e.message)
    end
  end


  private

  def load_resource(required_resource_role, resource_key=nil)
    if resource_key
      @resource=Project.by_key(resource_key)
      if @resource.nil?
        raise ApiException.new 404, "Resource not found: #{resource_key}"
      end

      unless has_role?(required_resource_role, @resource)
        raise ApiException.new 401, "Unauthorized"
      end
    else
      # global events
      unless is_admin?
        raise ApiException.new 401, "Unauthorized"
      end
    end
  end

  def load_resource_by_uuid(required_resource_role, component_uuid=nil)
    if component_uuid
      @resource=Project.first(:conditions => {:uuid => component_uuid})
      if @resource.nil?
        raise ApiException.new 404, "Component uuid not found: #{component_uuid}"
      end

      unless has_role?(required_resource_role, @resource)
        raise ApiException.new 401, "Unauthorized"
      end
    else
      # global events
      unless is_admin?
        raise ApiException.new 401, "Unauthorized"
      end
    end
  end

  def events_to_json(events=[])
    json=[]
    events.each do |event|
      json<<event_to_json(event)
    end
    json
  end

  def event_to_json(event)
    hash={}
    hash[:id]=event.id.to_s
    hash[:rk]=event.resource.key if event.resource
    hash[:n]=event.name if event.name
    hash[:c]=event.category
    hash[:dt]=Api::Utils.format_datetime(event.event_date) if event.event_date
    hash[:ds]=event.description if event.description
    hash
  end

  def events_to_xml(events, xml=Builder::XmlMarkup.new(:indent => 0))
    xml.events do
      events.each do |event|
        event_to_xml(event, xml)
      end
    end
  end

  def event_to_xml(event, xml=Builder::XmlMarkup.new(:indent => 0))
    xml.event do
      xml.id(event.id.to_s)
      xml.name(event.name) if event.name
      xml.resourceKey(event.resource.key) if event.resource
      xml.category(event.category)
      xml.date(Api::Utils.format_datetime(event.event_date)) if event.event_date
      xml.description(event.description) if event.description
    end
  end

end
