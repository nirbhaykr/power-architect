package ca.sqlpower.architect.layout;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractLayout implements ArchitectLayout {

    protected Rectangle frame;

    public void setup(Collection<? extends LayoutNode> nodes, Collection<? extends LayoutEdge> edges, Rectangle rect) {
        frame = rect;
    }

    HashMap<String, Object> properties;

    protected AbstractLayout() {
        properties = new HashMap<String, Object>();
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);

    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public Dimension getNewArea(List<? extends LayoutNode> nodes) {
        Dimension d = new Dimension();
        long area = 0;
        for (LayoutNode tp : nodes) {
            Rectangle b = tp.getBounds();
            area += b.width * b.height;
        }

        final double areaFudgeFactor = 16.0;
        double newWidth = Math.sqrt((11.0 / 8.5) * area * areaFudgeFactor);
        double newHeight = (8.5 / 11.0) * newWidth;
        d.setSize((int) newWidth, (int) newHeight);
        return d;
    }

}