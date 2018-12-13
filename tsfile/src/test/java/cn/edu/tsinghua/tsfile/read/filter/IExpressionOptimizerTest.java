package cn.edu.tsinghua.tsfile.read.filter;

import cn.edu.tsinghua.tsfile.read.expression.IExpression;
import cn.edu.tsinghua.tsfile.read.expression.impl.GlobalTimeExpression;
import cn.edu.tsinghua.tsfile.read.expression.impl.BinaryExpression;
import cn.edu.tsinghua.tsfile.read.expression.impl.SingleSeriesExpression;
import cn.edu.tsinghua.tsfile.read.filter.basic.Filter;
import cn.edu.tsinghua.tsfile.exception.filter.QueryFilterOptimizationException;
import cn.edu.tsinghua.tsfile.read.expression.util.ExpressionOptimizer;
import cn.edu.tsinghua.tsfile.read.expression.util.ExpressionPrinter;
import cn.edu.tsinghua.tsfile.read.filter.factory.FilterFactory;
import cn.edu.tsinghua.tsfile.read.common.Path;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


public class IExpressionOptimizerTest {

    private ExpressionOptimizer expressionOptimizer = ExpressionOptimizer.getInstance();
    private List<Path> selectedSeries;

    @Before
    public void before() {
        selectedSeries = new ArrayList<>();
        selectedSeries.add(new Path("d1.s1"));
        selectedSeries.add(new Path("d2.s1"));
        selectedSeries.add(new Path("d1.s2"));
        selectedSeries.add(new Path("d2.s2"));
    }

    @After
    public void after() {
        selectedSeries.clear();
    }

    @Test
    public void testTimeOnly() {
        try {
            Filter timeFilter = TimeFilter.lt(100L);
            IExpression expression = new GlobalTimeExpression(timeFilter);
            System.out.println(expressionOptimizer.optimize(expression, selectedSeries));

            IExpression expression2 = BinaryExpression.or(
                    BinaryExpression.and(new GlobalTimeExpression(TimeFilter.lt(50L)), new GlobalTimeExpression(TimeFilter.gt(10L))),
                    new GlobalTimeExpression(TimeFilter.gt(200L)));
            ExpressionPrinter.print(expressionOptimizer.optimize(expression2, selectedSeries));

        } catch (QueryFilterOptimizationException e) {
            e.printStackTrace();
        }


    }

