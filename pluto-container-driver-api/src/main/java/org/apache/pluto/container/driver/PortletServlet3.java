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
package org.apache.pluto.container.driver;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.Event;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.HeaderRequest;
import javax.portlet.HeaderResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.StateAwareResponse;
import javax.portlet.UnavailableException;
import javax.servlet.DispatcherType;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.pluto.container.FilterManager;
import org.apache.pluto.container.PortletAsyncManager;
import org.apache.pluto.container.PortletContainerException;
import org.apache.pluto.container.PortletInvokerService;
import org.apache.pluto.container.PortletRequestContext;
import org.apache.pluto.container.PortletResourceRequestContext;
import org.apache.pluto.container.PortletResponseContext;
import org.apache.pluto.container.PortletWindow;
import org.apache.pluto.container.bean.processor.AnnotatedConfigBean;
import org.apache.pluto.container.bean.processor.CDIEventsStore;
import org.apache.pluto.container.bean.processor.PortletArtifactProducer;
import org.apache.pluto.container.bean.processor.PortletCDIEvent;
import org.apache.pluto.container.bean.processor.PortletInvoker;
import org.apache.pluto.container.bean.processor.PortletRequestScopedBeanHolder;
import org.apache.pluto.container.bean.processor.PortletSessionBeanHolder;
import org.apache.pluto.container.bean.processor.PortletStateScopedBeanHolder;
import org.apache.pluto.container.impl.HttpServletPortletRequestWrapper;
import org.apache.pluto.container.om.portlet.EventDefinition;
import org.apache.pluto.container.om.portlet.EventDefinitionReference;
import org.apache.pluto.container.om.portlet.PortletApplicationDefinition;
import org.apache.pluto.container.om.portlet.PortletDefinition;
import org.apache.pluto.container.om.portlet.impl.ConfigurationHolder;
import org.apache.pluto.container.om.portlet.impl.EventDefinitionImpl;
import org.apache.pluto.container.om.portlet.impl.EventDefinitionReferenceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Portlet Invocation Servlet. This servlet receives cross context requests from the the container and services the
 * portlet request for the specified method.
 * 
 * @version 1.1
 * @since 09/22/2004
 */
public class PortletServlet3 extends HttpServlet {
   private static final long  serialVersionUID = -5096339022539360365L;
   
   /** Logger. */
   private static final Logger LOG = LoggerFactory.getLogger(PortletServlet3.class);

   /**
    * Portlet name constant, needed by portlet container initializer
    */
   public static final String PORTLET_NAME     = "portlet-name";

   // Private Member Variables ------------------------------------------------
   
   /**
    * For CDI support
    */
   private AnnotatedConfigBean acb = null;
   
   @Inject private BeanManager injectedBeanmgr;

   private BeanManager beanmgr = null;
   
   /**
    * The webapp configuration
    */
   ConfigurationHolder holder;

   /**
    * The portlet name as defined in the portlet app descriptor.
    */
   private String                 portletName;

   /**
    * The portlet instance wrapped by this servlet.
    */
   private PortletInvoker invoker = null;

   /**
    * The internal portlet context instance.
    */
   private DriverPortletContext   portletContext;

   /**
    * The internal portlet config instance.
    */
   private DriverPortletConfig    portletConfig;
   private boolean isOutOfService = false;
   private PortletContextService  contextService;

   private boolean                started = false;
   Timer                          startTimer;
   
   private static final QName CDI_EVENT_QNAME = new QName("javax.portlet.cdi.event", "javax.portlet.cdi.event");

   // HttpServlet Impl --------------------------------------------------------

   public String getServletInfo() {
      return "Pluto PortletServlet3 [" + portletName + "]";
   }

