/*
 * Created on Jun 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ca.sqlpower.architect.swingui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.SQLCatalog;
import ca.sqlpower.architect.SQLDatabase;
import ca.sqlpower.architect.SQLObject;
import ca.sqlpower.architect.SQLSchema;
import ca.sqlpower.architect.SQLTable;
import ca.sqlpower.architect.SQLDatabase.PopulateProgressMonitor;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class QuickStartPanel1 implements WizardPanel {
	private static final Logger logger = Logger.getLogger(WizardPanel.class);
	
	private QuickStartWizard wizard;
	
	public QuickStartPanel1 (QuickStartWizard wizard) {
		this.wizard = wizard;
	}
	
	private DBTree dbTree;
	private JProgressBar progressBar;
	private JLabel label;
	private JPanel panel;
	
	public JComponent getPanel() {
		
		// static method retrieves database connections
		List databases = QuickStartWizard.getDatabases();
		try {
			dbTree = new DBTree(databases);
		} catch (ArchitectException e) {
			logger.error("problem loading database list",e);
		}
		
		progressBar = ((WizardDialog)wizard.getParentDialog()).getProgressBar();
		label = ((WizardDialog)wizard.getParentDialog()).getProgressLabel();
		label.setText("Loading Database.....");
		
		dbTree.addTreeSelectionListener(new MyTreeSelectionListener(progressBar));
		dbTree.addTreeWillExpandListener(new MyTreeSelectionListener(progressBar));

		// the list of tables for the selected source
		JScrollPane scrollPane = new JScrollPane(dbTree);

		FormLayout layout = new FormLayout (
						"3dlu, 30dlu, 5dlu, fill:200dlu:grow, 3dlu",//columns
						"10dlu, pref, 4dlu, fill:150dlu:grow, 4dlu, 20dlu"); //rows
		
		PanelBuilder pb = new PanelBuilder(layout);
		CellConstraints cc = new CellConstraints();
		pb.add(new JLabel("Choose source database and tables"), cc.xyw(2,2,2, "l,c"));
		pb.add(scrollPane, cc.xyw(2,4,4));
		pb.add(label, cc.xy(2,6));
		pb.add(progressBar, cc.xy(4,6));
		
		panel = new JPanel();
		panel.add(pb.getPanel());
		return panel;		
	}				
			
	/* (non-Javadoc)
	 * @see ca.sqlpower.architect.swingui.ArchitectPanel#applyChanges()
	 */
	public boolean applyChanges() {
		// make a list of SQLTable objects and put them into 
		// the wizard
		TreePath [] paths = dbTree.getSelectionPaths();
		if (paths == null || paths.length == 0) {
			JOptionPane.showMessageDialog(getPanel(),
					"You must select at least one table or view.",
					"Error", JOptionPane.ERROR_MESSAGE);			
			return false;
		}
		List <SQLTable>list = new ArrayList();
		Iterator it = Arrays.asList(paths).iterator();
		while (it.hasNext()) {
			TreePath tp = (TreePath) it.next();
			Object lastObj = tp.getLastPathComponent();
			if (lastObj instanceof SQLTable) {
				logger.debug("adding table: " + ((SQLTable)lastObj).getName());
				list.add((SQLTable)lastObj);
			}
			else {
				if ( getTables((SQLObject)lastObj,list) != true )
					return false;
			}
		}
		wizard.setSourceTables(list);
		return true;
	}
	
	
	/**
	 * When a SQLObject is selected in the tree, this method is used
	 * to retrieve all the SQLTable within that certain SQLObject (or
	 * it could already be the SQLTables being selected) and 
	 * stores it in the list being passed in as parameter
	 * 
	 * @param sqlObj  The SQLObject selected in the tree
	 * @param list    The list in which all the retrieved SQLTable are stored
	 * @return
	 */
	
	private boolean getTables( SQLObject sqlObj, List <SQLTable> list) {
		if ( sqlObj instanceof SQLSchema ) {
			logger.debug("adding schema: " + ((SQLSchema)sqlObj).getName());
			
			try {
				list.addAll( ((SQLSchema)sqlObj).getChildren() );
			} catch (ArchitectException e) {
				JOptionPane.showMessageDialog(getPanel(),
						"Schema "+((SQLSchema)sqlObj).getName()+
						" reading error:"+e.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);			
				return false;
			}
		}
		else if ( sqlObj instanceof SQLCatalog ) {
			
			logger.debug("adding catalog: " + ((SQLCatalog)sqlObj).getName());
			try {
				((SQLCatalog)sqlObj).populate();

				if ( ((SQLCatalog)sqlObj).getChildType() == null ) {}
				else if ( ((SQLCatalog)sqlObj).getChildType() == SQLTable.class )
					list.addAll( ((SQLCatalog)sqlObj).getChildren() );
				else if ( ((SQLCatalog)sqlObj).getChildType() == SQLSchema.class ) {
					for ( SQLSchema schema : (List<SQLSchema>)((SQLCatalog)sqlObj).getChildren() ) {
						if ( getTables(schema,list) != true )
							return false;						
					}
				}
				else {
					JOptionPane.showMessageDialog(getPanel(),
						"Catalog "+((SQLCatalog)sqlObj).getName()+
						" contains unknown child type: "+
						((SQLCatalog)sqlObj).getChildType(),
						"Error", JOptionPane.ERROR_MESSAGE);			
				return false;			}
			} catch (ArchitectException e) {
				JOptionPane.showMessageDialog(getPanel(),
						"Catalog "+((SQLCatalog)sqlObj).getName()+
						" reading error:"+e.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);			
				return false;
			}
		}
		else if ( sqlObj instanceof SQLDatabase ) {
			
			logger.debug("adding database: " + ((SQLDatabase)sqlObj).getName());
			try {
				((SQLDatabase)sqlObj).populate();
				
				if ( ((SQLDatabase)sqlObj).getChildType() == null ) {}
				else if ( ((SQLDatabase)sqlObj).getChildType() == SQLTable.class )
					list.addAll( ((SQLDatabase)sqlObj).getChildren() );
				else if ( ((SQLDatabase)sqlObj).getChildType() == SQLSchema.class ) {
					for ( SQLSchema schema : (List<SQLSchema>)((SQLDatabase)sqlObj).getChildren() ) {
						if ( getTables(schema,list) != true )
							return false;						
					}
				}
				else if ( ((SQLDatabase)sqlObj).getChildType() == SQLCatalog.class ) {
					for ( SQLCatalog cat : (List<SQLCatalog>)((SQLDatabase)sqlObj).getChildren() ) {
						if ( getTables(cat,list) != true )
							return false;						
					}
				}
				else {
					JOptionPane.showMessageDialog(getPanel(),
						"Database "+((SQLDatabase)sqlObj).getName()+
						" contains unknown child type: "+
						((SQLDatabase)sqlObj).getChildType(),
						"Error", JOptionPane.ERROR_MESSAGE);			
				return false;			}
			} catch (ArchitectException e1) {
				JOptionPane.showMessageDialog(getPanel(),
						"Catalog "+((SQLCatalog)sqlObj).getName()+
						" reading error:"+e1.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);			
				return false;
			}	
		}
		else {
			JOptionPane.showMessageDialog(getPanel(),
					"Unknown selected Object type: "+sqlObj.getName()+
					" ("+sqlObj.getClass()+")",
					"Error", JOptionPane.ERROR_MESSAGE);			
			return false;
		}
		return true;
	}
	/* (non-Javadoc)
	 * @see ca.sqlpower.architect.swingui.ArchitectPanel#discardChanges()
	 */
	public void discardChanges() {
	    // nothing to throw away
	}

	public String getTitle() {
		return ("Architect Quick Start - Step 1 of 5 - Select Source Tables");
	}
	
	//***********Progress Monitor Components********
	
	public class MyTreeSelectionListener extends ArchitectSwingWorker implements
			TreeSelectionListener, TreeWillExpandListener, Lister {

		private JProgressBar progressBar;
		protected SQLDatabase.PopulateProgressMonitor progressMonitor;
		private ArrayList <SQLDatabase> databaseList;
		private ArrayList <PopulateProgressMonitor> progressMonitorList;
		
		public MyTreeSelectionListener(JProgressBar progressBar) {
			super();

			this.progressBar = progressBar;
			databaseList = new ArrayList();
			progressMonitorList = new ArrayList();
		}

		@Override
		public void cleanup() throws Exception {
			if ( progressBar != null )
				progressBar.setVisible(false);
		}

		
		@Override
		public void doStuff() throws Exception {

			try {
				ListerProgressBarUpdater progressBarUpdater = 
					new ListerProgressBarUpdater(progressBar, this);
				
				ArrayList l = new ArrayList();
				l.add(((WizardDialog)wizard.getParentDialog()).getNextButton());
				progressBarUpdater.setDisableEnableList(l);

				ArrayList l2 = new ArrayList();
				l2.add(label);
				progressBarUpdater.setVisableInvisableList(l2);
				
				new javax.swing.Timer(100, progressBarUpdater).start();

				for ( SQLDatabase database : databaseList ) {				
					database.populate();
				}

			} catch (ArchitectException e) {
				logger.debug(
						"Unexpected architect exception in ConnectionListener", e);
			}
		}

		public void valueChanged(TreeSelectionEvent e) {
			TreePath [] paths = dbTree.getSelectionPaths();
			if (paths == null || paths.length == 0)
				return;

			Iterator it = Arrays.asList(paths).iterator();
			while (it.hasNext()) {
				TreePath tp = (TreePath) it.next();
				SQLObject lastObj = (SQLObject)tp.getLastPathComponent();
				
				if (lastObj instanceof SQLDatabase) {
					databaseList.add( (SQLDatabase)lastObj );
					try {
						progressMonitorList.add( ((SQLDatabase)lastObj).getProgressMonitor());
					} catch (ArchitectException e1) {
						logger.debug("Error getting progressMonitor", e1);
					}
				}
			}

			new Thread(this).start();
		}

		public Integer getJobSize() throws ArchitectException {
		
			int size = 0;
			for ( PopulateProgressMonitor progress : progressMonitorList ) {
				if (progress != null && progress.getJobSize() != null ) {
					size += progress.getJobSize().intValue();
				}
			}
				
			if ( size > 0 )
				return new Integer(size);
			return null;
		}

		public int getProgress() throws ArchitectException {
			int size = 0;
			for ( PopulateProgressMonitor progress : progressMonitorList ) {
				if (progress != null) {
					size += progress.getProgress();
				}
			}
			return size;
		}

		public boolean isFinished() throws ArchitectException {
			
			for ( PopulateProgressMonitor progress : progressMonitorList ) {
				if (progress != null && !progress.isFinished() )
					return false;
			}
			return true;
		}

		public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
			TreePath path = event.getPath();
			if (path == null )
				return;
		
			SQLObject lastObj = (SQLObject)path.getLastPathComponent();
			if (lastObj instanceof SQLDatabase) {
				databaseList.add( (SQLDatabase)lastObj );
				try {
					progressMonitorList.add( ((SQLDatabase)lastObj).getProgressMonitor());				
				} catch (ArchitectException e1) {
					logger.debug("Error getting progressMonitor", e1);
				}
			}

			new Thread(this).start();
			
		}

		public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
		}

	}
	

}
