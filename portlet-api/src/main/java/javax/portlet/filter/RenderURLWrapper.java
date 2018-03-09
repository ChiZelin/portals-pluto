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


package javax.portlet.filter;

import javax.portlet.RenderURL;

/**
 * <div class="changed_added_3_0">
 * The <code>RenderURLWrapper</code> provides a convenient 
 * implementation of the <code>RenderURL</code> interface 
 * that can be subclassed by developers.
 * This class implements the Wrapper or Decorator pattern. 
 * Methods default to calling through to the wrapped request object.
 * </div>
 *
 * @since 3.0
 */
public class RenderURLWrapper extends PortletURLWrapper implements RenderURL {
   
   /**
    * @param wrapped   the wrapped object to set.
    * @throws java.lang.IllegalArgumentException   if the RenderURL is null.
    */
   public RenderURLWrapper(RenderURL wrapped) {
      super(wrapped);
   }
   

   /**
    * Gets the wrapped object.
    * 
    * @return the wrapped object.
    */
   public RenderURL getWrapped() {
      return (RenderURL) wrapped;
   }


   /**
    * Sets the wrapped object.
    * 
    * @param wrapped   the wrapped object to set.
    * @throws java.lang.IllegalArgumentException   if the RenderURL is null.
    */
   public void setWrapped(RenderURL wrapped) {
      super.setWrapped(wrapped);
   }

   /* (non-Javadoc)
    * @see javax.portlet.RenderURL#setFragmentIdentifier(java.lang.String)
    */
   @Override
   public void setFragmentIdentifier(String fragment) {
      ((RenderURL)wrapped).setFragmentIdentifier(fragment);
   }

   /* (non-Javadoc)
    * @see javax.portlet.RenderURL#getFragmentIdentifier()
    */
   @Override
   public String getFragmentIdentifier() {
      return ((RenderURL)wrapped).getFragmentIdentifier();
   }

}
