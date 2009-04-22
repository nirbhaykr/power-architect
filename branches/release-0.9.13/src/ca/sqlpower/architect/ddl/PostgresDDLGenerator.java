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
package ca.sqlpower.architect.ddl;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLIndex;
import ca.sqlpower.architect.SQLSequence;
import ca.sqlpower.architect.SQLTable;
import ca.sqlpower.architect.ddl.DDLStatement.StatementType;
import ca.sqlpower.sql.SQL;

/**
 * DDL Generator for Postgres 8.x (does not support e.g., ALTER COLUMN operations 7.[34]).
 */
public class PostgresDDLGenerator extends GenericDDLGenerator {

	public PostgresDDLGenerator() throws SQLException {
		super();
   	}

	public static final String GENERATOR_VERSION = "$Revision$";
	private static final Logger logger = Logger.getLogger(PostgresDDLGenerator.class);

	private static HashSet reservedWords;

	static {
		reservedWords = new HashSet();
        reservedWords.add("AND");
        reservedWords.add("ANY");
        reservedWords.add("ARRAY");
        reservedWords.add("AS");
        reservedWords.add("ASC");
        reservedWords.add("ASYMMETRIC");
        reservedWords.add("BOTH");
        reservedWords.add("CASE");
        reservedWords.add("CAST");
        reservedWords.add("CHECK");
        reservedWords.add("COLLATE");
        reservedWords.add("COLUMN");
        reservedWords.add("CONSTRAINT");
        reservedWords.add("CREATE");
        reservedWords.add("CURRENT_DATE");
        reservedWords.add("CURRENT_ROLE");
        reservedWords.add("CURRENT_TIME");
        reservedWords.add("CURRENT_TIMESTAMP");
        reservedWords.add("CURRENT_USER");
        reservedWords.add("DEFAULT");
        reservedWords.add("DEFERRABLE");
        reservedWords.add("DESC");
        reservedWords.add("DISTINCT");
        reservedWords.add("DO");
        reservedWords.add("ELSE");
        reservedWords.add("END");
        reservedWords.add("EXCEPT");
        reservedWords.add("FOR");
        reservedWords.add("FOREIGN");
        reservedWords.add("FROM");
        reservedWords.add("GRANT");
        reservedWords.add("GROUP");
        reservedWords.add("HAVING");
        reservedWords.add("IN");
        reservedWords.add("INITIALLY");
        reservedWords.add("INTERSECT");
        reservedWords.add("INTO");
        reservedWords.add("LEADING");
        reservedWords.add("LIMIT");
        reservedWords.add("LOCALTIME");
        reservedWords.add("LOCALTIMESTAMP");
        reservedWords.add("NEW");
        reservedWords.add("NOT");
        reservedWords.add("NULL");
        reservedWords.add("OFF");
        reservedWords.add("OFFSET");
        reservedWords.add("OLD");
        reservedWords.add("ON");
        reservedWords.add("ONLY");
        reservedWords.add("OR");
        reservedWords.add("ORDER");
        reservedWords.add("PLACING");
        reservedWords.add("PRIMARY");
        reservedWords.add("REFERENCES");
        reservedWords.add("RETURNING");
        reservedWords.add("SELECT");
        reservedWords.add("SESSION_USER");
        reservedWords.add("SOME");
        reservedWords.add("SYMMETRIC");
        reservedWords.add("TABLE");
        reservedWords.add("THEN");
        reservedWords.add("TO");
        reservedWords.add("TRAILING");
        reservedWords.add("UNION");
        reservedWords.add("UNIQUE");
        reservedWords.add("USER");
        reservedWords.add("USING");
        reservedWords.add("WHEN");
        reservedWords.add("WHERE");
        reservedWords.add("AUTHORIZATION");
        reservedWords.add("BETWEEN");
        reservedWords.add("BINARY");
        reservedWords.add("CROSS");
        reservedWords.add("FREEZE");
        reservedWords.add("FULL");
        reservedWords.add("ILIKE");
        reservedWords.add("INNER");
        reservedWords.add("IS");
        reservedWords.add("ISNULL");
        reservedWords.add("JOIN");
        reservedWords.add("LEFT");
        reservedWords.add("LIKE");
        reservedWords.add("NATURAL");
        reservedWords.add("NOTNULL");
        reservedWords.add("OUTER");
        reservedWords.add("OVERLAPS");
        reservedWords.add("RIGHT");
        reservedWords.add("SIMILAR");
        reservedWords.add("VERBOSE");

	}

