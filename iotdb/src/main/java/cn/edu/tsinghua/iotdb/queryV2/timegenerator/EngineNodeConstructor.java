package cn.edu.tsinghua.iotdb.queryV2.timegenerator;

import cn.edu.tsinghua.iotdb.engine.querycontext.QueryDataSource;
import cn.edu.tsinghua.iotdb.exception.FileNodeManagerException;
import cn.edu.tsinghua.iotdb.queryV2.factory.SeriesReaderFactory;
import cn.edu.tsinghua.iotdb.queryV2.reader.merge.PriorityMergeReader;
import cn.edu.tsinghua.iotdb.queryV2.reader.sequence.SequenceDataReader;
import cn.edu.tsinghua.iotdb.read.IReader;
import cn.edu.tsinghua.iotdb.read.QueryDataSourceManager;
import cn.edu.tsinghua.tsfile.exception.write.UnSupportedDataTypeException;
import cn.edu.tsinghua.tsfile.read.common.Path;
import cn.edu.tsinghua.tsfile.read.expression.IBinaryExpression;
import cn.edu.tsinghua.tsfile.read.expression.IExpression;
import cn.edu.tsinghua.tsfile.read.expression.impl.SingleSeriesExpression;
import cn.edu.tsinghua.tsfile.read.filter.basic.Filter;
import cn.edu.tsinghua.tsfile.read.query.timegenerator.node.AndNode;
import cn.edu.tsinghua.tsfile.read.query.timegenerator.node.LeafNode;
import cn.edu.tsinghua.tsfile.read.query.timegenerator.node.Node;
import cn.edu.tsinghua.tsfile.read.query.timegenerator.node.OrNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static cn.edu.tsinghua.tsfile.read.expression.ExpressionType.*;

public class EngineNodeConstructor {

    public EngineNodeConstructor() {
    }

    public Node construct(IExpression expression) throws IOException, FileNodeManagerException {
        if (expression.getType() == SERIES) {
            return new EngineLeafNode(generateSeriesReader((SingleSeriesExpression) expression));
        } else {
            Node leftChild;
            Node rightChild;
            if (expression.getType() == OR) {
                leftChild = this.construct(((IBinaryExpression) expression).getLeft());
                rightChild = this.construct(((IBinaryExpression) expression).getRight());
                return new OrNode(leftChild, rightChild);
            } else if (expression.getType() == AND) {
                leftChild = this.construct(((IBinaryExpression) expression).getLeft());
                rightChild = this.construct(((IBinaryExpression) expression).getRight());
                return new AndNode(leftChild, rightChild);
            } else {
                throw new UnSupportedDataTypeException("Unsupported QueryFilterType when construct OperatorNode: " + expression.getType());
            }
        }
    }

    /**
     * data of one path is from multiple source:
     *
     * sequence file
     */
    public IReader generateSeriesReader(SingleSeriesExpression singleSeriesExpression)
            throws IOException, FileNodeManagerException {

        PriorityMergeReader mergeReader = new PriorityMergeReader();

        Filter filter = singleSeriesExpression.getFilter();

        List<IReader> readersOfSelectedSeries = new ArrayList<>();

        Path path = singleSeriesExpression.getSeriesPath();

        QueryDataSource queryDataSource = QueryDataSourceManager.getQueryDataSource(path);

        PriorityMergeReader priorityReader = new PriorityMergeReader();

        // reader for all sequence data
        SequenceDataReader tsFilesReader = new SequenceDataReader(queryDataSource.getSeqDataSource(), filter);
        priorityReader.addReaderWithPriority(tsFilesReader, 1);

        // reader for all unSequence data
        PriorityMergeReader unSeqMergeReader = SeriesReaderFactory.getInstance().
                createUnSeqMergeReader(queryDataSource.getOverflowSeriesDataSource(), filter);
        priorityReader.addReaderWithPriority(unSeqMergeReader, 2);

        readersOfSelectedSeries.add(priorityReader);


        return mergeReader;
    }

}