   /**
    * Initialize the portlet invocation servlet.
    * 
    * @throws ServletException
    *            if an error occurs while loading portlet.
    */
   public void init(ServletConfig config) throws ServletException {

      // Call the super initialization method.
      super.init(config);
      
      // Retrieve portlet name as defined as an initialization parameter.
      portletName = getInitParameter(PORTLET_NAME);
      
      // look up the bean manager to get a properly initialized one
      // (Fix for running on Liberty)

      BeanManager ibm = null;
      try {
         InitialContext context = new InitialContext();
         
         try {
            // "regular" naming
            ibm = (BeanManager) context.lookup("java:comp/BeanManager");
         } catch (NameNotFoundException e) {
            // try again with Tomcat naming
            try {
               ibm = (BeanManager) context.lookup("java:comp/env/BeanManager");
            } catch (Throwable t) {}
         }
         
      } catch (Throwable e) {}

      if (ibm != null) {
         LOG.debug("BeanManager by JNDI lookup, portlet name: " + portletName);
         beanmgr = ibm;
      } else {
         LOG.debug("BeanManager by injection, portlet name: " + portletName);
         beanmgr = injectedBeanmgr;
      }
      
      if (beanmgr != null) {
         try {
            Set<Bean<?>> beans = beanmgr.getBeans(AnnotatedConfigBean.class);
            Bean<?> bean = beanmgr.resolve(beans);
            acb = (AnnotatedConfigBean) beanmgr.getReference(bean, bean.getBeanClass(), beanmgr.createCreationalContext(bean));
            LOG.debug("ACB instance: " + acb + ", RS config: " + ((acb==null) ? "null" : acb.getSessionScopedConfig()));
            acb.getSessionScopedConfig().activate(beanmgr);
            acb.getStateScopedConfig().activate(beanmgr);
         } catch (Throwable t) {
            LOG.debug("Could not retrieve annotated config bean.");
         }
      }
      
      // Get the config bean, instantiate the portlets, and create the invoker
      holder = (ConfigurationHolder) config.getServletContext().getAttribute(ConfigurationHolder.ATTRIB_NAME);
      try {
         if (holder == null || holder.getMethodStore() == null) {
            LOG.error("Could not obtain configuration bean for portlet " + portletName + ". Exiting.");
            return;
         } else {
            holder.instantiatePortlets(beanmgr);
            invoker = new PortletInvoker(holder.getMethodStore(), portletName);
            LOG.debug("Created the portlet invoker for portlet: " + portletName);
         }
      } catch(Exception e) {
         StringBuilder txt = new StringBuilder(128);
         txt.append("Exception obtaining configuration bean for portlet ");
         txt.append(portletName).append(". Exiting. Exception: ");

         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         e.printStackTrace(pw);
         pw.flush();
         txt.append(sw.toString());
         
         LOG.error(txt.toString());

         // take out of service
         
         invoker = null;
         portletConfig = null;
         isOutOfService = true;
         return;
      }

      started = false;

      startTimer = new Timer(true);
      final ServletContext servletContext = getServletContext();
      final ClassLoader paClassLoader = Thread.currentThread().getContextClassLoader();
      startTimer.schedule(new TimerTask() {
         public void run() {
            synchronized (servletContext) {
               if (startTimer != null) {
                  if (attemptRegistration(servletContext, paClassLoader)) {
                     startTimer.cancel();
                     startTimer = null;
                  }
               }
            }
         }
      }, 1, 10000);
   }