	public String getName() {
	    return "PostgreSQL";
	}

    @Override
	public boolean isReservedWord(String word) {
		return reservedWords.contains(word.toUpperCase());
	}

	@Override
	public void writeHeader() {
		println("-- Created by SQLPower PostgreSQL DDL Generator "+GENERATOR_VERSION+" --");
	}

	/**
	 * Creates and populates <code>typeMap</code> using
	 * DatabaseMetaData, but ignores nullability as reported by the
	 * driver's type map (because all types are reported as
	 * non-nullable).
	 */
	@Override
	protected void createTypeMap() throws SQLException {
		typeMap = new HashMap();

		typeMap.put(Integer.valueOf(Types.BIGINT), new GenericTypeDescriptor("NUMERIC", Types.BIGINT, 1000, null, null, DatabaseMetaData.columnNullable, true, false));
		typeMap.put(Integer.valueOf(Types.BINARY), new GenericTypeDescriptor("BYTEA", Types.BINARY, 4000000000L, null, null, DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.BIT), new GenericTypeDescriptor("BIT", Types.BIT, 1, null, null, DatabaseMetaData.columnNullable, true, false));
		typeMap.put(Integer.valueOf(Types.BLOB), new GenericTypeDescriptor("BYTEA", Types.BLOB, 4000000000L, null, null, DatabaseMetaData.columnNullable, false, false));
        typeMap.put(Integer.valueOf(Types.BOOLEAN), new GenericTypeDescriptor("BOOLEAN", Types.BOOLEAN, 1, null, null, DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.CHAR), new GenericTypeDescriptor("CHAR", Types.CHAR, 4000000000L, "'", "'", DatabaseMetaData.columnNullable, true, false));
		typeMap.put(Integer.valueOf(Types.CLOB), new GenericTypeDescriptor("TEXT", Types.CLOB, 4000000000L, null, null, DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.DATE), new GenericTypeDescriptor("DATE", Types.DATE, 0, "'", "'", DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.DECIMAL), new GenericTypeDescriptor("NUMERIC", Types.DECIMAL, 1000, null, null, DatabaseMetaData.columnNullable, true, true));
		typeMap.put(Integer.valueOf(Types.DOUBLE), new GenericTypeDescriptor("DOUBLE PRECISION", Types.DOUBLE, 38, null, null, DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.FLOAT), new GenericTypeDescriptor("REAL", Types.FLOAT, 38, null, null, DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.INTEGER), new GenericTypeDescriptor("INTEGER", Types.INTEGER, 38, null, null, DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.LONGVARBINARY), new GenericTypeDescriptor("BYTEA", Types.LONGVARBINARY, 4000000000L, null, null, DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.LONGVARCHAR), new GenericTypeDescriptor("TEXT", Types.LONGVARCHAR, 4000000000L, "'", "'", DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.NUMERIC), new GenericTypeDescriptor("NUMERIC", Types.NUMERIC, 1000, null, null, DatabaseMetaData.columnNullable, true, true));
		typeMap.put(Integer.valueOf(Types.REAL), new GenericTypeDescriptor("REAL", Types.REAL, 38, null, null, DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.SMALLINT), new GenericTypeDescriptor("SMALLINT", Types.SMALLINT, 16, null, null, DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.TIME), new GenericTypeDescriptor("TIME", Types.TIME, 0, "'", "'", DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.TIMESTAMP), new GenericTypeDescriptor("TIMESTAMP", Types.TIMESTAMP, 0, "'", "'", DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.TINYINT), new GenericTypeDescriptor("SMALLINT", Types.TINYINT, 16, null, null, DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.VARBINARY), new GenericTypeDescriptor("BYTEA", Types.VARBINARY, 4000000000L, null, null, DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.VARCHAR), new GenericTypeDescriptor("VARCHAR", Types.VARCHAR, 4000000000L, "'", "'", DatabaseMetaData.columnNullable, true, false));
	}

