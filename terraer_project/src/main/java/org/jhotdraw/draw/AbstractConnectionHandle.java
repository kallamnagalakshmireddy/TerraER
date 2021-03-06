/*
 * @(#)AbstractConnectionHandle.java  3.0  2007-05-18
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
 */

package org.jhotdraw.draw;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.jhotdraw.geom.BezierPath;
import org.jhotdraw.util.ResourceBundleUtil;
/**
 * AbstractConnectionHandle factors the common code for handles
 * that can be used to change the connection of a ConnectionFigure.
 *
 * XXX - Undo/Redo is not implemented yet.
 *
 * @author Werner Randelshofer
 * @version 3.0 2007-05-18 Changed due to changes in the canConnect methods
 * of the ConnectionFigure interface. Shortened the name from
 * AbstractChangeConnectionHandle to AbstractConnectionHandle.
 * <br>2.1 2006-02-16 Remove savedLiner from connection while tracking.
 * <br>2.0 2006-01-14 Changed to support double coordinates.
 * <br>1.0 2003-12-01 Derived from JHotDraw 5.4b1.
 * @see ChangeConnectionEndHandle
 * @see ChangeConnectionStartHandle
 */
public abstract class AbstractConnectionHandle extends AbstractHandle {
    private Connector         savedTarget;
    @SuppressWarnings("unused")//anota��o inserida por Terra
    private Connector            connectableConnector; 
    private Figure            connectableFigure;
    @SuppressWarnings("unused")//anota��o inserida por Terra
    private Point             start;
    /**
     * We temporarily remove the Liner from the connection figure, while the
     * handle is being moved.
     * We store the Liner here, and add it back when the user has finished
     * the interaction.
     */
    private Liner   savedLiner;
    /**
     * All connectors of the connectable Figure.
     */
    protected Collection<Connector> connectors = Collections.emptyList();
    
    
    /**
     * Initializes the change connection handle.
     */
    protected AbstractConnectionHandle(ConnectionFigure owner) {
        super(owner);
    }
    
    public ConnectionFigure getOwner() {
        return (ConnectionFigure) super.getOwner();
    }
    
    public boolean isCombinableWith(Handle handle) {
        return false;
    }
    /**
     * Returns the connector of the change.
     */
    protected abstract Connector getTarget();
    
    /**
     * Disconnects the connection.
     */
    protected abstract void disconnect();
    
    /**
     * Connect the connection with the given figure.
     */
    protected abstract void connect(Connector c);
    
    /**
     * Sets the location of the connectableConnector point.
     */
    protected abstract void setLocation(Point2D.Double p);
    /**
     * Returns the start point of the connection.
     */
    protected abstract Point2D.Double getLocation();
    
    /**
     * Gets the side of the connection that is unaffected by
     * the change.
     */
    protected Connector getSource() {
        if (getTarget() == getOwner().getStartConnector()) {
            return getOwner().getEndConnector();
        }
        return getOwner().getStartConnector();
    }
    
    
    /**
     * Disconnects the connection.
     */
    public void trackStart(Point anchor, int modifiersEx) {
        savedTarget = getTarget();
        start = anchor;
        savedLiner = getOwner().getLiner();
        getOwner().setLiner(null);
        //disconnect();
        fireHandleRequestSecondaryHandles();
    }
    
    /**
     * Finds a new connectableConnector of the connection.
     */
    public void trackStep(Point anchor, Point lead, int modifiersEx) {
        Point2D.Double p = view.viewToDrawing(lead);
        view.getConstrainer().constrainPoint(p);
        connectableFigure = findConnectableFigure(p, view.getDrawing());
        if (connectableFigure != null) {
            Connector aTarget = findConnectionTarget(p, view.getDrawing());
            if (aTarget != null) {
                p = aTarget.getAnchor();
            }
        }
        getOwner().willChange();
        setLocation(p);
        getOwner().changed();
        repaintConnectors();
    }
    
    /**
     * Connects the figure to the new connectableConnector. If there is no
     * new connectableConnector the connection reverts to its original one.
     */
    public void trackEnd(Point anchor, Point lead, int modifiersEx) {
        ConnectionFigure f = getOwner();
        // Change node type
        if ((modifiersEx & (InputEvent.META_DOWN_MASK | InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK  | InputEvent.SHIFT_DOWN_MASK)) != 0 &&
                (modifiersEx & InputEvent.BUTTON2_DOWN_MASK) == 0) {
            f.willChange();
            int index = getBezierNodeIndex();
            BezierPath.Node v = f.getNode(index);
            if (index > 0 && index < f.getNodeCount()) {
                v.mask = (v.mask + 3) % 4;
            } else if (index == 0) {
                v.mask = ((v.mask & BezierPath.C2_MASK) == 0) ? BezierPath.C2_MASK : 0;
            } else {
                v.mask = ((v.mask & BezierPath.C1_MASK) == 0) ? BezierPath.C1_MASK : 0;
            }
            f.setNode(index, v);
            f.changed();
            fireHandleRequestSecondaryHandles();
        }
        
        Point2D.Double p = view.viewToDrawing(lead);
        view.getConstrainer().constrainPoint(p);
        Connector target = findConnectionTarget(p, view.getDrawing());
        if (target == null) {
            target = savedTarget;
        }
        
        setLocation(p);
        if (target != savedTarget) {
            disconnect();
            connect(target);
        }
        getOwner().setLiner(savedLiner);
        getOwner().updateConnection();
        connectableConnector = null;
        connectors = Collections.emptyList();
    }
    
