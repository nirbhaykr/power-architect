/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.architect.swingui.olap;

import java.awt.Point;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.olap.OLAPObject;
import ca.sqlpower.architect.olap.MondrianModel.Measure;
import ca.sqlpower.architect.swingui.ASUtils;
import ca.sqlpower.architect.swingui.ContainerPane;
import ca.sqlpower.architect.swingui.PlayPen;
import ca.sqlpower.architect.swingui.PlayPenContentPane;
import ca.sqlpower.architect.swingui.PlayPenCoordinate;
import ca.sqlpower.architect.swingui.PlayPen.FloatingContainerPaneListener;
import ca.sqlpower.architect.swingui.PlayPen.MouseModeType;
import ca.sqlpower.architect.swingui.event.SelectionEvent;
import ca.sqlpower.swingui.DataEntryPanel;
import ca.sqlpower.swingui.DataEntryPanelBuilder;

/**
 * A class that provides all the generic behaviour applicable to OLAP
 * playpen components that have titles and sections of selectable items.
 *
 * @param <T> The model's type 
 * @param <C> The item type. If there are mixed item types, this will be OLAPObject.
 */
public abstract class OLAPPane<T extends OLAPObject, C extends OLAPObject> extends ContainerPane<T, C> {

    private static final Logger logger = Logger.getLogger(OLAPPane.class);
    
    /**
     * The sections of this OLAP Pane. There must always be at least one section.
     * The set of sections is allowed to change at any time, but an appropriate
     * event will be fired when it does change.
     */
    protected final List<PaneSection<C>> sections = new ArrayList<PaneSection<C>>();
    
    /**
     * Tracks which scetions in this container are currently selected.
     */
    protected final Set<PaneSection<C>> selectedSections = new HashSet<PaneSection<C>>();

    /**
     * The point where a dropped item would be inserted if the drop were
     * to happen now. This is mostly important as a visual clue for the
     * user while dragging over this pane.
     */
    private PlayPenCoordinate<T, C> insertionPoint;


    protected OLAPPane(PlayPenContentPane parent) {
        super(parent);
    }
    
    /**
     * Returns this pane's list of sections.
     */
    public List<PaneSection<C>> getSections() {
        return sections;
    }

    @Override
    public String getName() {
        return model.getName();
    }

    @Override
    @Deprecated
    public int pointToItemIndex(Point p) {
        return getUI().pointToItemIndex(p);
    }
    
    /**
     * Translates the given point into a {@link PlayPenCoordinate}.
     * 
     * @param p The point to be translated.
     * 
     * @return The PlayPenCoordinate that represents the point.
     */
    public PlayPenCoordinate<T, C> pointToPPCoordinate(Point p) {
        return getUI().pointToPPCoordinate(p);
    }
    
    @Override
    public OLAPPaneUI<T, C> getUI() {
        return (OLAPPaneUI<T, C>) super.getUI();
    }
    
    /**
     * Creates a edit dialog for the OLAPObject that is at the location
     * represented by the given coordinate.
     * 
     * @param coord
     *            Containas information about the OLAPObject that the edit
     *            dialog should be created for.
     * 
     * @return A DataEntryPanel for editting the OLAPObject, null if location is
     *         invalid.
     * @throws ArchitectException
     *             If creating an edit dialog failed.
     */
    public abstract DataEntryPanel createEditDialog(PlayPenCoordinate<T, C> coord) throws ArchitectException;
    
