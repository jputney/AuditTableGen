/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.certifi.audittablegen;

import com.google.common.base.Throwables;
import java.sql.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Glenn Sacks
 */
class GenericDMR implements DataSourceDMR {
    private static final Logger logger = LoggerFactory.getLogger(GenericDMR.class);
    
    DataSource dataSource;
    String databaseProduct;
    String unverifiedSchema;
    String verifiedSchema;
    String unverifiedAuditConfigTable = "auditconfig";
    String verifiedAuditConfigTable;
    ConfigSource configSource;
    IdentifierMetaData idMetaData;
   
    
    /**
     * 
     * @param ds A DataSource. Unless set elsewhere,
     * the default database/schema will be targeted.
     * 
     * @throws SQLException 
     */
    GenericDMR (DataSource ds) throws SQLException{
        
        this (ds, null);
        
    }
    /**
     *
     * @param ds A DataSource
     *
     * @param schema Name of schema to perform operations upon.
     * @throws SQLException
     */
    GenericDMR(DataSource ds, String schema) throws SQLException {

        dataSource = ds;
        Connection conn = ds.getConnection();
        DatabaseMetaData dmd;
        dmd = conn.getMetaData();
        databaseProduct = dmd.getDatabaseProductName();
        idMetaData = new IdentifierMetaData();

        //ToDo: not contemplating quoted identifiers for now
        idMetaData.setStoresLowerCaseIds(dmd.storesLowerCaseIdentifiers());
        idMetaData.setStoresMixedCaseIds(dmd.storesMixedCaseIdentifiers());
        idMetaData.setStoresUpperCaseIds(dmd.storesUpperCaseIdentifiers());

        unverifiedSchema = schema;
        configSource = new ConfigSource(idMetaData);

        conn.close();

    }
    
    /**
     * Generate a DataSource from Properties 
     * @param props
     * @return BasicDataSource as DataSource
     */
    static DataSource getRunTimeDataSource(Properties props){
        
        BasicDataSource dataSource = new BasicDataSource();
        
        dataSource.setDriverClassName(props.getProperty("driver", ""));
        dataSource.setUsername(props.getProperty("username"));
        dataSource.setPassword(props.getProperty("password"));
        dataSource.setUrl(props.getProperty("url"));
        dataSource.setMaxActive(10);
        dataSource.setMaxIdle(5);
        dataSource.setInitialSize(5);
        
        //dataSource.setValidationQuery("SELECT 1");
        
        return dataSource;
    }
    
    /**
     * Return true of the audit configuration source is
     * avaliable.  Only one source is currently supported, and
     * that is a table in the target database/schema named
     * auditconfig.
     * 
     * @return 
     */
    @Override
    public Boolean hasConfigSource (){
        
        return ( (getAuditConfigTable() != null) ? true : false );
        
    }
   
    void loadConfigAttributes(ConfigSource configSource){
        
        StringBuilder builder = new StringBuilder();
        builder.append("select attribute, table, column, value from ").append(verifiedAuditConfigTable);
                
        try {
            
            Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(builder.toString());
            String table;
            String column;
            String value;
            
            while (rs.next()){
                //load attributes into configSource
                
                //initially not worrying about wildcards.
                //TODO handle regexp match in ConfigSource.
                if (rs.getString("attribute").equals("exclude")){
                    //parse exclude attribute
                    table = rs.getString("table").trim();
                    column = rs.getString("column").trim();
                    configSource.addExcludedColumn(table, column);                    
                }
                
                //all columns are included by default, unless specifically
                //excluded.  This is here to handle overriding a
                //regexp match in the exclude list.
                //TODO implement regexp match in ConfigSource.
                else if (rs.getString("attribute").equals("include")){
                    //parse include attribute
                    table = rs.getString("table").trim();
                    column = rs.getString("column").trim();
                    configSource.addIncludedColumn(table, column);         
                }
                else if (rs.getString("attribute").equals("tablepreifx")){
                    //parse table prefix attribute
                    configSource.setTablePrefix(rs.getString("value").trim());
                }
                else if (rs.getString("attribute").equals("tablepostfix")){
                    //parse table postfix attribute
                    configSource.setTablePostfix(rs.getString("value").trim());
                }
                else if (rs.getString("attribute").equals("columnpreifx")){
                    //parse column prefix attribute
                    configSource.setColumnPrefix(rs.getString("value").trim());
                }
                else if (rs.getString("attribute").equals("columnpostfix")){
                    //parse column postfix attribute
                    configSource.setColumnPostfix(rs.getString("value").trim());
                }
                
                //these are unlikely to be used.  Value of 'false' turns the
                //trigger off.  Any other value is true.  If not set here, the
                //default is true for all triggers.
                else if (rs.getString("attribute").equals("auditinsert")){
                    //parse audit insert attribute
                    table = rs.getString("table").trim();
                    configSource.ensureTableConfig(table);
                    value = rs.getString("value").toUpperCase().trim();
                    configSource.getTableConfig(table)
                            .setHasInsertTrigger(value.equals("FALSE") ? false : true);
                }
                 else if (rs.getString("attribute").equals("auditupdate")){
                    //parse audit update attribute
                    table = rs.getString("table").trim();
                    configSource.ensureTableConfig(table);
                    value = rs.getString("value").toUpperCase().trim();
                    configSource.getTableConfig(table)
                            .setHasUpdateTrigger(value.equals("FALSE") ? false : true);
                }
                 else if (rs.getString("attribute").equals("auditdelete")){
                    //parse audit delete attribute
                    table = rs.getString("table").trim();
                    configSource.ensureTableConfig(table);
                    value = rs.getString("value").toUpperCase().trim();
                    configSource.getTableConfig(table)
                            .setHasDeleteTrigger(value.equals("FALSE") ? false : true); 
                }
            }         
            
            rs.close();
            stmt.close();
            conn.close();
            
        } catch (SQLException ex) {
            logger.error("Error retrieving audit configuration" + ex.getMessage());
        }
             
    }
    
