/*
 * @(#)BezierControlPointHandle.java  1.0  23. Januar 2006
 *
 * Copyright (c) 1996-2006 by the original authors of JHotDraw
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

import static org.jhotdraw.draw.AttributeKeys.TRANSFORM;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.jhotdraw.geom.BezierPath;
import org.jhotdraw.undo.CompositeEdit;
import org.jhotdraw.util.ResourceBundleUtil;
/**
 * BezierControlPointHandle.
 *
 * @author Werner Randelshofer
 * @version 1.0 23. Januar 2006 Created.
 */
public class BezierControlPointHandle extends AbstractHandle {
    private final static Color HANDLE_FILL_COLOR = new Color(0x2020fc);
    private final static Color HANDLE_STROKE_COLOR = Color.WHITE;
    protected int index, controlPointIndex;
    private CompositeEdit edit;
    private Figure transformOwner;
    
    /** Creates a new instance. */
    public BezierControlPointHandle(BezierFigure owner, int index, int coord) {
        this(owner, index, coord, owner);
    }
    public BezierControlPointHandle(BezierFigure owner, int index, int coord, Figure transformOwner) {
        super(owner);
        this.index = index;
        this.controlPointIndex = coord;
        this.transformOwner = transformOwner;
        transformOwner.addFigureListener(this);
    }
    public void dispose() {
        super.dispose();
        transformOwner.removeFigureListener(this);
        transformOwner = null;
    }
    protected BezierFigure getBezierFigure() {
        return (BezierFigure) getOwner();
    }
    
    protected Figure getTransformOwner() {
        return transformOwner;
    }
    
    protected Point getLocation() {
        if (getBezierFigure().getNodeCount() > index) {
            Point2D.Double p = getBezierFigure().getPoint(index, controlPointIndex);
            if (TRANSFORM.get(getTransformOwner()) != null) {
                TRANSFORM.get(getTransformOwner()).transform(p, p);
            }
            return view.drawingToView(p);
        } else {
            return new Point(10,10);
        }
    }
    protected BezierPath.Node getBezierNode() {
        return getBezierFigure().getNodeCount() > index ?
            getBezierFigure().getNode(index) :
            null;
    }
    
    /**
     * Draws this handle.
     */
    public void draw(Graphics2D g) {
        BezierFigure f = getBezierFigure();
        if (f.getNodeCount() > index) {
            BezierPath.Node v = f.getNode(index);
            if (v.keepColinear && v.mask == BezierPath.C1C2_MASK &&
                    (index > 0 && index < f.getNodeCount() - 1 || f.isClosed())) {
                drawCircle(g, HANDLE_STROKE_COLOR, HANDLE_FILL_COLOR);
            } else {
                drawCircle(g, HANDLE_FILL_COLOR, HANDLE_STROKE_COLOR);
            }
            g.setColor(HANDLE_FILL_COLOR);
            
            Point2D.Double p0 = new Point2D.Double(v.x[0], v.y[0]);
            Point2D.Double pc = new Point2D.Double(v.x[controlPointIndex], v.y[controlPointIndex]);
            if (TRANSFORM.get(getTransformOwner()) != null) {
                TRANSFORM.get(getTransformOwner()).transform(p0, p0);
                TRANSFORM.get(getTransformOwner()).transform(pc, pc);
            }
            
            g.draw(new Line2D.Double(
                    view.drawingToView(p0),
                    view.drawingToView(pc)
                    ));
        }
    }
    protected Rectangle basicGetBounds() {
        Rectangle r = new Rectangle(getLocation());
        r.grow(getHandlesize() / 2, getHandlesize() / 2);
        return r;
    }
    
    
    
