package ca.sqlpower.architect.swingui;

import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLIndex;
import ca.sqlpower.architect.SQLTable;
import ca.sqlpower.architect.SQLIndex.Column;
import ca.sqlpower.architect.SQLIndex.IndexType;
import junit.framework.TestCase;

public class IndexColumnTableModelTest extends TestCase {

    IndexColumnTableModel tm;
    Column indexCol;
    protected void setUp() throws Exception {
        super.setUp();
        SQLTable t = new SQLTable(null,true);
        SQLColumn col = new SQLColumn(t,"col1",1,0,0);
        t.addColumn(col);
        SQLIndex i = new SQLIndex("name",true,"a",IndexType.CLUSTERED,"");
        t.getIndicesFolder().addChild(i);
        i.addIndexColumn(col, true, false);
        i.addChild(i.new Column("expression",true,false));
        indexCol = i.getChild(0);
        tm = new IndexColumnTableModel(i,t);
        
    }

    public void testIsCellEditable() {
        assertTrue(tm.isCellEditable(0, 0));
        assertTrue(tm.isCellEditable(1, 1));
        assertTrue(tm.isCellEditable(0, 2));
    }

    public void testGetColumnClassInt() {
        assertEquals(String.class,tm.getColumnClass(0));
        assertEquals(SQLColumn.class,tm.getColumnClass(1));
        assertEquals(Boolean.class,tm.getColumnClass(2));
        assertEquals(Boolean.class,tm.getColumnClass(3));
    }

    public void testGetColumnCount() {
        assertEquals(4,tm.getColumnCount());
    }

    
    public void testGetRowCount() {
        assertEquals(2,tm.getRowCount());
    }

    public void testGetValueAt() {
        assertEquals("col1", tm.getValueAt(0,0));
        assertEquals(indexCol.getColumn(), tm.getValueAt(0,1));
        assertEquals(true,tm.getValueAt(0, 2));
        assertEquals(false,tm.getValueAt(1, 3));
    }

}