    /**
     * Load column metaData for all existing tables and audit tables
     * in the targeted database/schema
     * 
     * @param configSource 
     */
    void loadConfigTables (ConfigSource configSource){
     
        String table;
        
        try (Connection conn = dataSource.getConnection()){
            
            DatabaseMetaData dmd = conn.getMetaData();
            ResultSet rs = dmd.getTables(null, verifiedSchema, null, new String[]{"TABLE"});
            
            while (rs.next()){
                table = rs.getString("TABLE_NAME").trim();
                
                //ToDo: handle case where table full name matches the prefix or postfix
                if ( table.startsWith(configSource.getTablePrefix())
                     && table.endsWith(configSource.getTablePostfix()) ){
                    configSource.addExistingAuditTable(table);
                }
                else {
                    configSource.ensureTableConfig(table);
                }
            }
            
            dmd.getConnection().close();
            
        } catch (SQLException e){
            logger.error("SQL error retrieving table list: ", e);
            return;
        }
        
        //populate table meta data
        for ( Map.Entry <String, TableConfig> entry : configSource.tablesConfig.entrySet()){
            entry.getValue().columns = getColumnMetaDataForTable(entry.getKey());
        }
        
        //populate existing audit table meta data
        for ( Map.Entry <String, TableConfig> entry : configSource.existingAuditTables.entrySet()){
            entry.getValue().columns = getColumnMetaDataForTable(entry.getKey());
        }
        
        return;
 
    }
    
    String getAuditTableSql(ConfigSource configSource, String tableName){
        
        String sql;
        
        String auditTableName =
                configSource.getTablePrefix()
                + tableName
                + configSource.getTablePostfix();
        
        //check if table already exists
        if (configSource.existingAuditTables.containsKey(auditTableName)){
            sql = getAuditTableModifySql(configSource, tableName);
        }
        else {
            sql = getAuditTableCreateSql(configSource, tableName);
        }
        
        return sql;
    }
    
    Map getColumnMetaDataForTable (String tableName){
        
        Map columns = new HashMap<String, HashMap<String, String>>();
        
        try {
            Connection conn = dataSource.getConnection();
            DatabaseMetaData dmd = conn.getMetaData();
            ResultSet rs = dmd.getColumns(null, verifiedSchema, tableName, null);
            
            //load all of the metadata in the result set into a map for each column
            
            ResultSetMetaData rsmd = rs.getMetaData();
            int metaDataColumnCount = rsmd.getColumnCount();
            if (! rs.isBeforeFirst()) {
                throw new RuntimeException("No results for DatabaseMetaData.getColumns(" + verifiedSchema + "." + tableName + ")");
            }
            while (rs.next()){
                Map columnMetaData = new HashMap<String, String>();
                for (int i = 1; i <= metaDataColumnCount; i++){
                    columnMetaData.put(rsmd.getColumnName(i), rs.getString(i));
                }
                columns.put(rs.getString("COLUMN_NAME"), columnMetaData);                        
            }
            
        }
        catch (SQLException e) {
            throw Throwables.propagate(e);
        }
        
        return columns;
        
    }
    
