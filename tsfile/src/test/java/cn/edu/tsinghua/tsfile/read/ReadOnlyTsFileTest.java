package cn.edu.tsinghua.tsfile.read;

import cn.edu.tsinghua.tsfile.common.conf.TSFileDescriptor;
import cn.edu.tsinghua.tsfile.utils.TsFileGeneratorForTest;
import cn.edu.tsinghua.tsfile.utils.Binary;
import cn.edu.tsinghua.tsfile.read.filter.TimeFilter;
import cn.edu.tsinghua.tsfile.read.filter.ValueFilter;
import cn.edu.tsinghua.tsfile.read.filter.basic.Filter;
import cn.edu.tsinghua.tsfile.read.expression.QueryFilter;
import cn.edu.tsinghua.tsfile.read.expression.impl.GlobalTimeFilter;
import cn.edu.tsinghua.tsfile.read.expression.impl.QueryFilterFactory;
import cn.edu.tsinghua.tsfile.read.expression.impl.SeriesFilter;
import cn.edu.tsinghua.tsfile.read.filter.factory.FilterFactory;
import cn.edu.tsinghua.tsfile.read.common.Path;
import cn.edu.tsinghua.tsfile.read.common.RowRecord;
import cn.edu.tsinghua.tsfile.read.query.dataset.QueryDataSet;
import cn.edu.tsinghua.tsfile.read.query.QueryExpression;
import cn.edu.tsinghua.tsfile.exception.write.WriteProcessException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class ReadOnlyTsFileTest {

    private static final String FILE_PATH = TsFileGeneratorForTest.outputDataFile;
    private TsFileSequenceReader fileReader;
    private int rowCount = 1000;
    private ReadOnlyTsFile tsFile;

    @Before
    public void before() throws InterruptedException, WriteProcessException, IOException {
        TSFileDescriptor.getInstance().getConfig().timeSeriesEncoder = "TS_2DIFF";
        TsFileGeneratorForTest.generateFile(rowCount, 16 * 1024 * 1024, 10000);
        fileReader = new TsFileSequenceReader(FILE_PATH);
        tsFile = new ReadOnlyTsFile(fileReader);
    }

    @After
    public void after() throws IOException {
        tsFile.close();
        TsFileGeneratorForTest.after();
    }

    @Test
    public void queryTest() throws IOException {
        Filter filter = TimeFilter.lt(1480562618100L);
        Filter filter2 = ValueFilter.gt(new Binary("dog"));
        Filter filter3 = FilterFactory.and(TimeFilter.gtEq(1480562618000L), TimeFilter.ltEq(1480562618100L));

        QueryFilter queryFilter = QueryFilterFactory.or(
                QueryFilterFactory.and(
                        new SeriesFilter(new Path("d1.s1"), filter),
                        new SeriesFilter(new Path("d1.s4"), filter2)),
                new GlobalTimeFilter(filter3)
        );

        QueryExpression queryExpression = QueryExpression.create()
                .addSelectedPath(new Path("d1.s1"))
                .addSelectedPath(new Path("d1.s4"))
                .setQueryFilter(queryFilter);
        QueryDataSet queryDataSet = tsFile.query(queryExpression);
        long aimedTimestamp = 1480562618000L;
        while (queryDataSet.hasNext()) {
            //System.out.println("find next!");
            RowRecord rowRecord = queryDataSet.next();
            //System.out.println("result datum: "+rowRecord.getTimestamp()+"," +rowRecord.getFields());
            Assert.assertEquals(aimedTimestamp, rowRecord.getTimestamp());
            aimedTimestamp++;
        }

        queryExpression = QueryExpression.create()
                .addSelectedPath(new Path("d1.s1"))
                .addSelectedPath(new Path("d1.s4"));
        queryDataSet = tsFile.query(queryExpression);
        aimedTimestamp = 1480562618000L;
        int count = 0;
        while (queryDataSet.hasNext()) {
            RowRecord rowRecord = queryDataSet.next();
            Assert.assertEquals(aimedTimestamp, rowRecord.getTimestamp());
            aimedTimestamp++;
            count++;
        }
        Assert.assertEquals(rowCount, count);

        queryExpression = QueryExpression.create()
                .addSelectedPath(new Path("d1.s1"))
                .addSelectedPath(new Path("d1.s4"))
                .setQueryFilter(new GlobalTimeFilter(filter3));
        queryDataSet = tsFile.query(queryExpression);
        aimedTimestamp = 1480562618000L;
        count = 0;
        while (queryDataSet.hasNext()) {
            RowRecord rowRecord = queryDataSet.next();
            Assert.assertEquals(aimedTimestamp, rowRecord.getTimestamp());
            aimedTimestamp++;
            count++;
        }
        Assert.assertEquals(101, count);

    }
}