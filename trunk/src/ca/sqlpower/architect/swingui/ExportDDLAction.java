package ca.sqlpower.architect.swingui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.*;
import java.io.*;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.List;
import java.util.Iterator;
import javax.swing.*;
import ca.sqlpower.architect.*;
import ca.sqlpower.architect.ddl.*;
import org.apache.log4j.Logger;

public class ExportDDLAction extends AbstractAction {
	private static final Logger logger = Logger.getLogger(ExportDDLAction.class);

	protected ArchitectFrame architectFrame;

	public ExportDDLAction() {
		super("Forward Engineer...",
			  ASUtils.createIcon("ForwardEngineer",
								 "Forward Engineer",
								 ArchitectFrame.getMainInstance().sprefs.getInt(SwingUserSettings.ICON_SIZE, 24)));
		architectFrame = ArchitectFrame.getMainInstance();
		putValue(SHORT_DESCRIPTION, "Forward Engineer SQL Script");
	}

	public void actionPerformed(ActionEvent e) {
		final JDialog d = new JDialog(ArchitectFrame.getMainInstance(),
									  "Forward Engineer SQL Script");
		JPanel cp = new JPanel(new BorderLayout(12,12));
		cp.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
		final DDLExportPanel ddlPanel = new DDLExportPanel(architectFrame.project);
		cp.add(ddlPanel, BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		
		JButton okButton = new JButton("Ok");
		okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					try {
						ddlPanel.applyChanges();
						showPreview(architectFrame.project.getDDLGenerator(), d);
					} catch (Exception ex) {
						JOptionPane.showMessageDialog
							(architectFrame,
							 "Can't export DDL: "+ex.getMessage());
						logger.error("Got exception while exporting DDL", ex);
					}
					//d.setVisible(false);
				}
			});
		buttonPanel.add(okButton);
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					ddlPanel.discardChanges();
					d.setVisible(false);
				}
			});
		buttonPanel.add(cancelButton);
		
		cp.add(buttonPanel, BorderLayout.SOUTH);
		
		d.setContentPane(cp);
		d.pack();
		d.setVisible(true);
	}

	protected void showPreview(GenericDDLGenerator ddlGen, JDialog parentDialog) {
		final GenericDDLGenerator ddlg = ddlGen;
		final JDialog parent = parentDialog;
		final JDialog d = new JDialog(parent, "DDL Preview");
		try {
			JPanel cp = new JPanel(new BorderLayout(12, 12));
			cp.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
			StringBuffer ddl = ddlg.generateDDL(architectFrame.playpen.getDatabase());
			final JTextArea ddlArea = new JTextArea(ddl.toString(), 25, 60);
			ddlArea.setEditable(false); // XXX: will make this editable in the future
			cp.add(new JScrollPane(ddlArea), BorderLayout.CENTER);

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

			final JButton executeButton = new JButton("Execute");
			executeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						SQLDatabase target = architectFrame.playpen.getDatabase();
						Connection con;
						Statement stmt;
						try {
							con = target.getConnection();
						} catch (ArchitectException ex) {
							JOptionPane.showMessageDialog
								(d, "Couldn't connect to target database: "+ex.getMessage()
								 +"\nPlease check the connection settings and try again.");
							return;
						} catch (Exception ex) {
							JOptionPane.showMessageDialog
								(d, "You have to specify a target database connection"
								 +"\nbefore executing this script.");
							logger.error("Unexpected exception in DDL generation", ex);
							return;
						}

						List statements;
						try {
							stmt = con.createStatement();
							statements = ddlg.generateDDLStatements(target);
						} catch (SQLException ex) {
							JOptionPane.showMessageDialog
								(d, "Couldn't generate DDL statements: "+ex.getMessage()
								 +"\nThe problem was reported by the target database.");
							return;
						} catch (ArchitectException ex) {
							JOptionPane.showMessageDialog
								(d, "Couldn't generate DDL statements: "+ex.getMessage()
								 +"\nThe problem was detected internally to the Architect.");
							return;
						}

						int stmtsTried = 0;
						int stmtsCompleted = 0;
						Iterator it = statements.iterator();
						while (it.hasNext()) {
							String sql = (String) it.next();
							try {
								stmtsTried++;
								stmt.executeUpdate(sql);
								stmtsCompleted++;
							} catch (SQLException ex) {
								int decision = JOptionPane.showConfirmDialog
									(d, "SQL statement failed: "+ex.getMessage()
									 +"\nThe statement was:\n"+sql+"\nDo you want to continue?",
									 "SQL Failure", JOptionPane.YES_NO_OPTION);
								if (decision == JOptionPane.NO_OPTION) {
									return;
								}
							}
						}

						try {
							stmt.close();
						} catch (SQLException ex) {
							logger.error("SQLException while closing statement", ex);
						}

						JOptionPane.showMessageDialog(d, "Successfully executed "+stmtsCompleted
													  +" out of "+stmtsTried+" statements.");
					}
				});
			buttonPanel.add(executeButton);

			final JButton saveButton = new JButton("Save");
			saveButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						BufferedWriter out = null;
						try {
							out = new BufferedWriter(new FileWriter(ddlg.getFile()));
							out.write(ddlArea.getText());
						} catch (IOException ex) {
							JOptionPane.showMessageDialog(d, "Couldn't save DDL: "+ex.getMessage());
						} finally {
							try {
								if (out != null) out.close();
							} catch (IOException ioex) {
								logger.error("Couldn't close file in finally clause", ioex);
							}
							d.setVisible(false);
							parent.setVisible(false);
						}
					}
				});
			buttonPanel.add(saveButton);
											
			final JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						d.setVisible(false);
						parent.setVisible(false);
					}
				});
			buttonPanel.add(cancelButton);
			cp.add(buttonPanel, BorderLayout.SOUTH);

			d.setContentPane(cp);
			d.pack();
			d.setVisible(true);
		} catch (Exception e) {
			logger.error("Couldn't Generate DDL", e);
			JOptionPane.showMessageDialog(parent, "Couldn't Generate DDL: "+e.getMessage());
		}
	}
}
