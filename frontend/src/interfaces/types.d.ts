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

 declare module 'Models' {
    export type Order = {
      statuses: Array<OrderStatus>,
      userId: String,
      deliveryLat: number,
      deliveryLon: number,
      products: Array<Product>,
      deliveryStatus?: DeliveryStatus
    }

    export type DeliveryStatus = {
      deliveryLat: number,
      deliveryLon: number,
      ts: String
    }

    export type Product = {
      image: string,
      price: number,
      product: string,
      quantity: number
    }

    export type User = {
      userId: String
    }

    export type OrderSummary = {
      id: String,
      price: number,
      createdAt: String
    }

    export type OrderStatus = {
      status: String
      timestamp: String
    }

    export type ResultPane = {
      tableData: TableData, 
      queryInProgress?: boolean
    }

    export type Row = {
      id?: string,
      type: string,
      value: string
    }

    export type TableData = {
      records: Array<Array<string | number | boolean>>;
      columns: Array<string>;
      error?: string;
    };
  
    export type SQLResult = {
      resultTable: {
        dataSchema: {
          columnDataTypes: Array<string>;
          columnNames: Array<string>;
        }
        rows: Array<Array<number | string>>;
      },
      timeUsedMs: number
      numDocsScanned: number
      totalDocs: number
      numServersQueried: number
      numServersResponded: number
      numSegmentsQueried: number
      numSegmentsProcessed: number
      numSegmentsMatched: number
      numConsumingSegmentsQueried: number
      numEntriesScannedInFilter: number
      numEntriesScannedPostFilter: number
      numGroupsLimitReached: boolean
      partialResponse?: number
      minConsumingFreshnessTimeMs: number
      offlineThreadCpuTimeNs: number
      realtimeThreadCpuTimeNs: number
      offlineSystemActivitiesCpuTimeNs: number
      realtimeSystemActivitiesCpuTimeNs: number
      offlineResponseSerializationCpuTimeNs: number
      realtimeResponseSerializationCpuTimeNs: number
      offlineTotalCpuTimeNs: number
      realtimeTotalCpuTimeNs: number
    };
  
    export const enum AuthWorkflow {
      NONE = 'NONE',
      BASIC = 'BASIC',
      OIDC = 'OIDC',
    }
  }