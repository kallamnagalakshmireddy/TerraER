/*
 * @(#)CSSRule.java  1.0  6. Juni 2006
 *
 * Copyright (c) 1996-2007 by the original authors of JHotDraw
 * and all its contributors ("JHotDraw.org")
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * JHotDraw.org ("Confidential Information"). You shall not disclose
 * such Confidential Information and shall use it only in accordance
 * with the terms of the license agreement you entered into with
 * JHotDraw.org.
 *
 * Original code taken from article "Swing and CSS" by Joshua Marinacci 10/14/2003
 * http://today.java.net/pub/a/today/2003/10/14/swingcss.html
 */

package org.jhotdraw.xml.css;

import java.util.HashMap;
import java.util.Map;

import net.n3.nanoxml.IXMLElement;

import org.w3c.dom.Element;

/**
 * CSSRule matches on a CSS selector.
 * <p>
 * Supported selectors:
 * <ul>
 * <li><code>*</code> matches all objects.</li>
 * <li><code>name</code> matches an element name.</li>
 * <li><code>.name</code> matches the value of the attribute "class".</li>
 * <li><code>#name</code> matches the value of the attribute "id".</li>
 * </ul>
 * This class supports net.n3.nanoxml as well as org.w3c.dom.
 *
 * @author Werner Randelshofer
 * @version 1.0 6. Juni 2006 Created.
 */
public class CSSRule {
    private String selector;
    private static enum SelectorType {
        ALL, ELEMENT_NAME, CLASS_ATTRIBUTE, ID_ATTRIBUTE
    }
    private SelectorType type;
    protected Map<String,String> properties;
    
    public CSSRule(String name, String value) {
        properties = new HashMap();
        properties.put(name, value);
    }
    public CSSRule(String selector, String propertyName, String propertyValue) {
        setSelector(selector);
        properties = new HashMap();
        properties.put(propertyName, propertyValue);
    }
    public CSSRule(String selector, Map<String,String> properties) {
        setSelector(selector);
        this.properties = properties;
    }
    
    public void setSelector(String selector) {
        switch (selector.charAt(0)) {
            case '*' : type = SelectorType.ALL; break;
            case '.' : type = SelectorType.CLASS_ATTRIBUTE; break;
            case '#' : type = SelectorType.ID_ATTRIBUTE; break;
            default : type = SelectorType.ELEMENT_NAME; break;
        }
        this.selector = (type == SelectorType.ELEMENT_NAME) ? selector : selector.substring(1);
    }
    
    public boolean matches(Element elem) {
        boolean isMatch = false;
        switch (type) {
            case ALL :
                isMatch = true;
                break;
            case ELEMENT_NAME : {
                String name = elem.getLocalName();
                isMatch = name.equals(selector);
                break;
            }
            case CLASS_ATTRIBUTE : {
                String value = elem.getAttribute("class");
                if (value != null) {
                    String[] clazzes = value.split(" ");
                    for (String clazz : clazzes) {
                        if (clazz.equals(selector)) {
                            isMatch = true;
                            break;
                        }
                    }
                }
                break;
            }
            case ID_ATTRIBUTE : {
                String name = elem.getAttribute("id");
                isMatch = name != null && name.equals(selector);
                break;
            }
        }
        return isMatch;
    }
    public boolean matches(IXMLElement elem) {
        boolean isMatch = false;
        switch (type) {
            case ALL :
                isMatch = true;
                break;
            case ELEMENT_NAME : {
                String name = elem.getName();
                isMatch = name != null && name.equals(selector);
                break;
            }
            case CLASS_ATTRIBUTE : {
                String value = elem.getAttribute("class");
                if (value != null) {
                    String[] clazzes = value.split(" ");
                    for (String clazz : clazzes) {
                        if (clazz.equals(selector)) {
                            isMatch = true;
                            break;
                        }
                    }
                }
                break;
            }
            case ID_ATTRIBUTE : {
                String name = elem.getAttribute("id");
                isMatch = name != null && name.equals(selector);
                break;
            }
        }
        return isMatch;
    }
    
    public void apply(Element elem) {
        for (Map.Entry<String,String> property : properties.entrySet()) {
            if (! elem.hasAttribute(property.getKey())) {
                elem.setAttribute(property.getKey(), property.getValue());
            }
        }
    }
    public void apply(IXMLElement elem) {
        for (Map.Entry<String,String> property : properties.entrySet()) {
            if (! elem.hasAttribute(property.getKey())) {
                elem.setAttribute(property.getKey(), property.getValue());
            }
        }
    }
    
    
    public String toString() {
        return "CSSRule["+selector+properties+"]";
    }
}