    private Connector findConnectionTarget(Point2D.Double p, Drawing drawing) {
        Figure targetFigure = findConnectableFigure(p, drawing);
        
        if (getSource() == null && targetFigure != null) {
            return findConnector(p, targetFigure, getOwner());
        } else if (targetFigure != null) {
            Connector target = findConnector(p, targetFigure, getOwner());
            if ((targetFigure != null) && targetFigure.canConnect()
            && targetFigure != savedTarget
                    && !targetFigure.includes(getOwner())
                    && (canConnect(getSource(), target) ||
                    getTarget() != null && getTarget().getOwner() == targetFigure)) {
                return target;
            }
        }
        return null;
    }
    
    protected abstract boolean canConnect(Connector existingEnd, Connector targetEnd);
    
    protected Connector findConnector(Point2D.Double p, Figure f, ConnectionFigure prototype) {
        return f.findConnector(p, prototype);
    }
    
    /**
     * Draws this handle.
     */
    public void draw(Graphics2D g) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.transform(view.getDrawingToViewTransform());
        for (Connector c : connectors) {
            c.draw(gg);
        }
        gg.dispose();
        drawCircle(g,
                (getTarget() == null) ? Color.red : Color.green,
                Color.black
                );
    }
    
    private Figure findConnectableFigure(Point2D.Double p, Drawing drawing) {
        for (Figure f : drawing.getFiguresFrontToBack()) {
            if (! f.includes(getOwner()) && f.canConnect() && f.contains(p)) {
                return f;
            }
        }
        return null;
    }
    
    protected void setPotentialTarget(Connector newTarget) {
        this.connectableConnector = newTarget;
    }
    
    protected Rectangle basicGetBounds() {
        //if (connection.getPointCount() == 0) return new Rectangle(0, 0, getHandlesize(), getHandlesize());
        Point center = view.drawingToView(getLocation());
        return new Rectangle(center.x - getHandlesize() / 2, center.y - getHandlesize() / 2, getHandlesize(), getHandlesize());
    }
    
    protected BezierFigure getBezierFigure() {
        return (BezierFigure) getOwner();
    }
    
    abstract protected int getBezierNodeIndex();
    
    @Override final public Collection<Handle> createSecondaryHandles() {
        LinkedList<Handle> list = new LinkedList<Handle>();
        if (((ConnectionFigure) getOwner()).getLiner() == null && savedLiner == null) {
            int index = getBezierNodeIndex();
            BezierFigure f = getBezierFigure();
            BezierPath.Node v = f.getNode(index);
            if ((v.mask & BezierPath.C1_MASK) != 0 &&
                    (index != 0 || f.isClosed())) {
                list.add(new BezierControlPointHandle(f, index, 1));
            }
            if ((v.mask & BezierPath.C2_MASK) != 0 &&
                    (index < f.getNodeCount() - 1 ||
                    f.isClosed())) {
                list.add(new BezierControlPointHandle(f, index, 2));
            }
            if (index > 0 || f.isClosed()) {
                int i = (index == 0) ? f.getNodeCount() - 1 : index - 1;
                v = f.getNode(i);
                if ((v.mask & BezierPath.C2_MASK) != 0) {
                    list.add(new BezierControlPointHandle(f, i, 2));
                }
            }
            if (index < f.getNodeCount() - 2 || f.isClosed()) {
                int i = (index == f.getNodeCount() - 1) ? 0 : index + 1;
                v = f.getNode(i);
                if ((v.mask & BezierPath.C1_MASK) != 0) {
                    list.add(new BezierControlPointHandle(f, i, 1));
                }
            }
        }
        return list;
    }
    protected BezierPath.Node getBezierNode() {
        int index = getBezierNodeIndex();
        return getBezierFigure().getNodeCount() > index ?
            getBezierFigure().getNode(index) :
            null;
    }
    
    public String getToolTipText(Point p) {
        ConnectionFigure f = (ConnectionFigure) getOwner();
        if (f.getLiner() == null && savedLiner == null) {
            ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.draw.Labels");
            BezierPath.Node node = getBezierNode();
            return (node == null) ? null : labels.getFormatted("bezierNodeHandle.tip",
                    labels.getFormatted(
                    (node.getMask() == 0) ?
                        "bezierNode.linearNode" :
                        ((node.getMask() == BezierPath.C1C2_MASK) ?
                            "bezierNode.cubicNode" : "bezierNode.quadraticNode")
                            )
                            );
        } else {
            return null;
        }
    }
    /**
     * Updates the list of connectors that we draw when the user
     * moves or drags the mouse over a figure to which can connect.
     */
    public void repaintConnectors() {
        Rectangle2D.Double invalidArea = null;
        for (Connector c : connectors) {
            if (invalidArea == null) {
                invalidArea = c.getDrawingArea();
            } else {
                invalidArea.add(c.getDrawingArea());
            }
        }
        connectors = (connectableFigure == null) ?
            new java.util.LinkedList<Connector>() :
            connectableFigure.getConnectors(getOwner());
        for (Connector c : connectors) {
            if (invalidArea == null) {
                invalidArea = c.getDrawingArea();
            } else {
                invalidArea.add(c.getDrawingArea());
            }
        }
        if (invalidArea != null) {
            view.getComponent().repaint(
                    view.drawingToView(invalidArea)
                    );
        }
    }
}