    @Test
    public void testSeriesOnly() {
        try {
            Filter filter1 = FilterFactory.and(FilterFactory.or(
                    ValueFilter.gt(100L), ValueFilter.lt(50L)), TimeFilter.gt(1400L));
            SingleSeriesExpression singleSeriesExp1 = new SingleSeriesExpression(new Path("d2.s1"), filter1);

            Filter filter2 = FilterFactory.and(FilterFactory.or(
                    ValueFilter.gt(100.5f), ValueFilter.lt(50.6f)), TimeFilter.gt(1400L));
            SingleSeriesExpression singleSeriesExp2 = new SingleSeriesExpression(new Path("d1.s2"), filter2);

            Filter filter3 = FilterFactory.or(FilterFactory.or(
                    ValueFilter.gt(100.5), ValueFilter.lt(50.6)), TimeFilter.gt(1400L));
            SingleSeriesExpression singleSeriesExp3 = new SingleSeriesExpression(new Path("d2.s2"), filter3);

            IExpression expression = BinaryExpression.and(BinaryExpression.or(singleSeriesExp1, singleSeriesExp2), singleSeriesExp3);
            Assert.assertEquals(true, expression.toString().equals(
                    expressionOptimizer.optimize(expression, selectedSeries).toString()));

        } catch (QueryFilterOptimizationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testOneTimeAndSeries() {
        Filter filter1 = FilterFactory.or(ValueFilter.gt(100L), ValueFilter.lt(50L));
        SingleSeriesExpression singleSeriesExp1 = new SingleSeriesExpression(new Path("d2.s1"), filter1);

        Filter filter2 = FilterFactory.or(ValueFilter.gt(100.5f), ValueFilter.lt(50.6f));
        SingleSeriesExpression singleSeriesExp2 = new SingleSeriesExpression(new Path("d1.s2"), filter2);

        Filter timeFilter = TimeFilter.lt(14001234L);
        IExpression globalTimeFilter = new GlobalTimeExpression(timeFilter);
        IExpression expression = BinaryExpression.and(BinaryExpression.or(singleSeriesExp1, singleSeriesExp2), globalTimeFilter);
        ExpressionPrinter.print(expression);
        try {
            String rightRet = "[[d2.s1:((value > 100 || value < 50) && time < 14001234)] || [d1.s2:((value > 100.5 || value < 50.6) && time < 14001234)]]";
            IExpression regularFilter = expressionOptimizer.optimize(expression, selectedSeries);
            Assert.assertEquals(true, rightRet.equals(regularFilter.toString()));
            ExpressionPrinter.print(regularFilter);
        } catch (QueryFilterOptimizationException e) {
            Assert.fail();
        }
    }


    @Test
    public void testSeriesAndGlobalOrGlobal() {
        Filter filter1 = FilterFactory.or(ValueFilter.gt(100L), ValueFilter.lt(50L));
        SingleSeriesExpression singleSeriesExp1 = new SingleSeriesExpression(new Path("d2.s1"), filter1);

        Filter timeFilter = TimeFilter.lt(14001234L);
        IExpression globalTimeFilter = new GlobalTimeExpression(timeFilter);

        Filter timeFilter2 = TimeFilter.gt(1L);
        IExpression globalTimeFilter2 = new GlobalTimeExpression(timeFilter2);

        IExpression expression = BinaryExpression.or(BinaryExpression.and(singleSeriesExp1, globalTimeFilter), globalTimeFilter2);
        ExpressionPrinter.print(expression);
        try {
            String rightRet = "[[[[[d1.s1:time > 1] || [d2.s1:time > 1]] || [d1.s2:time > 1]] || [d2.s2:time > 1]] || [d2.s1:((value > 100 || value < 50) && time < 14001234)]]";
            IExpression regularFilter = expressionOptimizer.optimize(expression, selectedSeries);
            Assert.assertEquals(true, rightRet.equals(regularFilter.toString()));
            ExpressionPrinter.print(regularFilter);
        } catch (QueryFilterOptimizationException e) {
            Assert.fail();
        }
    }


    @Test
    public void testSeriesAndGlobal() {
        Filter filter1 = FilterFactory.or(ValueFilter.gt(100L), ValueFilter.lt(50L));
        SingleSeriesExpression singleSeriesExp1 = new SingleSeriesExpression(new Path("d2.s1"), filter1);

        Filter timeFilter = TimeFilter.lt(14001234L);
        IExpression globalTimeFilter = new GlobalTimeExpression(timeFilter);

        IExpression expression = BinaryExpression.and(singleSeriesExp1, globalTimeFilter);

        try {
            String rightRet = "[d2.s1:((value > 100 || value < 50) && time < 14001234)]";
            IExpression regularFilter = expressionOptimizer.optimize(expression, selectedSeries);
            Assert.assertEquals(true, rightRet.equals(regularFilter.toString()));
            ExpressionPrinter.print(regularFilter);
        } catch (QueryFilterOptimizationException e) {
            Assert.fail();
        }
    }


    @Test
    public void testOneTimeOrSeries() {
        Filter filter1 = FilterFactory.or(ValueFilter.gt(100L), ValueFilter.lt(50L));
        SingleSeriesExpression singleSeriesExp1 = new SingleSeriesExpression(
                new Path("d2.s1"), filter1);

        Filter filter2 = FilterFactory.or(ValueFilter.gt(100.5f), ValueFilter.lt(50.6f));
        SingleSeriesExpression singleSeriesExp2 = new SingleSeriesExpression(
                new Path("d1.s2"), filter2);

        Filter timeFilter = TimeFilter.lt(14001234L);
        IExpression globalTimeFilter = new GlobalTimeExpression(timeFilter);
        IExpression expression = BinaryExpression.or(BinaryExpression.or(singleSeriesExp1, singleSeriesExp2), globalTimeFilter);
        ExpressionPrinter.print(expression);

        try {
            String rightRet = "[[[[[d1.s1:time < 14001234] || [d2.s1:time < 14001234]] || [d1.s2:time < 14001234]] " +
                    "|| [d2.s2:time < 14001234]] || [[d2.s1:(value > 100 || value < 50)] || [d1.s2:(value > 100.5 || value < 50.6)]]]";
            IExpression regularFilter = expressionOptimizer.optimize(expression, selectedSeries);
            Assert.assertEquals(true, rightRet.equals(regularFilter.toString()));
            ExpressionPrinter.print(regularFilter);
        } catch (QueryFilterOptimizationException e) {
            Assert.fail();
        }
    }

    @Test
    public void testTwoTimeCombine() {
        Filter filter1 = FilterFactory.or(ValueFilter.gt(100L), ValueFilter.lt(50L));
        SingleSeriesExpression singleSeriesExp1 = new SingleSeriesExpression(new Path("d2.s1"), filter1);

        Filter filter2 = FilterFactory.or(ValueFilter.gt(100.5f), ValueFilter.lt(50.6f));
        SingleSeriesExpression singleSeriesExp2 = new SingleSeriesExpression(new Path("d1.s2"), filter2);

        IExpression globalTimeFilter1 = new GlobalTimeExpression(TimeFilter.lt(14001234L));
        IExpression globalTimeFilter2 = new GlobalTimeExpression(TimeFilter.gt(14001000L));
        IExpression expression = BinaryExpression.or(BinaryExpression.or(singleSeriesExp1, singleSeriesExp2),
                BinaryExpression.and(globalTimeFilter1, globalTimeFilter2));

        try {
            String rightRet = "[[[[[d1.s1:(time < 14001234 && time > 14001000)] || [d2.s1:(time < 14001234 " +
                    "&& time > 14001000)]] || [d1.s2:(time < 14001234 && time > 14001000)]] || [d2.s2:(time < 14001234 " +
                    "&& time > 14001000)]] || [[d2.s1:(value > 100 || value < 50)] || [d1.s2:(value > 100.5 || value < 50.6)]]]";
            IExpression regularFilter = expressionOptimizer.optimize(expression, selectedSeries);
            Assert.assertEquals(true, rightRet.equals(regularFilter.toString()));
        } catch (QueryFilterOptimizationException e) {
            Assert.fail();
        }

        IExpression expression2 = BinaryExpression.and(BinaryExpression.or(singleSeriesExp1, singleSeriesExp2),
                BinaryExpression.and(globalTimeFilter1, globalTimeFilter2));

        try {
            String rightRet2 = "[[d2.s1:((value > 100 || value < 50) && (time < 14001234 && time > 14001000))] || " +
                    "[d1.s2:((value > 100.5 || value < 50.6) && (time < 14001234 && time > 14001000))]]";
            IExpression regularFilter2 = expressionOptimizer.optimize(expression2, selectedSeries);
            Assert.assertEquals(true, rightRet2.equals(regularFilter2.toString()));
        } catch (QueryFilterOptimizationException e) {
            Assert.fail();
        }

        IExpression expression3 = BinaryExpression.or(expression2, expression);
        ExpressionPrinter.print(expression3);
        try {
            IExpression regularFilter3 = expressionOptimizer.optimize(expression3, selectedSeries);
            ExpressionPrinter.print(regularFilter3);
        } catch (QueryFilterOptimizationException e) {
            Assert.fail();
        }
    }
}