	/**
	 * Turns a logical identifier into a legal identifier (physical name) for PostgreSQL.
     * Also, downcases the identifier for consistency.
     *
     * <p>Uses a deterministic method to generate tie-breaking numbers when there is a namespace
     * conflict.  If you pass null as the physical name, it will use just the logical name when
     * trying to come up with tie-breaking hashes for identifier names.  If the first attempt
     * at generating a unique name fails, subsequent calls should pass each new illegal
     * identifier which will be used with the logical name to generate a another hash.
     *
     * <p>Postgres 8.0 rules:
     * <ul>
     *  <li> no spaces
     *  <li> 63 character limit
     *  <li> identifiers must begin with a letter (one is added if needed)
     *  <li> can't be a postgres reserved word
     *  <li> can only be comprised of letters, numbers, underscores, and $
     * </ul>
	 */
	private String toIdentifier(String logicalName, String physicalName) {
		// replace spaces with underscores
		if (logicalName == null) return null;
		if (logger.isDebugEnabled()) logger.debug("getting physical name for: " + logicalName);
		String ident = logicalName.replace(' ','_').toLowerCase();
		if (logger.isDebugEnabled()) logger.debug("after replace of spaces: " + ident);


		// replace anything that is not a letter, character, or underscore with an underscore...
		ident = ident.replaceAll("[^a-zA-Z0-9_$]", "_");

		// first time through
        // XXX clean this up
		if (physicalName == null) {
			// length is ok
            if (ident.length() <= 63) {
				return ident;
			} else {
				// length is too big
				if (logger.isDebugEnabled()) logger.debug("truncating identifier: " + ident);
				String base = ident.substring(0, 60);
				int tiebreaker = ((ident.hashCode() % 1000) + 1000) % 1000;
				if (logger.isDebugEnabled()) logger.debug("new identifier: " + base + tiebreaker);
				return (base + tiebreaker);
			}
		} else {
			// back for more, which means that we probably
            // had a namespace conflict.  Hack the ident down
            // to size if it's too big, and then generate
            // a hash tiebreaker using the ident and the
            // passed value physicalName
			if (logger.isDebugEnabled()) logger.debug("physical identifier is not unique, regenerating: " + physicalName);
			String base = ident;
			if (ident.length() > 63) {
				base = ident.substring(0, 60);
			}
			int tiebreaker = (((ident + physicalName).hashCode() % 1000) + 1000) % 1000;
			if (logger.isDebugEnabled()) logger.debug("regenerated identifier is: " + (base + tiebreaker));
			return (base + tiebreaker);
		}
	}

    @Override
	public String toIdentifier(String name) {
		return toIdentifier(name,null);
	}

	/**
     * Generates a command for dropping a foreign key.
     * The statement looks like <code>ALTER TABLE ONLY $fktable DROP CONSTRAINT $fkname</code>.
     */
    @Override
    public String makeDropForeignKeySQL(String fkTable, String fkName) {
        return "\nALTER TABLE ONLY "
            + toQualifiedName(fkTable)
            + " DROP CONSTRAINT "
            + fkName;
    }

    @Override
    public void modifyColumn(SQLColumn c) {
        Map colNameMap = new HashMap();
        SQLTable t = c.getParentTable();
        print("\nALTER TABLE ONLY ");
        print( toQualifiedName(t) );
        print(" ALTER COLUMN ");

        // Column name
        String columnPhysName = createPhysicalName(colNameMap,c);
        print(columnPhysName);
        print(" TYPE ");
        print(columnType(c));

        // Column nullability
        print(", ALTER COLUMN ");
        print(columnPhysName);
        print(" ");
        print(c.isDefinitelyNullable() ? "DROP" : "SET");
        print(" NOT NULL");

        endStatement(DDLStatement.StatementType.MODIFY, c);

    }