    Map getNewAuditTableColumnMetaData (ConfigSource configSource, String tableToAudit){
        
        TableConfig tc = configSource.getTableConfig(tableToAudit);
        
        Map<String, Map<String, String>> auditTableColumns = new HashMap<String, Map<String, String>>();
        
        for (Map.Entry<String, Map<String, String>> entry : tc.getColumns().entrySet() ){
            
            //ToDo: name this search look for regexp
            if (!tc.excludedColumns.contains(entry.getKey())
                || tc.includedColumns.contains(entry.getKey()) ){
                //include this column in the audit table
                String auditColumnName = configSource.getColumnPrefix()
                        + entry.getKey() + configSource.getColumnPostfix();
                Map auditColumnMetaData = new HashMap<String, String>();
                
                //copy metaData from primary table/column map to audit table/ciolumn map
                for (Map.Entry<String, String> metaDataEntry : entry.getValue().entrySet() ){
                    String metaDataKey = metaDataEntry.getKey();
                    String metaDataValue = metaDataEntry.getValue();
                    if (metaDataKey.equals("IS_AUTOINCREMENT")){
                        metaDataValue="NO";
                    }
                    if (metaDataKey.equals("IS_GENERATEDCOLUMN")){
                        metaDataValue="NO";
                    }
                    auditColumnMetaData.put(metaDataKey, metaDataValue);
                }
                
                auditTableColumns.put(auditColumnName, auditColumnMetaData);
            }
            else {
                logger.info("Table: %s  Column: %s excluded", tableToAudit, entry.getKey());                
            }
        }
        
        return auditTableColumns;
        
    }
        
    String getAuditTableCreateSql(ConfigSource configSource, String tableName){
        
                StringBuilder builder = new StringBuilder();
               //TODO everything.
        
        
        return builder.toString();
    }
    
    String getAuditTableModifySql(ConfigSource configSource, String tableName){
        
        StringBuilder builder = new StringBuilder();
        
        TableConfig tc = configSource.getTableConfig(tableName);
        
        //TODO everything.
        
        
        return builder.toString();
    }

    @Override
    public void setSchema(String unverifiedSchema) {

        this.unverifiedSchema = unverifiedSchema;
        this.verifiedSchema = null;
        
        if(unverifiedSchema == null){
            return;
        }
        
        try (Connection conn = dataSource.getConnection()){

            DatabaseMetaData dmd = conn.getMetaData();
            ResultSet rs = dmd.getSchemas();
            while (rs.next()) {
                if (rs.getString("TABLE_SCHEM").trim().equalsIgnoreCase(unverifiedSchema)) {
                    //store value with whatever case sensitivity it is returned as
                    verifiedSchema = rs.getString("TABLE_SCHEM").trim();
                }
            }
            rs.close();
        } catch (SQLException e) {
            logger.error("error verifying schema", e);
        }
    }
    
    @Override
    public String getSchema() {

        if (verifiedSchema == null
                && unverifiedSchema != null) {
            setSchema(unverifiedSchema);
        }

        return verifiedSchema;
    }
    
    @Override
    public void setAuditConfigTable (String unverifiedTable){
        
        this.unverifiedAuditConfigTable = unverifiedTable;
        this.verifiedAuditConfigTable = null;
        String candidate = null;
        boolean multiMatch = false;
        
        if(unverifiedAuditConfigTable == null){
            return;
        }
        
        if (null == verifiedSchema){
            logger.error("attempting to verify auditConfigTable with unverified schema");
        }
        
        try (Connection conn = dataSource.getConnection()){

            DatabaseMetaData dmd = conn.getMetaData();
            ResultSet rs = dmd.getTables(null, null == verifiedSchema ? null : verifiedSchema, null, null);
            while (rs.next()) {
                if (rs.getString("TABLE_NAME").trim().equalsIgnoreCase(unverifiedTable)) {
                    //store value with whatever case sensitivity it is returned as
                    if (candidate == null){
                    candidate = rs.getString("TABLE_NAME").trim();
                    }
                    else{
                        multiMatch = true;
                    }
                }
            }
            rs.close();
        } catch (SQLException e) {
            logger.error("error verifying auditConfigTable", e);
        }
        
        /** Fails to set verified value if more than one match.
         * This can occur if schema is not set and there are multiple
         * tables in different schemas matching the table name.
         */
        if (!multiMatch){
            this.verifiedAuditConfigTable = candidate;
        }
        
    }
    
    @Override
    public String getAuditConfigTable(){
         if (verifiedAuditConfigTable == null
                && unverifiedAuditConfigTable != null) {
            setAuditConfigTable(unverifiedAuditConfigTable);
        }

        return verifiedAuditConfigTable;
    }
}
