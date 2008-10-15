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
package org.apache.pluto.descriptors.portlet10;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.pluto.om.portlet.Preference;

/**
 * Persistent preference values that may be used for customization and personalization by the portlet. Used in:
 * portlet-preferences <p>Java class for preferenceType complex type. <p>The following schema fragment specifies the
 * expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="preferenceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="name" type="{http://java.sun.com/xml/ns/portlet/portlet-app_1_0.xsd}nameType"/>
 *         &lt;element name="value" type="{http://java.sun.com/xml/ns/portlet/portlet-app_1_0.xsd}valueType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="read-only" type="{http://java.sun.com/xml/ns/portlet/portlet-app_1_0.xsd}read-onlyType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "preferenceType", propOrder = { "name", "value", "readOnly" })
public class PreferenceType implements Preference
{
    @XmlElement(required = true)
    @XmlJavaTypeAdapter(value=CollapsedStringAdapter.class)
    protected String name;
    @XmlJavaTypeAdapter(value=CollapsedStringAdapter.class)
    protected List<String> value;
    @XmlElement(name = "read-only")
    protected Boolean readOnly;
    @XmlAttribute
    protected String id;

    public String getName()
    {
        return name;
    }

    public void setName(String value)
    {
        name = value;
    }

    public List<String> getValues()
    {
        if (value == null)
        {
            value = new ArrayList<String>();
        }
        return value;
    }

    public boolean isReadOnly()
    {
        return readOnly != null ? readOnly.booleanValue() : false;
    }

    public void setReadOnly(boolean value)
    {
        readOnly = value ? Boolean.TRUE : Boolean.FALSE;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String value)
    {
        id = value;
    }

    public void setValues(List<String> value)
    {
        this.value = value;
    }
}