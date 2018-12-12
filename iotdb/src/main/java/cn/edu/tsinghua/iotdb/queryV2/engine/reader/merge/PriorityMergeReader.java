package cn.edu.tsinghua.iotdb.queryV2.engine.reader.merge;

import cn.edu.tsinghua.iotdb.read.IReader;
import cn.edu.tsinghua.iotdb.utils.TimeValuePair;
import cn.edu.tsinghua.tsfile.read.common.BatchData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;


/**
 * usage:
 *
 * (1) merge multiple chunk group readers in the unsequence file
 * (2）merge sequence reader , unsequence reader and mem reader
 */
public class PriorityMergeReader implements IReader {

    private List<IReader> readerList = new ArrayList<>();
    private List<Integer> priorityList = new ArrayList<>();
    private PriorityQueue<Element> heap = new PriorityQueue<>();

    public void addReaderWithPriority(IReader reader, int priority) throws IOException {
        if (reader.hasNext()) {
            heap.add(new Element(readerList.size(), reader.next(), priority));
        }
        readerList.add(reader);
        priorityList.add(priority);
    }

    @Override
    public boolean hasNext() {
        return heap.size() > 0;
    }

    @Override
    public TimeValuePair next() throws IOException {
        Element top = heap.peek();
        updateHeap(top);
        return top.timeValuePair;
    }

    private void updateHeap(Element top) throws IOException {
        while (heap.size() > 0 && heap.peek().timeValuePair.getTimestamp() == top.timeValuePair.getTimestamp()) {
            Element e = heap.poll();
            IReader prioritySeriesReader = readerList.get(e.index);
            if (prioritySeriesReader.hasNext()) {
                heap.add(new Element(e.index, prioritySeriesReader.next(), priorityList.get(e.index)));
            }
        }
    }

    @Override
    public void skipCurrentTimeValuePair() throws IOException {
        if (hasNext()) {
            next();
        }
    }

    @Override
    public void close() throws IOException {
        for (IReader reader : readerList) {
            reader.close();
        }
    }

    protected class Element implements Comparable<Element> {
        int index;
        TimeValuePair timeValuePair;
        Integer priority;

        public Element(int index, TimeValuePair timeValuePair, int priority) {
            this.index = index;
            this.timeValuePair = timeValuePair;
            this.priority = priority;
        }

        @Override
        public int compareTo(Element o) {
            return this.timeValuePair.getTimestamp() > o.timeValuePair.getTimestamp() ? 1 :
                    this.timeValuePair.getTimestamp() < o.timeValuePair.getTimestamp() ? -1 :
                            o.priority.compareTo(this.priority);
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