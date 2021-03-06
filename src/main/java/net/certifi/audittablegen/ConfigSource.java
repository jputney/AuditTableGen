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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Glenn Sacks
 */
public class ConfigSource {
    private static final Logger logger = LoggerFactory.getLogger(ConfigSource.class);
    
    //Map<String, TableConfig> existingTables;
    //Map<String, TableConfig> existingAuditTables;

    List<TableDef> allTables; //list of all tables in the tartget db/schema
    List<ConfigAttribute> excludes; //only exclude attributes
    List<ConfigAttribute> includes; //only include attributes
    List<ConfigAttribute> dbAttribs; //prefixes & postfixes
    List<ConfigAttribute> triggerAttribs; //triggers
    List<ConfigAttribute> otherAttributes; //these are of unknown type
    //IdentifierMetaData idMetaData;
    int maxUserNameLength;

    
    ConfigSource(){
        //this.idMetaData = idMetaData;
        //existingTables = new CaseInsensitiveMap();
        //existingAuditTables = new CaseInsensitiveMap();
        dbAttribs = new ArrayList<>();
        triggerAttribs = new ArrayList<>();
        excludes = new ArrayList<>();
        includes = new ArrayList<>();
        otherAttributes = new ArrayList<>();
                
        allTables = new ArrayList<>();

    }
    
    void addAttributes(List<ConfigAttribute> attributes){
        
        for ( ConfigAttribute attrib : attributes){
            
            addAttribute(attrib);
            
        }
    }

    void addAttribute(ConfigAttribute attrib) {
        
        switch (attrib.getType()) {
            case exclude:
                excludes.add(attrib);
                break;
            case include:
                includes.add(attrib);
                break;
            case tableprefix: 
            case tablepostfix:
            case columnprefix:
            case columnpostfix:
            case iddatatype:               
            case userdatatype:
            case actiondatatype:
            case timestampdatatype:
            case sessionusersql:
            case sessionuserdatatype:
            case sessionuserdatasize:
                dbAttribs.add(attrib);
                break;
            case auditinsert:
            case auditupdate:
            case auditdelete:
                triggerAttribs.add(attrib);
            case unknown:
            default:
                otherAttributes.add(attrib);
                break;
        }
    }
    
    void addTable(TableDef tableDef){
        
        allTables.add(tableDef);
    }
    
    void addTables(List<TableDef> tablesDefs){
        
        allTables.addAll(tablesDefs);
        
    }

    public int getMaxUserNameLength() {
        return maxUserNameLength;
    }

    public void setMaxUserNameLength(int maxUserNameLength) {
        this.maxUserNameLength = maxUserNameLength;
    }
    
    
}