   protected boolean attemptRegistration(ServletContext context, ClassLoader paClassLoader) {
      if (PlutoServices.getServices() != null) {
         contextService = PlutoServices.getServices().getPortletContextService();
         try {
            ServletConfig sConfig = getServletConfig();
            if (sConfig == null) {
               String msg = "Problem obtaining servlet configuration(getServletConfig() returns null).";
               context.log(msg);

               // take out of service
               
               invoker = null;
               portletConfig = null;
               isOutOfService = true;
               return true;
            }
            
            String applicationName = contextService.register(sConfig);
            started = true;
            portletContext = contextService.getPortletContext(applicationName);
            portletConfig = contextService.getPortletConfig(applicationName, portletName);
            
            // Get the portlet application definition
            //PortletApplicationDefinition apd = portletContext.getPortletApplicationDefinition();
            PortletApplicationDefinition pad = holder.getPad();
            
            PortletDefinition portletDefinition =pad.getPortlet(portletName);
            
            // Make a new Portlet event to correspond to CDI event
            EventDefinition eventDefinition = new EventDefinitionImpl(CDI_EVENT_QNAME);
            eventDefinition.setQName(CDI_EVENT_QNAME);
            eventDefinition.setValueType("java.io.Serializable");
            EventDefinitionReference eventDefinitionReference = new EventDefinitionReferenceImpl(CDI_EVENT_QNAME);
            
            // Add definition of new portlet CDI event to portlet application definition
            if(pad.getEventDefinition(CDI_EVENT_QNAME)==null){
               pad.addEventDefinition(eventDefinition);
            }
            
            // Made this portlet publisher of the new portlet CDI event
            portletDefinition.addSupportedPublishingEvent(eventDefinitionReference);
            
            // Add the modified portlet definition in portlet application definition  
            
            if(!CDIEventsStore.portletAdded.containsKey(pad)){          
               // Made this portlet subscriber of the new portlet CDI event
               portletDefinition.addSupportedProcessingEvent(eventDefinitionReference);
               CDIEventsStore.portletAdded.put(pad, portletName);
            } 
            pad.addPortlet(portletDefinition);
            contextService.updatePortletConfig(portletContext, portletDefinition);
            

         } catch (PortletContainerException ex) {
            context.log(ex.getMessage(), ex);

            // take out of service
            
            invoker = null;
            portletConfig = null;
            isOutOfService = true;
            return true;
         }

         // initialize the portlet wrapped in the servlet.
         try {
            invoker.init(portletConfig);
            return true;
         } catch (Throwable ex) {
 
            StringBuilder txt = new StringBuilder(128);
            txt.append("Portlet threw exception during initialization and will be taken out of service. Portlet name: ");
            txt.append(portletName).append(". Exiting. Exception: ");

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            pw.flush();
            txt.append(sw.toString());
            
            LOG.error(txt.toString());

            // take out of service
            
            invoker = null;
            portletConfig = null;
            isOutOfService = true;
            return true;
         }
      }
      return false;
   }

   public void destroy() {
      synchronized (getServletContext()) {
         if (startTimer != null) {
            startTimer.cancel();
            startTimer = null;
         } else if (started && portletContext != null) {
            started = false;
            contextService.unregister(portletContext);
            if (invoker != null) {
               try {
                  invoker.destroy();
               } catch (Exception e) {
                  // ignore
               }
               invoker = null;
            }
         }
         super.destroy();
      }
   }