    public void trackStart(Point anchor, int modifiersEx) {
        view.getDrawing().fireUndoableEditHappened(edit = new CompositeEdit("Punkt verschieben"));
        Point2D.Double location = view.getConstrainer().constrainPoint(view.viewToDrawing(getLocation()));
        Point2D.Double p = view.getConstrainer().constrainPoint(view.viewToDrawing(anchor));
    }
    public void trackStep(Point anchor, Point lead, int modifiersEx) {
        BezierFigure figure = getBezierFigure();
        Point2D.Double p = view.getConstrainer().constrainPoint(view.viewToDrawing(lead));
        BezierPath.Node v = figure.getNode(index);
        fireAreaInvalidated(v);
        figure.willChange();
        
            if (TRANSFORM.get(getTransformOwner()) != null) {
                try {
                    TRANSFORM.get(getTransformOwner()).inverseTransform(p, p);
                } catch (NoninvertibleTransformException ex) {
                    ex.printStackTrace();
                }
            }
        
        if (! v.keepColinear) {
            // move control point independently
            figure.setPoint(index, controlPointIndex, p);
            
        } else {
            // move control point and opposite control point on same line
            double a = Math.PI + Math.atan2(p.y - v.y[0], p.x - v.x[0]);
            int c2 = (controlPointIndex == 1) ? 2 : 1;
            double r = Math.sqrt((v.x[c2] - v.x[0]) * (v.x[c2] - v.x[0]) +
                    (v.y[c2] - v.y[0]) * (v.y[c2] - v.y[0]));
            double sina = Math.sin(a);
            double cosa = Math.cos(a);
            
            Point2D.Double p2 = new Point2D.Double(
                    r * cosa + v.x[0],
                    r * sina + v.y[0]
                    );
            figure.setPoint(index, controlPointIndex, p);
            figure.setPoint(index, c2, p2);
        }
        figure.changed();
        fireAreaInvalidated(figure.getNode(index));
        
    }
    
    private void fireAreaInvalidated(BezierPath.Node v) {
        Rectangle2D.Double dr = new Rectangle2D.Double(v.x[0], v.y[0], 0, 0);
        for (int i=1; i < 3; i++) {
            dr.add(v.x[i], v.y[i]);
        }
        Rectangle vr = view.drawingToView(dr);
        vr.grow(getHandlesize(), getHandlesize());
        fireAreaInvalidated(vr);
    }
    
    
    public void trackEnd(Point anchor, Point lead, int modifiersEx) {
        BezierFigure figure = getBezierFigure();
        if ((modifiersEx & (InputEvent.META_DOWN_MASK | InputEvent.CTRL_DOWN_MASK  | InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)) != 0) {
            figure.willChange();
            BezierPath.Node v = figure.getNode(index);
            v.keepColinear = ! v.keepColinear;
            if (v.keepColinear) {
                // move control point and opposite control point on same line
                Point2D.Double p = figure.getPoint(index, controlPointIndex);
                double a = Math.PI + Math.atan2(p.y - v.y[0], p.x - v.x[0]);
                int c2 = (controlPointIndex == 1) ? 2 : 1;
                double r = Math.sqrt((v.x[c2] - v.x[0]) * (v.x[c2] - v.x[0]) +
                        (v.y[c2] - v.y[0]) * (v.y[c2] - v.y[0]));
                double sina = Math.sin(a);
                double cosa = Math.cos(a);
                
                Point2D.Double p2 = new Point2D.Double(
                        r * cosa + v.x[0],
                        r * sina + v.y[0]
                        );
                v.x[c2] = p2.x;
                v.y[c2] = p2.y;
            }
            figure.setNode(index, v);
            figure.changed();
        }
        view.getDrawing().fireUndoableEditHappened(edit);
    }
    public boolean isCombinableWith(Handle h) {
        if (super.isCombinableWith(h)) {
            BezierControlPointHandle that = (BezierControlPointHandle) h;
            return that.index == this.index &&
                    that.controlPointIndex == this.controlPointIndex &&
                    that.getBezierFigure().getNodeCount() ==
                    this.getBezierFigure().getNodeCount();
        }
        return false;
    }
    
    public String getToolTipText(Point p) {
        ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.draw.Labels");
        BezierPath.Node node = getBezierNode();
        if (node == null) {
            return null;
        }
        if (node.mask == BezierPath.C1C2_MASK) {
            return labels.getFormatted("bezierCubicControlHandle.tip",
                    labels.getFormatted(
                    node.keepColinear ? "bezierCubicControl.colinearControl" :
                        "bezierCubicControl.unconstrainedControl")
                        );
        } else {
            return labels.getString("bezierQuadraticControlHandle.tip");
        }
    }
}
