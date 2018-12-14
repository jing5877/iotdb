package cn.edu.tsinghua.iotdb.queryV2.job;

import cn.edu.tsinghua.iotdb.queryV2.component.job.QueryJob;
import cn.edu.tsinghua.iotdb.queryV2.component.job.QueryJobFuture;
import cn.edu.tsinghua.tsfile.read.query.dataset.QueryDataSet;


public interface QueryEngine {

    /**
     * Submit a QueryJob to EngineQueryExecutor
     *
     * @param job
     * @return QueryJobFuture for submitted QueryJob
     */
    QueryJobFuture submit(QueryJob job) throws InterruptedException;

    void finishJob(QueryJob queryJob, QueryDataSet queryDataSet);

    void terminateJob(QueryJob queryJob);

    /**
     *
     * @param queryJob
     * @return null if there is NOT corresponding OnePassQueryDataSet for given queryJob
     */
    QueryDataSet retrieveQueryDataSet(QueryJob queryJob);
}