/*    Copyright 2014 Certifi Inc.
 *
 *    This file is part of AuditTableGen.
 *
 *        AuditTableGen is free software: you can redistribute it and/or modify
 *        it under the terms of the GNU General Public License as published by
 *        the Free Software Foundation, either version 3 of the License, or
 *        (at your option) any later version.
 *
 *        AuditTableGen is distributed in the hope that it will be useful,
 *        but WITHOUT ANY WARRANTY; without even the implied warranty of
 *        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *        GNU General Public License for more details.
 *
 *        You should have received a copy of the GNU General Public License
 *        along with AuditTableGen.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.certifi.audittablegen;

import java.util.HashSet;
import java.util.Map;
import org.apache.commons.collections.map.CaseInsensitiveMap;

/**
 *
 * @author Glenn Sacks
 */
public class TableConfig {
    
    Boolean hasInsertTrigger = true;
    Boolean hasUpdateTrigger = true;
    Boolean hasDeleteTrigger = true;
    Boolean excludeTable = false;
    String tableName;
    Map<String, Map<String,String>> columns; //column meta data
    
    /** excluded columns are represented in the audit table, but changes
     *  to the data in excluded columns to not cause the insert or
     *  update triggers to fire.
     */
    Map<String, String> excludedColumns;
    Map<String, String> includedColumns;

    TableConfig(String tableName){
       excludedColumns = new CaseInsensitiveMap();
       includedColumns = new CaseInsensitiveMap();
       this.tableName = tableName;
       columns = new CaseInsensitiveMap();
       
    }

    public Map<String, Map<String, String>> getColumns() {
        return columns;

    }

    public void setColumns(Map<String, Map<String, String>> columns) {
        this.columns = new CaseInsensitiveMap(columns);
    }
    
    void addExcludedColumn(String columnName){
        excludedColumns.put(columnName, columnName);
    }
    
    public CaseInsensitiveMap getExcludedColumns() {
        return new CaseInsensitiveMap(excludedColumns);
    }
        
    void addIncludedColumn(String columnName){
        includedColumns.put(columnName, columnName);
    }
    
    public CaseInsensitiveMap getIncludedColumns() {
        return new CaseInsensitiveMap(includedColumns);
    }
    
    Boolean getHasDeleteTrigger() {
        return hasDeleteTrigger;
    }

    void setHasDeleteTrigger(Boolean hasDeleteTrigger) {
        this.hasDeleteTrigger = hasDeleteTrigger;
    }

    Boolean getHasInsertTrigger() {
        return hasInsertTrigger;
    }

    void setHasInsertTrigger(Boolean hasInsertTrigger) {
        this.hasInsertTrigger = hasInsertTrigger;
    }

    Boolean getHasUpdateTrigger() {
        return hasUpdateTrigger;
    }

    void setHasUpdateTrigger(Boolean hasUpdateTrigger) {
        this.hasUpdateTrigger = hasUpdateTrigger;
    }

    String getTableName() {
        return tableName;
    }
    
    void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Boolean getExcludeTable() {
        return excludeTable;
    }

    public void setExcludeTable(Boolean excludeTable) {
        this.excludeTable = excludeTable;
    }
    
    
}
