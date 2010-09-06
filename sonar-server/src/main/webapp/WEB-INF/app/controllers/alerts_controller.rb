#
# Sonar, open source software quality management tool.
# Copyright (C) 2009 SonarSource SA
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
class AlertsController < ApplicationController
  SECTION=Navigation::SECTION_CONFIGURATION

  before_filter :admin_required, :except => [ 'index' ]
  before_filter :load_profile
  
  protected
  
  def load_profile
    @profile = Profile.find(params[:profile_id])
  end
  
  public
  
  # GET /profiles/:profile_id/alerts
  # GET /profiles/:profile_id/alerts.xml
  def index
    @alerts = @profile.alerts.sort
    @alert=Alert.new
    
    respond_to do |format|
      format.html # index.html.erb
      format.xml  { render :xml => @alerts }
    end
  end

  # GET /profiles/:profile_id/alerts/1
  # GET /profiles/:profile_id/alerts/1.xml
  def show
    @alert = @profile.alerts.find(params[:id])

    respond_to do |format|
      format.html # show.html.erb
      format.xml  { render :xml => @alert }
    end
  end

  # GET /profiles/:profile_id/alerts/new
  # GET /profiles/:profile_id/alerts/new.xml
  def new
    @alert = @profile.alerts.build(params[:alert])

    respond_to do |format|
      format.js {
        render :update do |page|
         page.replace_html :new_alert_form, :partial => 'new'
        end
      }
      format.html # new.html.erb
      format.xml  { render :xml => @alert }
    end
  end

  # GET /profiles/:profile_id/alerts/edit
  def edit
    @alert = @profile.alerts.find(params[:id])
  end

  # POST /profiles/:profile_id/alerts
  # POST /profiles/:profile_id/alerts.xml
  def create
    @alert = @profile.alerts.build(params[:alert])
    
    respond_to do |format|
      if @alert.save
        flash[:notice] = 'Alert is created.'
        format.html { redirect_to profile_alerts_path(@profile) }
        format.xml  { render :xml => @alert, :status => :created, :location => profile_alert_path(profile, @alert) }
        format.js { render :update do |page|
          page.redirect_to profile_alerts_path(@profile)
        end}
      else

        @alerts = @profile.alerts.reload
        format.html { render :action => "index" }
        format.xml  { render :xml => @alert.errors, :status => :unprocessable_entity }
        format.js { render :update do |page|
          page.replace_html( 'new_alert_form', :partial => 'new')
          
        end}
      end
    end
  end

  # PUT /profiles/:profile_id/alerts/1
  # PUT /profiles/:profile_id/alerts/1.xml
  def update
    @alerts=@profile.alerts
    alert = @alerts.find(params[:id])

    respond_to do |format|
      if alert.update_attributes(params[:alert])
        flash[:notice] = 'Alert is updated.'
        format.html { redirect_to profile_alerts_path(@profile) }
        format.xml  { head :ok }
        format.js { render :update do |page| page.redirect_to profile_alerts_path(@profile) end}
      else
        @alert=Alert.new
        format.html { render :action => "index" }
        format.xml  { render :xml => @alert.errors, :status => :unprocessable_entity }
        format.js { render :update do |page| 
          page.replace_html( "row_alert_#{alert.id}", :partial => 'edit', :locals => {:alert => alert})
        end}
      end
    end
  end

  # DELETE /profiles/:profile_id/alerts/1
  # DELETE /profiles/:profile_id/alerts/1.xml
  def destroy
    @alert = @profile.alerts.find(params[:id])
    @alert.destroy
    flash[:notice] = 'Alert is deleted.'

    respond_to do |format|
      format.html { redirect_to(profile_alerts_path(@profile)) }
      format.xml  { head :ok }
    end
  end
end
