package cn.edu.tsinghua.iotdb.queryV2.engine.reader.sequence;

import cn.edu.tsinghua.iotdb.engine.querycontext.GlobalSortedSeriesDataSource;
import cn.edu.tsinghua.iotdb.queryV2.engine.control.QueryJobManager;
import cn.edu.tsinghua.iotdb.queryV2.engine.reader.mem.MemChunkReaderWithFilter;
import cn.edu.tsinghua.iotdb.queryV2.engine.reader.mem.MemChunkReaderWithoutFilter;
import cn.edu.tsinghua.iotdb.read.IReader;
import cn.edu.tsinghua.iotdb.utils.TimeValuePair;
import cn.edu.tsinghua.tsfile.read.common.BatchData;
import cn.edu.tsinghua.tsfile.read.expression.impl.SingleSeriesExpression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p> A reader for sequentially inserts data，including a list of sealedTsFile, unSealedTsFile
 * and data in MemTable.
 */
public class SequenceDataReader implements IReader {

  protected List<IReader> seriesReaders;
  protected long jobId;

  private boolean hasSeriesReaderInitialized;
  private int nextSeriesReaderIndex;
  private IReader currentSeriesReader;

  public SequenceDataReader(GlobalSortedSeriesDataSource sortedSeriesDataSource, SingleSeriesExpression singleSeriesExpression) throws IOException {
    seriesReaders = new ArrayList<>();
    jobId = QueryJobManager.getInstance().addJobForOneQuery();

    hasSeriesReaderInitialized = false;
    nextSeriesReaderIndex = 0;

    // add data in sealedTsFiles and unSealedTsFile
    if (sortedSeriesDataSource.getSealedTsFiles() != null) {
      seriesReaders.add(new SealedTsFileReader(sortedSeriesDataSource, singleSeriesExpression));
    }
    if (sortedSeriesDataSource.getUnsealedTsFile() != null) {
      seriesReaders.add(new UnSealedTsFileReader(sortedSeriesDataSource.getUnsealedTsFile(), singleSeriesExpression));
    }

    // add data in memTable
    if (sortedSeriesDataSource.hasRawSeriesChunk() && singleSeriesExpression == null) {
      seriesReaders.add(new MemChunkReaderWithoutFilter(sortedSeriesDataSource.getRawSeriesChunk()));
    }
    if (sortedSeriesDataSource.hasRawSeriesChunk() && singleSeriesExpression != null) {
      seriesReaders.add(new MemChunkReaderWithFilter(sortedSeriesDataSource.getRawSeriesChunk(), singleSeriesExpression.getFilter()));
    }
  }

  @Override
  public boolean hasNext() throws IOException {
    if (hasSeriesReaderInitialized && currentSeriesReader.hasNext()) {
      return true;
    } else {
      hasSeriesReaderInitialized = false;
    }

    while (nextSeriesReaderIndex < seriesReaders.size()) {
      if (!hasSeriesReaderInitialized) {
        currentSeriesReader = seriesReaders.get(nextSeriesReaderIndex++);
        hasSeriesReaderInitialized = true;
      }
      if (currentSeriesReader.hasNext()) {
        return true;
      } else {
        hasSeriesReaderInitialized = false;
      }
    }
    return false;
  }

  @Override
  public TimeValuePair next() throws IOException {
    return currentSeriesReader.next();
  }

  @Override
  public void skipCurrentTimeValuePair() throws IOException {
    next();
  }

  @Override
  public void close() throws IOException {
    for (IReader seriesReader : seriesReaders) {
      seriesReader.close();
    }
  }

  @Override
  public boolean hasNextBatch() {
    return false;
  }

  @Override
  public BatchData nextBatch() {
    return null;
  }

  @Override
  public BatchData currentBatch() {
    return null;
  }

}