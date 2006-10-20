/*
 * Copyright 2006 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * This source code implements specifications defined by the Java
 * Community Process. In order to remain compliant with the specification
 * DO NOT add / change / or delete method signatures!
 */
/*
 * Copyright 2006 IBM Corporation.
 */
package javax.portlet;

/**
 * The <CODE>BaseUIRL</CODE> defines the basic capabilities
 * of a portlet URL pointing back to the portlet.
 *
 * @since 2.0
 */
public interface BaseURL {

    /**
     * Sets the given String parameter to this URL. 
     * <p>
     * This method replaces all parameters with the given key.
     * <p>
     * The <code>PortletURL</code> implementation 'x-www-form-urlencoded' encodes
     * all  parameter names and values. Developers should not encode them.
     * <p>
     * A portlet container may prefix the attribute names internally 
     * in order to preserve a unique namespace for the portlet.
     * <p>
     * A parameter value of <code>null</code> indicates that this
     * parameter should be removed.
     *
     * @param   name
     *          the parameter name
     * @param   value
     *          the parameter value
     *
     * @exception  java.lang.IllegalArgumentException 
     *                            if name is <code>null</code>.
     */

    public void setParameter (String name, String value);


    /**
     * Sets the given String array parameter to this URL. 
     * <p>
     * This method replaces all parameters with the given key.
     * <p>
     * The <code>PortletURL</code> implementation 'x-www-form-urlencoded' encodes
     * all  parameter names and values. Developers should not encode them.
     * <p>
     * A portlet container may prefix the attribute names internally 
     * in order to preserve a unique namespace for the portlet.
     *
     * @param   name
     *          the parameter name
     * @param   values
     *          the parameter values
     *
     * @exception  java.lang.IllegalArgumentException 
     *                            if name or values are <code>null</code>.
     */

    public void setParameter (String name, String[] values);


    /**
     * Sets a parameter map for this URL.
     * <p>
     * All previously set parameters are cleared.
     * <p>
     * The <code>PortletURL</code> implementation 'x-www-form-urlencoded' encodes
     * all  parameter names and values. Developers should not encode them.
     * <p>
     * A portlet container may prefix the attribute names internally, 
     * in order to preserve a unique namespace for the portlet.
     *
     * @param  parameters   Map containing parameter names for 
     *                      the render phase as 
     *                      keys and parameter values as map 
     *                      values. The keys in the parameter
     *                      map must be of type String. The values 
     *                      in the parameter map must be of type
     *                      String array (<code>String[]</code>).
     *
     * @exception   java.lang.IllegalArgumentException 
     *                      if parameters is <code>null</code>, if
     *                      any of the key/values in the Map are <code>null</code>, 
     *                      if any of the keys is not a String, or if any of 
     *                      the values is not a String array.
     */

    public void setParameters(java.util.Map parameters);


    /**
     * Indicated the security setting for this URL. 
     * <p>
     * Secure set to <code>true</code> indicates that the portlet requests
     * a secure connection between the client and the portlet window for
     * this URL. Secure set to <code>false</code> indicates that the portlet 
     * does not need a secure connection for this URL. If the security is not
     * set for a URL, it will stay the same as the current request. 
     *
     * @param  secure  true, if portlet requests to have a secure connection
     *                 between its portlet window and the client; false, if
     *                 the portlet does not require a secure connection.
     *
     * @throws PortletSecurityException  if the run-time environment does
     *                                   not support the indicated setting
     */

    public void setSecure (boolean secure) throws PortletSecurityException;

    /**
     * Returns the portlet URL string representation to be embedded in the
     * markup.<br>
     * Note that the returned String may not be a valid URL, as it may
     * be rewritten by the portal/portlet-container before returning the 
     * markup to the client.
     *
     * @return   the encoded URL as a string
     */

    public String toString ();
}
