/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pluto.container;

import java.io.IOException;

import javax.portlet.EventPortlet;
import javax.portlet.Portlet;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.ResourceServingPortlet;
/**
 * Manage the initialization and doFilter for the filter which are
 * declareted in the deployment descriptor.
 * @since 05/29/2007
 * @version 2.0
 */
public interface FilterManager {
	public void processFilter(PortletRequest req, PortletResponse res, ClassLoader loader, Portlet portlet, PortletContext portletContext) throws PortletException, IOException;
	public void processFilter(PortletRequest req, PortletResponse res, ClassLoader loader, ResourceServingPortlet resourceServingPortlet, PortletContext portletContext)throws PortletException, IOException;
	public void processFilter(PortletRequest req, PortletResponse res, ClassLoader loader, EventPortlet eventPortlet, PortletContext portletContext)throws PortletException, IOException;
}