/*  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

/**
 * This bean encapsulates a single test result. When converted to a string or 
 * written to a writer, it generates HTML markup containing elements and IDs
 * that can be read by the TCK test driver. 
 * 
 * The TCK test driver works by accessing the page containing the test portlet,
 * clicking a link for the test if one is present, and reading the test output.
 * 
 * The test case is identified by a unique name string that contains no blanks. 
 * The test case name is used to generate ID's for the HTML result elements.
 * The following IDs are generated by this bean:
 * 
 * (TestCaseName)-success     - indicates general test case success or failure
 * (TestCaseName)-detail      - a detailed message about the test     
 * 
 */
package javax.portlet.tck.beans;

import javax.portlet.PortletURL;
import javax.portlet.tck.constants.Constants;

/**
 * Formats a link to set render parameters for a test case.
 * 
 * The test driver will look for setup links for a test case and click 
 * those before clicking the actual test case links.
 * 
 * @author nick
 */
public class TestSetupLink extends TestLink {

   /**
    * Creates an empty test result.
    */
   public TestSetupLink() {
      super();
   }

   /**
    * Creates a test link initialized according to the parameters.
    * 
    * @param tcName     test case name
    * @param url        url for the test case
    */
   public TestSetupLink(String tcName, PortletURL purl) {
      super(tcName, purl);
      actId = tcName + Constants.SETUP_ID;
   }
   
}