package com.wanjun.canalsync.service.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.wanjun.canalsync.model.DatabaseTableModel;
import com.wanjun.canalsync.model.IndexTypeModel;
import com.wanjun.canalsync.service.MappingService;
import com.wanjun.canalsync.util.DateUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author wangchengli
 * @version 1.0
 * @date 2017-12-10
 */
@Service
@PropertySource("classpath:mapping.properties")
@ConfigurationProperties
public class MappingServiceImpl implements MappingService, InitializingBean {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MappingServiceImpl.class);

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    private Map<String, String> dbEsMapping;
    //private Map<String,String> dbAggregationMapping;
    private BiMap<DatabaseTableModel, IndexTypeModel> dbEsBiMapping;
    // private Map<DatabaseTableModel, AggregationModel> dbAggregationModelMapping;
    private Map<String, String> tablePrimaryKeyMap;
    private Map<String, Converter> mysqlTypeElasticsearchTypeMapping;

    @Override
    public Map<String, String> getTablePrimaryKeyMap() {
        return tablePrimaryKeyMap;
    }

    @Override
    public void setTablePrimaryKeyMap(Map<String, String> tablePrimaryKeyMap) {
        this.tablePrimaryKeyMap = tablePrimaryKeyMap;
    }

    @Override
    public IndexTypeModel getIndexType(DatabaseTableModel databaseTableModel) {
        return dbEsBiMapping.get(databaseTableModel);
    }

    /* @Override
     public AggregationModel getAggregationMapping(DatabaseTableModel databaseTableModel) {
         return dbAggregationModelMapping.get(databaseTableModel);
     }
 */
    @Override
    public DatabaseTableModel getDatabaseTableModel(IndexTypeModel indexTypeModel) {
        return dbEsBiMapping.inverse().get(indexTypeModel);
    }

    @Override
    public Object getElasticsearchTypeObject(String mysqlType, String data) {
        Optional<Entry<String, Converter>> result = mysqlTypeElasticsearchTypeMapping.entrySet().parallelStream().filter(entry -> mysqlType.toLowerCase().contains(entry.getKey())).findFirst();
        return (result.isPresent() ? result.get().getValue() : (Converter) data1 -> data1).convert(data);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        dbEsBiMapping = HashBiMap.create();
        dbEsMapping.forEach((key, value) -> {
            String[] keyStrings = StringUtils.split(key, ".");
            String[] valueStrings = StringUtils.split(value, "|");
            if (valueStrings.length == 2) {
                dbEsBiMapping.put(new DatabaseTableModel(keyStrings[0], keyStrings[1]), new IndexTypeModel(valueStrings[0], valueStrings[1]));
            } else if (valueStrings.length == 3) {
                dbEsBiMapping.put(new DatabaseTableModel(keyStrings[0], keyStrings[1]), new IndexTypeModel(valueStrings[0], valueStrings[1], valueStrings[2]));
            } else {
                Map<String, String> map = Maps.newHashMap();
                for (int i = 3; i < valueStrings.length; i++) {
                    String[] splitStrings = StringUtils.split(valueStrings[i], ":");
                    map.put(splitStrings[0], splitStrings[1]);
                    dbEsBiMapping.put(new DatabaseTableModel(keyStrings[0], keyStrings[1]), new IndexTypeModel(valueStrings[0], valueStrings[1], valueStrings[2], map));
                }
            }
        });
/*
        dbAggregationModelMapping = Maps.newHashMap();
        dbEsBiMapping = HashBiMap.create();
        dbAggregationMapping.forEach((key, value) -> {
            String[] keyStrings = StringUtils.split(key, ".");
            String[] valueStrings = StringUtils.split(value, ".");
            String tableType = valueStrings[0];
            if(tableType.equals("1")) {
                String index = valueStrings[1];
                String type = valueStrings[2];
                String mainTableName = valueStrings[3];
                String mainPKColumn = valueStrings[4];
                String fkColumnListStr = valueStrings[5];
                String slaveTableNameListStr = valueStrings[6];
                String slavePKColumnListStr = valueStrings[7];
                String aggregationTableName = valueStrings[8];
                String aggregationPKCoumn = valueStrings[9];
                String aggregationFKColumnListStr = valueStrings[10];
                String databaseName = valueStrings[11];
                List<String> fkColumnList = Arrays.asList(StringUtils.split(fkColumnListStr, ","));
                List<String> slaveTableNameList = Arrays.asList(StringUtils.split(slaveTableNameListStr, ","));
                List<String> slavePKColumnList = Arrays.asList(StringUtils.split(slavePKColumnListStr, ","));
                List<String> aggregationFKColumnList = Arrays.asList(StringUtils.split(aggregationFKColumnListStr, ","));


                dbAggregationModelMapping.put(new DatabaseTableModel(keyStrings[0], keyStrings[1]),
                        new AggregationModel(Integer.parseInt(tableType), index, type, mainTableName, mainPKColumn, fkColumnList, slaveTableNameList, slavePKColumnList, aggregationTableName, aggregationPKCoumn, aggregationFKColumnList, databaseName));
            }else if(tableType.equals("2")){
                dbAggregationModelMapping.put(new DatabaseTableModel(keyStrings[0], keyStrings[1]),new AggregationModel(Integer.parseInt(tableType),valueStrings[1],valueStrings[2],valueStrings[3],valueStrings[4],valueStrings[5],valueStrings[6]));
            }
            dbEsBiMapping.put(new DatabaseTableModel(keyStrings[0], keyStrings[1]), new IndexTypeModel(valueStrings[1], valueStrings[2]));

        });*/

        mysqlTypeElasticsearchTypeMapping = Maps.newHashMap();
        mysqlTypeElasticsearchTypeMapping.put("json", data -> StringUtils.trimToNull(data));
        mysqlTypeElasticsearchTypeMapping.put("char", data -> StringUtils.trimToNull(data));
        mysqlTypeElasticsearchTypeMapping.put("text", data -> StringUtils.trimToNull(data));
        mysqlTypeElasticsearchTypeMapping.put("blob", data -> data);
        mysqlTypeElasticsearchTypeMapping.put("int", Long::valueOf);
        mysqlTypeElasticsearchTypeMapping.put("datetime", data -> DateUtils.parse(data));
        mysqlTypeElasticsearchTypeMapping.put("date", data -> DateUtils.parse(data,DateUtils.DATE_SMALL_STR));
        mysqlTypeElasticsearchTypeMapping.put("float", Double::valueOf);
        mysqlTypeElasticsearchTypeMapping.put("double", Double::valueOf);
        mysqlTypeElasticsearchTypeMapping.put("decimal", Double::valueOf);
    }


    public Map<String, String> getDbEsMapping() {
        return dbEsMapping;
    }

    public void setDbEsMapping(Map<String, String> dbEsMapping) {
        this.dbEsMapping = dbEsMapping;
    }


    /* public Map<String, String> getDbAggregationMapping() {
         return dbAggregationMapping;
     }

     public void setDbAggregationMapping(Map<String, String> dbAggregationMapping) {
         this.dbAggregationMapping = dbAggregationMapping;
     }
 */
    private interface Converter {
        Object convert(String data);
    }
}