    @Override
    public void handleMouseEvent(MouseEvent evt) {
        PlayPen pp = getPlayPen();
        
        Point p = evt.getPoint();
        pp.unzoomPoint(p);
        p.translate(-getX(), -getY());

        PlayPenCoordinate<T, C> clickedCoor = pointToPPCoordinate(p);
        int clickedIndex = clickedCoor.getIndex();
        if (evt.getID() == MouseEvent.MOUSE_CLICKED) {
            if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1) {
                try {
                    DataEntryPanel panel = createEditDialog(clickedCoor);
                    if (panel != null) {
                        Window owner = SwingUtilities.getWindowAncestor(getPlayPen());
                        JDialog dialog = DataEntryPanelBuilder.createDataEntryPanelDialog(panel, owner,
                                "Modify Properties", "OK");
                        dialog.setLocationRelativeTo(owner);
                        dialog.setVisible(true);
                    }
                } catch (Exception e) {
                    logger.debug("Error from creating edit dialog at coordinate: " + clickedCoor, e);
                    ASUtils.showExceptionDialogNoReport(SwingUtilities.getWindowAncestor(getPlayPen()),
                            "Failed to create edit dialog!", e);
                }
            }
        } else if (evt.getID() == MouseEvent.MOUSE_PRESSED) {
            componentPreviouslySelected = false;

            if (pp.getMouseMode() != MouseModeType.CREATING_TABLE) {
                if ((evt.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) == 0) {
                    if (!isSelected() || pp.getMouseMode() == MouseModeType.IDLE) {
                        pp.setMouseMode(MouseModeType.SELECT_TABLE);
                        pp.selectNone();
                    }
                } else {
                    pp.setMouseMode(MouseModeType.MULTI_SELECT);
                }

                // Alt-click drags table no matter where you clicked
                if ((evt.getModifiersEx() & InputEvent.ALT_DOWN_MASK) != 0) {
                    clickedIndex = PlayPenCoordinate.ITEM_INDEX_TITLE;
                }

                if (clickedIndex == PlayPenCoordinate.ITEM_INDEX_SECTION_TITLE) {
                    if ((evt.getModifiersEx() &
                            (InputEvent.SHIFT_DOWN_MASK |
                                    InputEvent.CTRL_DOWN_MASK)) == 0) {

                        if (!isSectionSelected(clickedCoor.getSection()) ){
                            deSelectEverythingElse(evt);
                            selectNone();
                        }
                        pp.setMouseMode(MouseModeType.SELECT_ITEM);
                    }
                    if (isSectionSelected(clickedCoor.getSection())) {
                        componentPreviouslySelected = true;
                    } else {
                        selectSection(clickedCoor.getSection());
                    }

                    fireSelectionEvent(new SelectionEvent(this, SelectionEvent.SELECTION_EVENT,SelectionEvent.SINGLE_SELECT));
                    repaint();
                } else if (clickedIndex > PlayPenCoordinate.ITEM_INDEX_TITLE) {

                    if ((evt.getModifiersEx() &
                            (InputEvent.SHIFT_DOWN_MASK |
                                    InputEvent.CTRL_DOWN_MASK)) == 0) {

                        if (!isItemSelected(clickedCoor.getItem()) ){
                            deSelectEverythingElse(evt);
                            selectNone();
                        }
                        pp.setMouseMode(MouseModeType.SELECT_ITEM);
                    }
                    if (isItemSelected(clickedCoor.getItem())) {
                        componentPreviouslySelected = true;
                    } else {
                        selectItem(clickedCoor.getItem());
                    }

                    fireSelectionEvent(new SelectionEvent(this, SelectionEvent.SELECTION_EVENT,SelectionEvent.SINGLE_SELECT));
                    repaint();
                }
                
                if (isSelected() && clickedIndex == PlayPenCoordinate.ITEM_INDEX_TITLE){
                    componentPreviouslySelected = true;
                } else {
                    setSelected(true,SelectionEvent.SINGLE_SELECT);
                }
            }

            if (clickedIndex == PlayPenCoordinate.ITEM_INDEX_TITLE && !pp.getSession().getArchitectFrame().createRelationshipIsActive()) {
                Iterator<ContainerPane<?, ?> > it = pp.getSelectedContainers().iterator();
                logger.debug("event point: " + p); //$NON-NLS-1$
                logger.debug("zoomed event point: " + pp.zoomPoint(new Point(p))); //$NON-NLS-1$
                pp.setDraggingTablePanes(true);

                while (it.hasNext()) {
                    // create FloatingContainerPaneListener for each selected item
                    ContainerPane<?, ?> cp = (ContainerPane<?, ?> )it.next();
                    logger.debug("(" + cp.getModel() + ") zoomed selected containerPane's point: " + cp.getLocationOnScreen()); //$NON-NLS-1$ //$NON-NLS-2$
                    logger.debug("(" + cp.getModel() + ") unzoomed selected containerPane's point: " + pp.unzoomPoint(cp.getLocationOnScreen())); //$NON-NLS-1$ //$NON-NLS-2$
                    /* the floating ContainerPane listener expects zoomed handles which are relative to
                               the location of the ContainerPane column which was clicked on.  */
                    Point clickedItem = getLocationOnScreen();
                    Point otherContainer = cp.getLocationOnScreen();
                    logger.debug("(" + cp.getModel() + ") translation x=" //$NON-NLS-1$ //$NON-NLS-2$
                            + (otherContainer.getX() - clickedItem.getX()) + ",y=" //$NON-NLS-1$
                            + (otherContainer.getY() - clickedItem.getY()));
                    Point handle = pp.zoomPoint(new Point(p));
                    handle.translate((int)(clickedItem.getX() - otherContainer.getX()), (int) (clickedItem.getY() - otherContainer.getY()));
                    new FloatingContainerPaneListener(pp, cp, handle);
                }
            }
        } else if (evt.getID() == MouseEvent.MOUSE_MOVED || evt.getID() == MouseEvent.MOUSE_DRAGGED) {
            logger.debug("Mouse moved/dragged to " + evt.getPoint());
//            setSelected(pp.getRubberBand().intersects(getBounds(new Rectangle())),SelectionEvent.SINGLE_SELECT);
        }
    }
    
    // -- Section selection methods -- //

    /**
     * Deselects the given section.
     * 
     * @param sect the section to deselect.
     */
    public void deselectSection(PaneSection<C> sect) {
        selectedSections.remove(sect);
        repaint();
    }

    /**
     * Selects the section.
     * 
     */
    public void selectSection(PaneSection<C> sect) {
        selectedSections.add(sect);
        repaint();
    }

    /**
     * Returns true if the given section was selected in this olap pane.
     * 
     * @param sect
     *            The section to check
     * @return true if section is currently selected.
     */
    public boolean isSectionSelected(PaneSection<C> sect) {
        return selectedSections.contains(sect);
    }
    
    /**
     * Returns a list of the sections that are currently in the selection that
     * also currently exist in the model.
     */
    public List<PaneSection<C>> getSelectedSections() {
        List<PaneSection<C>> selectedSects = new ArrayList<PaneSection<C>>();
        for (PaneSection<C> sect : getSections()) {
            if (isSectionSelected(sect)) {
                selectedSects.add(sect);
            }
        }
        return selectedSects;
    }
    
    
    /**
     * Returns a list of the sections that are currently in the selection that
     * also currently exist in the model. If a section title is selected,
     * the selection state of its items is not considered; the section itself
     * will be in the returned list and none of its items will. This is consistent
     * with other multi-select GUIs we looked at.
     */
    public List<PlayPenCoordinate<T, C>> getSelectedCoordinates() {
        List<PlayPenCoordinate<T, C>> selection = new ArrayList<PlayPenCoordinate<T,C>>();
        for (PaneSection<C> sect : getSections()) {
            if (isSectionSelected(sect)) {
                selection.add(new PlayPenCoordinate<T, C>(
                        this, sect, PlayPenCoordinate.ITEM_INDEX_SECTION_TITLE, null));
            } else {
                int i = 0;
                for (C item : sect.getItems()) {
                    if (isItemSelected(item)) {
                        selection.add(new PlayPenCoordinate<T, C>(
                                this, sect, i, item));
                    }
                    i++;
                }
            }
        }
        return selection;
    }

   
    @Override
    public void selectNone() {
        Set<C> previouslySelectedItems = new HashSet<C>(selectedItems);
        selectedItems.clear();
        fireItemsDeselected(previouslySelectedItems);
        
        selectedSections.clear();
        repaint();
    }
    
    public JPopupMenu getPopup() {
        return getPlayPen().getPopupFactory().createPopupMenu();
    }
    
    @Override
    public Transferable createTransferableForSelection() {
        
        // eclipse wouldn't let us do the obvious thing, so we're actually going to
        // make a copy of the list!
        List<PlayPenCoordinate<?, ?>> annoying = new ArrayList<PlayPenCoordinate<?, ?>>();
        for (PlayPenCoordinate<T, C> ridiculous : getSelectedCoordinates()) {
            annoying.add(ridiculous);
        }
        
        return new DnDOLAPTransferable(getPlayPen(), annoying);
    }
    
    // ------------------------ DROP TARGET LISTENER ------------------------

    /**
     * Called while a drag operation is ongoing, when the mouse
     * pointer enters the operable part of the drop site for the
     * DropTarget registered with this listener.
     *
     * <p>NOTE: This method is expected to be called from the
     * PlayPen's dragOver method (not directly by Swing), and as
     * such the DropTargetContext (and the mouse co-ordinates)
     * will be of the PlayPen.
     */
    public void dragEnter(DropTargetDragEvent dtde) {
        if (logger.isDebugEnabled()) {
            logger.debug("DragEnter event on "+getName()); //$NON-NLS-1$
        }
    }

    /**
     * Called while a drag operation is ongoing, when the mouse
     * pointer has exited the operable part of the drop site for the
     * DropTarget registered with this listener.
     *
     * <p>NOTE: This method is expected to be called from the
     * PlayPen's dragOver method (not directly by Swing), and as
     * such the DropTargetContext (and the mouse co-ordinates)
     * will be of the PlayPen.
     */
    public void dragExit(DropTargetEvent dte) {
        if (logger.isDebugEnabled()) {
            logger.debug("DragExit event on "+getName()); //$NON-NLS-1$
        }
        setInsertionPoint(null);
    }

    /**
     * Called when a drag operation is ongoing, while the mouse
     * pointer is still over the operable part of the drop site for
     * the DropTarget registered with this listener.
     *
     * <p>NOTE: This method is expected to be called from the
     * PlayPen's dragOver method (not directly by Swing), and as
     * such the DropTargetContext (and the mouse co-ordinates)
     * will be of the PlayPen.
     */
    public void dragOver(DropTargetDragEvent dtde) {
        if (logger.isDebugEnabled()) {
            logger.debug("DragOver event on "+getName()+": "+dtde); //$NON-NLS-1$ //$NON-NLS-2$
            logger.debug("Drop Action = "+dtde.getDropAction()); //$NON-NLS-1$
            logger.debug("Source Actions = "+dtde.getSourceActions()); //$NON-NLS-1$
        }
        dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE & dtde.getDropAction());
        Point loc = getPlayPen().unzoomPoint(new Point(dtde.getLocation()));
        loc.x -= getX();
        loc.y -= getY();
        setInsertionPoint(pointToPPCoordinate(loc));
    }

    /**
     * Called when the drag operation has terminated with a drop on
     * the operable part of the drop site for the DropTarget
     * registered with this listener.
     *
     * <p>NOTE: This method is expected to be called from the
     * PlayPen's dragOver method (not directly by Swing), and as
     * such the DropTargetContext (and the mouse co-ordinates)
     * will be of the PlayPen.
     */
    public void drop(DropTargetDropEvent dtde) {
        logger.debug("Drop target drop event on "+getName()+": "+dtde); //$NON-NLS-1$ //$NON-NLS-2$
        try {
            Transferable t = dtde.getTransferable();
            DataFlavor importFlavor = bestImportFlavor(null, t.getTransferDataFlavors());
            logger.debug("Import flavor: " + importFlavor);
            if (importFlavor == null) {
                dtde.rejectDrop();
                return;
            }
            List<List<Integer>> paths = (List<List<Integer>>) t.getTransferData(importFlavor);
            logger.debug("Paths = " + paths);
            List<PlayPenCoordinate<? extends OLAPObject, ? extends OLAPObject>> items =
                DnDOLAPTransferable.resolve(getPlayPen(), paths);
            logger.debug("Resolved Paths = " + items);
            boolean accepted = false;
            for (PlayPenCoordinate<? extends OLAPObject, ? extends OLAPObject> ppco : items) {
                if (ppco.getItem() instanceof Measure) {
                    logger.debug("Trying to add measure " + ppco.getItem());
                    // TODO offer this to the OLAPPane subclass via abstract method
                    getModel().addChild(ppco.getItem());
                    accepted = true;
                }
            }
            dtde.acceptDrop(DnDConstants.ACTION_MOVE);
            dtde.dropComplete(accepted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            setInsertionPoint(null);
        }
    }

    /**
     * Called if the user has modified the current drop gesture.
     */
    public void dropActionChanged(DropTargetDragEvent dtde) {
        // we don't care
    }

    /**
     * Chooses the best import flavour from the flavors array for
     * importing into c.
     *
     * @return The first acceptable DataFlavor in the flavors
     * list, or null if no acceptable flavours are present.
     */
    public DataFlavor bestImportFlavor(JComponent c, DataFlavor[] flavors) {
        for (DataFlavor f : flavors) {
            if (f == DnDOLAPTransferable.PP_COORDINATE_FLAVOR) {
                return f;
            }
        }
        return null;
    }

    /**
     * Changes the insertion point and fires a property change to
     * that effect.
     * 
     * @param newIP The new insertion point. Null is allowed, and means
     * there shouldn't be a visible insertion point.
     */
    public void setInsertionPoint(PlayPenCoordinate<T, C> newIP) {
        PlayPenCoordinate<T, C> oldIP = insertionPoint;
        insertionPoint = newIP;
        firePropertyChange("insertionPoint", oldIP, newIP);
    }
    
    /**
     * Returns the current insertion point. The insertion point is the
     * point directly above the item at the returned coordinate. Therefore,
     * the coordinate might be one item index beyond the last item in
     * the section.
     */
    public PlayPenCoordinate<T, C> getInsertionPoint() {
        return insertionPoint;
    }
}