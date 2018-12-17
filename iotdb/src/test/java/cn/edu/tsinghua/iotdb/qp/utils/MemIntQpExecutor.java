package cn.edu.tsinghua.iotdb.qp.utils;

import cn.edu.tsinghua.iotdb.exception.PathErrorException;
import cn.edu.tsinghua.iotdb.exception.ProcessorException;
import cn.edu.tsinghua.iotdb.qp.constant.SQLConstant;
import cn.edu.tsinghua.iotdb.qp.executor.QueryProcessExecutor;
import cn.edu.tsinghua.iotdb.qp.physical.PhysicalPlan;
import cn.edu.tsinghua.iotdb.qp.physical.crud.DeletePlan;
import cn.edu.tsinghua.iotdb.qp.physical.crud.InsertPlan;
import cn.edu.tsinghua.iotdb.qp.physical.crud.UpdatePlan;
import cn.edu.tsinghua.tsfile.file.metadata.enums.TSDataType;
import cn.edu.tsinghua.tsfile.read.common.Path;
import cn.edu.tsinghua.tsfile.read.expression.IExpression;
import cn.edu.tsinghua.tsfile.read.query.dataset.QueryDataSet;
import cn.edu.tsinghua.tsfile.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Implement a simple executor with a memory demo reading processor for test.
 */
public class MemIntQpExecutor extends QueryProcessExecutor {
    private static Logger LOG = LoggerFactory.getLogger(MemIntQpExecutor.class);

    //pathStr, TreeMap<time, value>
    private Map<String, TestSeries> demoMemDataBase = new HashMap<>();

    private TreeSet<Long> timeStampUnion = new TreeSet<>();

    public void setFakeAllPaths(Map<String, List<String>> fakeAllPaths) {
        this.fakeAllPaths = fakeAllPaths;
    }

    private Map<String, List<String>> fakeAllPaths;

    public MemIntQpExecutor() {
        this.fetchSize.set(5);
    }

    @Override
    public TSDataType getSeriesType(Path fullPath) {
        if (fullPath.equals(SQLConstant.RESERVED_TIME))
            return TSDataType.INT64;
        if (fullPath.equals(SQLConstant.RESERVED_FREQ))
            return TSDataType.FLOAT;
        if (fakeAllPaths != null && fakeAllPaths.containsKey(fullPath.toString()))
            return TSDataType.INT32;
        if (demoMemDataBase.containsKey(fullPath.toString()))
            return TSDataType.INT32;
        return null;
    }

    @Override
    public boolean processNonQuery(PhysicalPlan plan) throws ProcessorException {
        switch (plan.getOperatorType()) {
            case DELETE:
                DeletePlan delete = (DeletePlan) plan;
                return delete(delete.getPaths(), delete.getDeleteTime());
            case UPDATE:
                UpdatePlan update = (UpdatePlan) plan;
                boolean flag = true;
                for (Pair<Long, Long> timePair : update.getIntervals()) {
                    flag &= update(update.getPath(), timePair.left, timePair.right, update.getValue());
                }
                return flag;
            case INSERT:
                InsertPlan insert = (InsertPlan) plan;
                int result = multiInsert(insert.getDeltaObject(), insert.getTime(), insert.getMeasurements(), insert
                        .getValues());
                return result == 0;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public QueryDataSet aggregate(List<Pair<Path, String>> aggres, IExpression expression) {
        return null;
    }

    @Override
    public QueryDataSet groupBy(List<Pair<Path, String>> aggres, IExpression expression, long unit, long origin,
                                List<Pair<Long, Long>> intervals, int fetchSize) {
        return null;
    }

    @Override
    public boolean judgePathExists(Path path) {
        if (SQLConstant.isReservedPath(path))
            return true;
        if (fakeAllPaths != null) {
            return fakeAllPaths.containsKey(path.toString());
        }
        return demoMemDataBase.containsKey(path.toString());
    }

    @Override
    public boolean update(Path path, long startTime, long endTime, String value) {
        if (!demoMemDataBase.containsKey(path.toString())) {
            LOG.warn("no series:{}", path);
            return false;
        }
        TestSeries series = demoMemDataBase.get(path.toString());
        for (Entry<Long, Integer> entry : series.data.entrySet()) {
            long timestamp = entry.getKey();
            if (timestamp >= startTime && timestamp <= endTime)
                entry.setValue(Integer.valueOf(value));
        }
        LOG.info("update, series:{}, time range:<{},{}>, value:{}", path, startTime, endTime, value);
        return true;
    }

    @Override
    protected boolean delete(Path path, long deleteTime) {
        if (!demoMemDataBase.containsKey(path.toString()))
            return true;
        TestSeries series = demoMemDataBase.get(path.toString());
        TreeMap<Long, Integer> delResult = new TreeMap<>();
        for (Entry<Long, Integer> entry : series.data.entrySet()) {
            long timestamp = entry.getKey();
            if (timestamp >= deleteTime) {
                delResult.put(timestamp, entry.getValue());
            }
        }
        series.data = delResult;
        LOG.info("delete series:{}, timestamp:{}", path, deleteTime);
        return true;
    }

    @Override
    public int insert(Path path, long insertTime, String value) {
        String strPath = path.toString();
        if (!demoMemDataBase.containsKey(strPath)) {
            demoMemDataBase.put(strPath, new TestSeries());
        }
        demoMemDataBase.get(strPath).data.put(insertTime, Integer.valueOf(value));
        timeStampUnion.add(insertTime);
        LOG.info("insert into {}:<{},{}>", path, insertTime, value);
        return 0;
    }

    @Override
    public List<String> getAllPaths(String fullPath) {
        return fakeAllPaths != null ? fakeAllPaths.get(fullPath) :
                new ArrayList<String>() {{
                    add(fullPath);
                }};
    }

    @Override
    public int multiInsert(String deltaObject, long insertTime, List<String> measurementList, List<String>
            insertValues) {
        return 0;
    }

    private class TestSeries {
        public TreeMap<Long, Integer> data = new TreeMap<>();
    }
}
