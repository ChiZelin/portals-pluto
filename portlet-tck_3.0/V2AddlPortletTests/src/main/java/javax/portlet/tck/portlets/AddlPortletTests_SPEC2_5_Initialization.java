/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package javax.portlet.tck.portlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.tck.beans.JSR286SpecTestCaseDetails;
import javax.portlet.tck.beans.TestResult;

import static javax.portlet.tck.beans.JSR286SpecTestCaseDetails.V2ADDLPORTLETTESTS_SPEC2_5_INITIALIZATION_INITIALIZATION6;
import static javax.portlet.tck.constants.Constants.THREADID_ATTR;

/**
 * This portlet implements several test cases for the JSR 362 TCK. The test case names are defined
 * in the /src/main/resources/xml-resources/additionalTCs.xml file. The build process will integrate
 * the test case names defined in the additionalTCs.xml file into the complete list of test case
 * names for execution by the driver.
 *
 * This is the main portlet for the test cases. If the test cases call for events, this portlet will
 * initiate the events, but not process them. The processing is done in the companion portlet
 * AddlPortletTests_SPEC2_5_Initialization_event
 * 
 * @author ahmed
 */
public class AddlPortletTests_SPEC2_5_Initialization implements Portlet {
  private static final String LOG_CLASS = AddlPortletTests_SPEC2_5_Initialization.class.getName();
  private final Logger LOGGER = Logger.getLogger(LOG_CLASS);


  @Override
  public void init(PortletConfig config) throws PortletException {
    // TODO: How do I catch this exception as PortletException? Where can I see the stack trace?
    // TODO: Might be a pluto bug. Report in JIRA if it is
    try {
      throw new RuntimeException(
          "RuntimeException from V2AddlPortletTests_SPEC2_5_Initialization_initialization6");
    } catch (Exception e) {
      LOGGER.entering(LOG_CLASS, e.toString());
    }
    throw new RuntimeException(
        "RuntimeException from V2AddlPortletTests_SPEC2_5_Initialization_initialization6");
  }

  @Override
  public void destroy() {}

  @Override
  public void processAction(ActionRequest portletReq, ActionResponse portletResp)
      throws PortletException, IOException {

    portletResp.setRenderParameters(portletReq.getParameterMap());
    long tid = Thread.currentThread().getId();
    portletReq.setAttribute(THREADID_ATTR, tid);

  }

  @Override
  public void render(RenderRequest portletReq, RenderResponse portletResp)
      throws PortletException, IOException {

    long tid = Thread.currentThread().getId();
    portletReq.setAttribute(THREADID_ATTR, tid);

    PrintWriter writer = portletResp.getWriter();

    JSR286SpecTestCaseDetails tcd = new JSR286SpecTestCaseDetails();

    // Create result objects for the tests

    /* TestCase: V2AddlPortletTests_SPEC2_5_Initialization_initialization6 */
    /* Details: "A RuntimeException thrown during initialization must be */
    /* handled as a PortletException" */
    // TODO: Might be a pluto bug. Report in JIRA if it is
    TestResult tr0 =
        tcd.getTestResultFailed(V2ADDLPORTLETTESTS_SPEC2_5_INITIALIZATION_INITIALIZATION6);
    /* TODO: implement test */
    tr0.appendTcDetail("Not implemented.");
    tr0.writeTo(writer);

  }

}