	/**
	 * Returns null, even though Postgres calls this "Database."  The reason is,
	 * you can't refer to objects in a different database than the default
	 * database for your current connection.  Also, the Postgres DatabaseMetaData
	 * always shows nulls for the catalog/database name of tables.
	 */
    @Override
	public String getCatalogTerm() {
		return null;
	}

	/**
	 * Returns "Schema".
	 */
    @Override
	public String getSchemaTerm() {
		return "Schema";
	}

	/**
	 * Returns the previously-set target schema name, or "public" if there is no
	 * current setting. Public is the Postgres default when no schema is
	 * specified.
	 */
    @Override
	public String getTargetSchema() {
		if (targetSchema != null) return targetSchema;
		else return "public";
	}

    /**
     * create index ddl in postgresql syntax
     */
    @Override
    public void addIndex(SQLIndex index) throws ArchitectException {
        
        createPhysicalName(topLevelNames, index);
        println("");
        print("CREATE ");
        if (index.isUnique()) {
            print("UNIQUE ");
        }
        print("INDEX ");
        print(toIdentifier(index.getName()));
        print("\n ON ");
        print(toQualifiedName(index.getParentTable()));
        if(index.getType() != null) {            
            print(" USING "+ index.getType());
        }
        print("\n ( ");

        boolean first = true;
        for (SQLIndex.Column c : (List<SQLIndex.Column>) index.getChildren()) {
            if (!first) print(", ");
            print(c.getName());
            //TODO: ASC and DESC are not supported in the current version of PostgreSQL (8.2.3)
            //but is expected to be added in later versions (8.3 for example)
            first = false;
        }

        print(" )");
        endStatement(DDLStatement.StatementType.CREATE, index);
        if(index.isClustered()) {
            addCluster(index, toIdentifier(index.getName()), index.getParentTable().getName());
        }
    }
    
    /**
     * This will create a clustered index on a given table.
     */
    private void addCluster(SQLIndex index, String indexName, String table) {
        println("");
        print("CLUSTER " + indexName + " ON " + table);
        endStatement(DDLStatement.StatementType.CREATE, index);
    }
    
    @Override
    public void addTable(SQLTable t) throws SQLException, ArchitectException {
        
        // Create all the sequences that will be needed for auto-increment cols in this table
        for (SQLColumn c : t.getColumns()) {
            if (c.isAutoIncrement()) {
                SQLSequence seq = new SQLSequence(toIdentifier(c.getAutoIncrementSequenceName()));
                print("\nCREATE SEQUENCE ");
                print(toQualifiedName(createSeqPhysicalName(topLevelNames, seq, c)));
                endStatement(StatementType.CREATE, seq);
            }
        }
        
        super.addTable(t);
        
        // attach sequences to columns
        for (SQLColumn c : t.getColumns()) {
            if (c.isAutoIncrement()) {
                SQLSequence seq = new SQLSequence(toIdentifier(c.getAutoIncrementSequenceName()));
                print("\nALTER SEQUENCE " + toQualifiedName(seq.getName()) + " OWNED BY " + toQualifiedName(t) + "." + c.getPhysicalName());
                endStatement(StatementType.CREATE, seq);
            }
        }
    }
    
    /**
     * Augments the default columnDefinition behaviour by adding the correct
     * default value clause for auto-increment columns. For non-autoincrement
     * columns, the behaviour is the same as {@link GenericDDLGenerator#columnDefinition(SQLColumn, Map)}.
     */
    @Override
    protected String columnDefinition(SQLColumn c, Map colNameMap) {
        String nameAndType = super.columnDefinition(c, colNameMap);
        
        if (c.isAutoIncrement()) {
            SQLSequence seq = new SQLSequence(toIdentifier(c.getAutoIncrementSequenceName()));
            return nameAndType + " DEFAULT nextval(" + SQL.quote(toQualifiedName(seq.getName())) + ")";
        } else {
            return nameAndType;
        }
    }

}