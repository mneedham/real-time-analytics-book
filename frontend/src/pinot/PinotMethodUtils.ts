/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 import _ from 'lodash';
 import { SQLResult } from 'Models';
 import moment from 'moment';
 import { AxiosResponse } from 'axios';

 import { baseApi, transformApi } from './axios-config';

 const JSONbig = require('json-bigint')({'storeAsString': true})
 
 const headers = {
    'Content-Type': 'application/json; charset=UTF-8',
    'Accept': 'text/plain, */*; q=0.01'
  };

 const getQueryResult = (params: Object, url: string): Promise<AxiosResponse<SQLResult>> =>
    transformApi.post(`${url}`, params, {headers});

 
 const getAsObject = (str: SQLResult) => {
   if (typeof str === 'string' || str instanceof String) {
     try {
       return JSONbig.parse(str);
     } catch(e) {
       return JSON.parse(JSON.stringify(str));
     }
   }
   return str;
 };
 
 // This method is used to display query output in tabular format as well as JSON format on query page
 // API: /:urlName (Eg: sql or pql)
 // Expected Output: {columns: [], records: []}
 const getQueryResults = (params, url, checkedOptions) => {
   return getQueryResult(params, url).then(({ data }) => {
     let queryResponse = null;
     queryResponse = getAsObject(data);
 
     let errorStr = '';
     let dataArray = [];
     let columnList = [];
     // if sql api throws error, handle here
     if(typeof queryResponse === 'string'){
       errorStr = queryResponse;
     } else if (queryResponse && queryResponse.exceptions && queryResponse.exceptions.length) {
       errorStr = JSON.stringify(queryResponse.exceptions, null, 2);
     } else
     {
       if (checkedOptions.querySyntaxPQL === true)
       {
         if (queryResponse)
         {
           if (queryResponse.selectionResults)
           {
             // Selection query
             columnList = queryResponse.selectionResults.columns;
             dataArray = queryResponse.selectionResults.results;
           }
           else if (!queryResponse.aggregationResults[0]?.groupByResult)
           {
             // Simple aggregation query
             columnList = _.map(queryResponse.aggregationResults, (aggregationResult) => {
               return {title: aggregationResult.function};
             });
 
             dataArray.push(_.map(queryResponse.aggregationResults, (aggregationResult) => {
               return aggregationResult.value;
             }));
           }
           else if (queryResponse.aggregationResults[0]?.groupByResult)
           {
             // Aggregation group by query
             // TODO - Revisit
             const columns = queryResponse.aggregationResults[0].groupByColumns;
             columns.push(queryResponse.aggregationResults[0].function);
             columnList = _.map(columns, (columnName) => {
               return columnName;
             });
 
             dataArray = _.map(queryResponse.aggregationResults[0].groupByResult, (aggregationGroup) => {
               const row = aggregationGroup.group;
               row.push(aggregationGroup.value);
               return row;
             });
           }
         }
       }
       else if (queryResponse.resultTable?.dataSchema?.columnNames?.length)
       {
         columnList = queryResponse.resultTable.dataSchema.columnNames;
         dataArray = queryResponse.resultTable.rows;
       }
     }
 
     const columnStats = ['timeUsedMs',
       'numDocsScanned',
       'totalDocs',
       'numServersQueried',
       'numServersResponded',
       'numSegmentsQueried',
       'numSegmentsProcessed',
       'numSegmentsMatched',
       'numConsumingSegmentsQueried',
       'numEntriesScannedInFilter',
       'numEntriesScannedPostFilter',
       'numGroupsLimitReached',
       'partialResponse',
       'minConsumingFreshnessTimeMs',
       'offlineThreadCpuTimeNs',
       'realtimeThreadCpuTimeNs',
       'offlineSystemActivitiesCpuTimeNs',
       'realtimeSystemActivitiesCpuTimeNs',
       'offlineResponseSerializationCpuTimeNs',
       'realtimeResponseSerializationCpuTimeNs',
       'offlineTotalCpuTimeNs',
       'realtimeTotalCpuTimeNs'
     ];
 
     return {
       error: errorStr,
       result: {
         columns: columnList,
         records: dataArray,
         error: errorStr
       },
       queryStats: {
         columns: columnStats,
         records: [[queryResponse.timeUsedMs, queryResponse.numDocsScanned, queryResponse.totalDocs, queryResponse.numServersQueried, queryResponse.numServersResponded,
           queryResponse.numSegmentsQueried, queryResponse.numSegmentsProcessed, queryResponse.numSegmentsMatched, queryResponse.numConsumingSegmentsQueried,
           queryResponse.numEntriesScannedInFilter, queryResponse.numEntriesScannedPostFilter, queryResponse.numGroupsLimitReached,
           queryResponse.partialResponse ? queryResponse.partialResponse : '-', queryResponse.minConsumingFreshnessTimeMs,
           queryResponse.offlineThreadCpuTimeNs, queryResponse.realtimeThreadCpuTimeNs,
           queryResponse.offlineSystemActivitiesCpuTimeNs, queryResponse.realtimeSystemActivitiesCpuTimeNs,
           queryResponse.offlineResponseSerializationCpuTimeNs, queryResponse.realtimeResponseSerializationCpuTimeNs,
           queryResponse.offlineTotalCpuTimeNs, queryResponse.realtimeTotalCpuTimeNs]]
       },
       data: queryResponse,
     };
   });
 };
 
 
 export default {
   getQueryResults,
 };