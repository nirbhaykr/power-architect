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

package ca.sqlpower.architect.swingui.olap.action;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFileChooser;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.olap.MondrianXMLReader;
import ca.sqlpower.architect.olap.OLAPObject;
import ca.sqlpower.architect.olap.OLAPSession;
import ca.sqlpower.architect.olap.MondrianModel.Schema;
import ca.sqlpower.architect.swingui.ASUtils;
import ca.sqlpower.architect.swingui.ArchitectSwingSession;
import ca.sqlpower.architect.swingui.action.AbstractArchitectAction;
import ca.sqlpower.architect.swingui.olap.OLAPEditSession;
import ca.sqlpower.swingui.SPSUtils;

public class ImportSchemaAction extends AbstractArchitectAction {

    private static final Logger logger = Logger.getLogger(ImportSchemaAction.class);
    
    public ImportSchemaAction(ArchitectSwingSession session) {
        super(session, "Import Schema...", "Imports an OLAP schema"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser(session.getRecentMenu().getMostRecentFile());
        chooser.addChoosableFileFilter(SPSUtils.XML_FILE_FILTER);
        int returnVal = chooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            Schema loadedSchema = null;
            try {
                OLAPObject olapObj = MondrianXMLReader.importXML(f);
                if (olapObj instanceof Schema) {
                    loadedSchema = (Schema) olapObj;
                } else {
                    throw new IllegalStateException("File parse failed to return a schema object!");
                }
                
                OLAPSession osession = new OLAPSession(loadedSchema);
                osession.setDatabase(session.getTargetDatabase());
                session.getOLAPRootObject().addChild(osession);
                OLAPEditSession editSession = session.getOLAPEditSession(osession);
                
                JDialog d = editSession.getDialog();
                d.setLocationRelativeTo(session.getArchitectFrame());
                d.setVisible(true);
            } catch (Exception ex) {
                logger.error("Failed to parse " + f.getName() + ".");
                ASUtils.showExceptionDialog(session, "Could not read xml schema file.", ex);
            } 
            
        }
    }
    
}