   @Override
   protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      dispatch(request, response);
   }

   @Override
   protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      dispatch(request, response);
   }

   @Override
   protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      dispatch(request, response);
   }

   // Private Methods ---------------------------------------------------------

   /**
    * Dispatch the request to the appropriate portlet methods. This method assumes that the following attributes are set
    * in the servlet request scope:
    * <ul>
    * <li>METHOD_ID: indicating which method to dispatch.</li>
    * <li>PORTLET_REQUEST: the internal portlet request.</li>
    * <li>PORTLET_RESPONSE: the internal portlet response.</li>
    * </ul>
    * 
    * @param request
    *           the servlet request.
    * @param response
    *           the servlet response.
    * @throws ServletException
    * @throws IOException
    */
   private void dispatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      if (LOG.isDebugEnabled()) {
         StringBuilder txt = new StringBuilder();
         txt.append("Processing request.");
         txt.append(" Dispatcher type: ").append(request.getDispatcherType());
         txt.append(", request URI: ").append(request.getRequestURI());
         LOG.debug(txt.toString());
      }

      // Retrieve attributes from the servlet request.
      Integer methodId = (Integer) request.getAttribute(PortletInvokerService.METHOD_ID);
      
      // check for out of service. If it's a render, display string.
      if (isOutOfService) {
         LOG.warn("Portlet is out of service. Portlet name: " + portletName);
         if (methodId == PortletInvokerService.METHOD_RENDER) {
            PrintWriter writer = response.getWriter();
            writer.write("<p>Out of service.</p>");
         }
         return;
      }

      final PortletRequest portletRequest = (PortletRequest) request
            .getAttribute(PortletInvokerService.PORTLET_REQUEST);

      final PortletResponse portletResponse = (PortletResponse) request
            .getAttribute(PortletInvokerService.PORTLET_RESPONSE);

      final PortletRequestContext requestContext = (PortletRequestContext) portletRequest
            .getAttribute(PortletInvokerService.REQUEST_CONTEXT);
      final PortletResponseContext responseContext = (PortletResponseContext) portletRequest
            .getAttribute(PortletInvokerService.RESPONSE_CONTEXT);

      final FilterManager filterManager = 
            (FilterManager) request.getAttribute(PortletInvokerService.FILTER_MANAGER);
      filterManager.setBeanManager(beanmgr);

      if (LOG.isTraceEnabled()) {
         StringBuilder txt = new StringBuilder(128);
         txt.append("\nRequest wrapper stack: ");
         ServletRequest wreq = request;
         ServletRequest tstreq = requestContext.getServletRequest();
         int n = 1;
         while (wreq instanceof ServletRequestWrapper) {
            txt.append("\nLevel ").append(n++).append(": ").append(wreq.getClass().getCanonicalName());
            txt.append(", dispatch type: ").append(wreq.getDispatcherType());
            txt.append(", equal to req context req: ").append(wreq == tstreq);
            wreq = ((ServletRequestWrapper) wreq).getRequest();
         }
         txt.append("\nLevel ").append(n++).append(": ").append(wreq.getClass().getCanonicalName());
         txt.append(", dispatch type: ").append(wreq.getDispatcherType());
         txt.append(", equal to req context req: ").append(wreq == tstreq);

         txt.append("\n\nResponse wrapper stack: ");
         ServletResponse wresp = response;
         ServletResponse tstresp = requestContext.getServletResponse();
         n = 1;
         while (wresp instanceof ServletResponseWrapper) {
            txt.append("\nLevel ").append(n++).append(": ").append(wresp.getClass().getCanonicalName());
            txt.append(", equal to req context resp: ").append(wresp == tstresp);
            wresp = ((ServletResponseWrapper) wresp).getResponse();
         }
         txt.append("\nLevel ").append(n++).append(": ").append(wresp.getClass().getCanonicalName());
         txt.append(", equal to req context resp: ").append(wresp == tstresp);
         LOG.debug(txt.toString());
      }
      
      if (request.getDispatcherType() == DispatcherType.ASYNC) {
         
         // have to reinitialize the request context with the request under our wrapper.
         
         ServletRequest wreq = request;
         while ((wreq instanceof ServletRequestWrapper) &&
               !(wreq instanceof HttpServletPortletRequestWrapper) ) {
            wreq = ((ServletRequestWrapper) wreq).getRequest();
         }
         
         if (wreq instanceof HttpServletPortletRequestWrapper) {
            
            HttpServletRequest hreq = (HttpServletRequest) ((HttpServletPortletRequestWrapper) wreq).getRequest();
            HttpServletResponse hresp = requestContext.getServletResponse();
            
            LOG.debug("Extracted wrapped request. Dispatch type: " + hreq.getDispatcherType());

            requestContext.init(portletConfig, getServletContext(), hreq, hresp, responseContext);
            requestContext.setAsyncServletRequest(request);       // store original request
            responseContext.init(hreq, hresp);
            
         } else {
            LOG.debug("Couldn't find the portlet async wrapper.");
         }

         // enable contextual support for async
         ((PortletResourceRequestContext)requestContext).getPortletAsyncContext().registerContext(false);
         
      } else {
         
         // Not an async dispatch
         
         requestContext.init(portletConfig, getServletContext(), request, response, responseContext);
         requestContext.setExecutingRequestBody(true);
         responseContext.init(request, response);

         // enable contextual support
         beforeInvoke(portletRequest, portletResponse, portletConfig);
      
      }

      PortletWindow window = requestContext.getPortletWindow();

      PortletInvocationEvent event = new PortletInvocationEvent(portletRequest, window, methodId.intValue());
      notify(event, true, null);
      
      try {
         
         // The requested method is RENDER: call Portlet.render(..)
         if (methodId == PortletInvokerService.METHOD_RENDER) {
            RenderRequest renderRequest = (RenderRequest) portletRequest;
            String rh = requestContext.getRenderHeaders();
            if (rh != null) {
               renderRequest.setAttribute(PortletRequest.RENDER_PART, rh);
            }
            RenderResponse renderResponse = (RenderResponse) portletResponse;
            filterManager.processFilter(renderRequest, renderResponse, invoker, portletContext);
         }

         // The requested method is HEADER: call
         // HeaderPortlet.renderHeaders(..)
         else if (methodId == PortletInvokerService.METHOD_HEADER) {
            HeaderRequest headerRequest = (HeaderRequest) portletRequest;
            HeaderResponse headerResponse = (HeaderResponse) portletResponse;
            filterManager.processFilter(headerRequest, headerResponse, invoker, portletContext);
         }

         // The requested method is RESOURCE: call
         // ResourceServingPortlet.serveResource(..)
         else if (methodId == PortletInvokerService.METHOD_RESOURCE) {
            ResourceRequest resourceRequest = (ResourceRequest) portletRequest;

            // if pageState != null, we're dealing with a Partial Action request, so
            // store the page state string as a request attribute
            PortletResourceRequestContext rc = (PortletResourceRequestContext) requestContext;

            rc.setBeanManager(beanmgr);

            ResourceResponse resourceResponse = (ResourceResponse) portletResponse;
            filterManager.processFilter(resourceRequest, resourceResponse, invoker, portletContext);
         }

         // The requested method is ACTION: call Portlet.processAction(..)
         else if (methodId == PortletInvokerService.METHOD_ACTION) {
            ActionRequest actionRequest = (ActionRequest) portletRequest;
            ActionResponse actionResponse = (ActionResponse) portletResponse;
            filterManager.processFilter(actionRequest, actionResponse, invoker, portletContext);
            // TODO: Document this
            System.out.println("We have "+CDIEventsStore.universalEventList.size()+" events in universal event list.");
            for(PortletCDIEvent newPortletCDIEvent : CDIEventsStore.universalEventList){
               Object value = newPortletCDIEvent.getData();
               if(value!=null){
                  ClassLoader cl = Thread.currentThread().getContextClassLoader();
                  Writer out = new StringWriter();
      
                  try {
                     @SuppressWarnings("rawtypes")
                     Class clazz = value.getClass();
                     System.setProperty( "com.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize", "true");
                     JAXBContext jc = JAXBContext.newInstance(clazz);
                     Marshaller marshaller = jc.createMarshaller();
                     JAXBElement<Serializable> element = new JAXBElement<Serializable>(CDI_EVENT_QNAME, clazz, (Serializable) value);
                     marshaller.marshal(element, out);
                     newPortletCDIEvent.setSerializedData(out.toString());
                     newPortletCDIEvent.setProcessing(true);
                     actionResponse.setEvent(CDI_EVENT_QNAME, out.toString());
                     // TODO: Check if the events has already been set in another portlet of the same web app
                     /*
                     
                     PortletApplicationDefinition pad = holder.getPad();
                     if(CDIEventsStore.portletAppCDIEventList.containsKey(pad)){
                        Set<PortletCDIEvent> portletAppCDIEventList = CDIEventsStore.portletAppCDIEventList.get(pad);
                        for(PortletCDIEvent existingPortletCDIEvent: portletAppCDIEventList){
                           if(!existingPortletCDIEvent.equals(newPortletCDIEvent)){
                              System.out.println("Wrongly set CDI portlet event again");
                              portletAppCDIEventList.add(newPortletCDIEvent);
                              CDIEventsStore.portletAppCDIEventList.put(pad, portletAppCDIEventList);
                              actionResponse.setEvent(CDI_EVENT_QNAME, out.toString());
                           } else {
                              System.out.println("Skipped setting CDI portlet event because its already set by another portlet in the same web app");
                           }
                        }
                     } else {
                        System.out.println("Set CDI portlet event for the first time");
                        Set<PortletCDIEvent> portletAppCDIEventList = new HashSet<PortletCDIEvent>();
                        portletAppCDIEventList.add(newPortletCDIEvent);
                        CDIEventsStore.portletAppCDIEventList.put(pad, portletAppCDIEventList);
                        actionResponse.setEvent(CDI_EVENT_QNAME, out.toString());
                     }
                     */
                     
                  } catch(Exception e) {
                     System.out.println("Error while serializing cdi event data "+e.toString());
                     e.printStackTrace();
                  } finally {
                     Thread.currentThread().setContextClassLoader(cl);
                     System.getProperties().remove("com.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize");
                  }
               }
            }
         }

         // The request method is Event: call Portlet.processEvent(..)
         else if (methodId == PortletInvokerService.METHOD_EVENT) {
            EventRequest eventRequest = (EventRequest) portletRequest;
            EventResponse eventResponse = (EventResponse) portletResponse;
            System.out.println("Calling event method of "+portletName);
            
            // check if it is my cdi event
            // then deserialize data, don't let invoker get control and fire the cdi event
            
            Event portletEvent = eventRequest.getEvent();
            if(portletEvent!=null){
               if(portletEvent.getQName().equals(CDI_EVENT_QNAME)){
                  Object value = portletEvent.getValue();
                  XMLStreamReader xml = null;
                  try {
                     if (value instanceof String) {
                        String in = (String) value;
                        xml = XMLInputFactory.newInstance().createXMLStreamReader(
                              new StringReader(in));
                     } 
                  } catch (XMLStreamException e1) {
                     System.out.println(e1.toString());
                     throw new IllegalStateException(e1);
                  } catch (FactoryConfigurationError e1) {
                     System.out.println(e1.toString());
                     throw new IllegalStateException(e1);
                  }

                  if (xml != null) {
                     try {
                        System.out.println("Universal event list size is "+CDIEventsStore.universalEventList.size());
                        for(PortletCDIEvent portletCDIEvent : CDIEventsStore.universalEventList){
                           if(portletCDIEvent.getData().equals(value)){
                              ClassLoader loader = portletContext.getClassLoader();
                              Class<? extends Serializable> clazz = loader.loadClass(
                                    portletCDIEvent.getDataType()).asSubclass(
                                    Serializable.class);
               
                              JAXBContext jc = JAXBContext.newInstance(clazz);
                              Unmarshaller unmarshaller = jc.createUnmarshaller();
               
                              JAXBElement result = unmarshaller.unmarshal(xml, clazz);
               
                              try{
                                    System.out.println("Now firing event from bean manager");
                                    // TODO: Check if the event has already been fired before
                                    //       by another portlet of the same web app
                                    CDIEventsStore.firedFromBeanManager=true;
                                    beanmgr.fireEvent(result.getValue(), portletCDIEvent.getQualifiers());
                                    CDIEventsStore.firedFromBeanManager=false;
                              } catch (Exception e){
                                 e.printStackTrace();
                              }
                           }
                        }
                        /*
                        boolean contains = false;
                        for(String portletName : CDIEventsStore.recieverPortlets){
                           if(this.portletName.equals(portletName)){
                              contains = true;
                              break;
                           }
                        }
                        if(!contains){
                           for(PortletCDIEvent portletCDIEvent : CDIEventsStore.universalEventList){
                              if(portletCDIEvent.getData().equals(value)){
                                 ClassLoader loader = portletContext.getClassLoader();
                                 Class<? extends Serializable> clazz = loader.loadClass(
                                       portletCDIEvent.getDataType()).asSubclass(
                                       Serializable.class);
                  
                                 JAXBContext jc = JAXBContext.newInstance(clazz);
                                 Unmarshaller unmarshaller = jc.createUnmarshaller();
                  
                                 JAXBElement result = unmarshaller.unmarshal(xml, clazz);
                  
                                 try{
                                       System.out.println("Now firing event from bean manager");
                                       // TODO: Check if the event has already been fired before
                                       //       by another portlet of the same web app
                                       CDIEventsStore.firedFromBeanManager=true;
                                       beanmgr.fireEvent(result.getValue(), portletCDIEvent.getQualifiers());
                                       PortletApplicationDefinition pad = holder.getPad();
                                       List<PortletDefinition> portlets = pad.getPortlets();
                                       for(PortletDefinition portlet : portlets){
                                          CDIEventsStore.recieverPortlets.remove(portlet.getPortletName());
                                       }
                                       if(CDIEventsStore.recieverPortlets.isEmpty()){
                                          CDIEventsStore.universalEventList.clear();
                                       }
                                       CDIEventsStore.firedFromBeanManager=false;
                                 } catch (Exception e){
                                    e.printStackTrace();
                                 }
                              }
                           }
                        }
                        */
                        /*Set<Entry<PortletApplicationDefinition, Set<PortletCDIEvent>>> portletAppCDIEventList = CDIEventsStore.portletAppCDIEventList.entrySet();
                        for(Entry<PortletApplicationDefinition, Set<PortletCDIEvent>> iterator : portletAppCDIEventList){
                           if(pad.equals(iterator.getKey())){
                              for(PortletCDIEvent portletCDIEvent : iterator.getValue()){
                                 if(portletCDIEvent.getData().equals(value)){
                                    contains = true;
                                    break;
                                 }
                              }
                              if(contains){
                                 break;
                              }
                           }
                        }
                        */
                        
                       
                     } catch (JAXBException e) {
                        System.out.println(e.toString());
                        throw new IllegalStateException(e);
                     } catch (ClassCastException e) {
                        System.out.println(e.toString());
                        throw new IllegalStateException(e);
                     } catch (ClassNotFoundException e) {
                       System.out.println(e.toString());
                        throw new IllegalStateException(e);
                     }
                  }
                  
                  
               } else {
                  filterManager.processFilter(eventRequest, eventResponse, invoker, portletContext);
                  for(PortletCDIEvent newPortletCDIEvent : CDIEventsStore.universalEventList){
                     if(!newPortletCDIEvent.isProcessing()){
                        Object value = newPortletCDIEvent.getData();
                        if(value!=null){
                           ClassLoader cl = Thread.currentThread().getContextClassLoader();
                           Writer out = new StringWriter();
               
                           try {
                              @SuppressWarnings("rawtypes")
                              Class clazz = value.getClass();
                              System.setProperty( "com.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize", "true");
                              JAXBContext jc = JAXBContext.newInstance(clazz);
                              Marshaller marshaller = jc.createMarshaller();
                              JAXBElement<Serializable> element = new JAXBElement<Serializable>(CDI_EVENT_QNAME, clazz, (Serializable) value);
                              marshaller.marshal(element, out);
                              newPortletCDIEvent.setSerializedData(out.toString());
                              newPortletCDIEvent.setProcessing(true);
                              eventResponse.setEvent(CDI_EVENT_QNAME, out.toString());
                              // TODO: Check if the events has already been set in another portlet of the same web app
                              /*
                              
                              PortletApplicationDefinition pad = holder.getPad();
                              if(CDIEventsStore.portletAppCDIEventList.containsKey(pad)){
                                 Set<PortletCDIEvent> portletAppCDIEventList = CDIEventsStore.portletAppCDIEventList.get(pad);
                                 for(PortletCDIEvent existingPortletCDIEvent: portletAppCDIEventList){
                                    if(!existingPortletCDIEvent.equals(newPortletCDIEvent)){
                                       System.out.println("Wrongly set CDI portlet event again");
                                       portletAppCDIEventList.add(newPortletCDIEvent);
                                       CDIEventsStore.portletAppCDIEventList.put(pad, portletAppCDIEventList);
                                       actionResponse.setEvent(CDI_EVENT_QNAME, out.toString());
                                    } else {
                                       System.out.println("Skipped setting CDI portlet event because its already set by another portlet in the same web app");
                                    }
                                 }
                              } else {
                                 System.out.println("Set CDI portlet event for the first time");
                                 Set<PortletCDIEvent> portletAppCDIEventList = new HashSet<PortletCDIEvent>();
                                 portletAppCDIEventList.add(newPortletCDIEvent);
                                 CDIEventsStore.portletAppCDIEventList.put(pad, portletAppCDIEventList);
                                 actionResponse.setEvent(CDI_EVENT_QNAME, out.toString());
                              }
                              */
                              
                           } catch(Exception e) {
                              System.out.println("Error while serializing cdi event data "+e.toString());
                              e.printStackTrace();
                           } finally {
                              Thread.currentThread().setContextClassLoader(cl);
                              System.getProperties().remove("com.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize");
                           }
                        }
                     }
                  }
               }
            } else {
               System.out.println("Portlet Event is null.");
            }
         }
         // The requested method is ADMIN: call handlers.
         else if (methodId == PortletInvokerService.METHOD_ADMIN) {
            PortalAdministrationService pas = PlutoServices.getServices().getPortalAdministrationService();

            for (AdministrativeRequestListener l : pas.getAdministrativeRequestListeners()) {
               l.administer(portletRequest, portletResponse);
            }
         }

         // The requested method is LOAD: do nothing.
         else if (methodId == PortletInvokerService.METHOD_LOAD) {
            // Do nothing.
         }

         notify(event, false, null);

      } catch (UnavailableException ex) {
         //
         // if (e.isPermanent()) { throw new
         // UnavailableException(e.getMessage()); } else { throw new
         // UnavailableException(e.getMessage(), e.getUnavailableSeconds());
         // }
         //

         // Portlet.destroy() isn't called by Tomcat, so we have to fix it.
         try {
            invoker.destroy();
         } catch (Throwable th) {
            // Don't care for Exception
            this.getServletContext().log("Error during portlet destroy.", th);
         }
         
         // take portlet out of service
         isOutOfService = true;

         throw new javax.servlet.UnavailableException(ex.getMessage());

      } catch (PortletException ex) {
         notify(event, false, ex);
         throw new ServletException(ex);

      } finally {
         
         requestContext.setExecutingRequestBody(false);
         
         // If an async request is running or has been dispatched, resources
         // will be released by the PortletAsyncListener. Otherwise release here.
         
         if (!request.isAsyncStarted() && (request.getDispatcherType() != DispatcherType.ASYNC)) {

            LOG.debug("Async not being processed, releasing resources. executing req body: " + requestContext.isExecutingRequestBody());

            request.removeAttribute(PortletInvokerService.METHOD_ID);
            request.removeAttribute(PortletInvokerService.PORTLET_REQUEST);
            request.removeAttribute(PortletInvokerService.PORTLET_RESPONSE);
            request.removeAttribute(PortletInvokerService.FILTER_MANAGER);

            afterInvoke(portletResponse);

         } else {
            LOG.debug("Async started, not releasing resources. executing req body: " + requestContext.isExecutingRequestBody());

            if (requestContext instanceof PortletResourceRequestContext) {
               PortletResourceRequestContext resctx = (PortletResourceRequestContext)requestContext;
               PortletAsyncManager pac = resctx.getPortletAsyncContext();
               if (pac != null) {
                  pac.deregisterContext(false);
                  pac.launchRunner();
               } else {
                  LOG.warn("Couldn't get portlet async context.");
               }
            } else {
               LOG.warn("Wrong kind of request context: " + requestContext.getClass().getCanonicalName());
            }

         }
      }
   }

   protected void notify(PortletInvocationEvent event, boolean pre, Throwable e) {
      PortalAdministrationService pas = PlutoServices.getServices().getPortalAdministrationService();

      for (PortletInvocationListener listener : pas.getPortletInvocationListeners()) {
         if (pre) {
            listener.onBegin(event);
         } else if (e == null) {
            listener.onEnd(event);
         } else {
            listener.onError(event, e);
         }
      }
   }


   /**
    * To be called before bean method invocation begins
    */
   private void beforeInvoke(PortletRequest req, PortletResponse resp, PortletConfig config) {

      if (acb != null) {

         // Set the portlet session bean holder for the thread & session
         PortletRequestScopedBeanHolder.setBeanHolder();

         // Set the portlet session bean holder for the thread & session
         PortletSessionBeanHolder.setBeanHolder(req, acb.getSessionScopedConfig());

         // Set the render state scoped bean holder
         PortletStateScopedBeanHolder.setBeanHolder(req, acb.getStateScopedConfig());

         // Set up the artifact producer with request, response, and portlet config
         PortletArtifactProducer.setPrecursors(req, resp, config);
         
         if (LOG.isTraceEnabled()) {
            LOG.trace("CDI context is now set up.");
         }

      } else {
         if (LOG.isTraceEnabled()) {
            LOG.trace("CDI contextual support not available");
         }
      }      
      
   }

   /**
    * must be called after all method invocations have taken place, even if an
    * exception occurs.
    */
   private void afterInvoke(PortletResponse resp) {

      if (acb != null) {

         // Remove the portlet session bean holder for the thread
         PortletRequestScopedBeanHolder.removeBeanHolder();

         // Remove the portlet session bean holder for the thread
         PortletSessionBeanHolder.removeBeanHolder();

         // Remove the render state bean holder. pass response if we're
         // dealing with a StateAwareResponse. The response is used for state
         // storage.

         StateAwareResponse sar = null;
         if (resp instanceof StateAwareResponse) {
            sar = (StateAwareResponse) resp;
         }
         PortletStateScopedBeanHolder.removeBeanHolder(sar);

         // remove the portlet artifact producer
         PortletArtifactProducer.remove();
         
         if (LOG.isTraceEnabled()) {
            LOG.trace("CDI context is now deactivated.");
         }
      
      }
   }
}
