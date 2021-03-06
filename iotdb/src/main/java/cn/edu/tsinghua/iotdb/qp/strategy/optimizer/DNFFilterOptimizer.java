package cn.edu.tsinghua.iotdb.qp.strategy.optimizer;

import static cn.edu.tsinghua.iotdb.qp.constant.SQLConstant.KW_AND;
import static cn.edu.tsinghua.iotdb.qp.constant.SQLConstant.KW_OR;

import java.util.ArrayList;
import java.util.List;

import cn.edu.tsinghua.iotdb.qp.exception.LogicalOptimizeException;
import cn.edu.tsinghua.iotdb.qp.logical.crud.FilterOperator;

public class DNFFilterOptimizer implements IFilterOptimizer {

    /**
     * get DNF(disjunctive normal form) for this filter operator tree. Before getDNF, this op tree
     * must be binary, in another word, each non-leaf node has exactly two children.
     * 
     * @return FilterOperator optimized operator
     * @throws LogicalOptimizeException exception in DNF optimize
     */
    @Override
    public FilterOperator optimize(FilterOperator filter) throws LogicalOptimizeException {
        return getDNF(filter);
    }

    private FilterOperator getDNF(FilterOperator filter) throws LogicalOptimizeException {
        if (filter.isLeaf())
            return filter;
        List<FilterOperator> childOperators = filter.getChildren();
        if (childOperators.size() != 2) {
            throw new LogicalOptimizeException("node :" + filter.getTokenName() + " has "
                    + childOperators.size() + " children");

        }
        FilterOperator left = getDNF(childOperators.get(0));
        FilterOperator right = getDNF(childOperators.get(1));
        List<FilterOperator> newChildrenList = new ArrayList<>();
        switch (filter.getTokenIntType()) {
            case KW_OR:
                addChildOpInOr(left, newChildrenList);
                addChildOpInOr(right, newChildrenList);
                break;
            case KW_AND:
                if (left.getTokenIntType() != KW_OR && right.getTokenIntType() != KW_OR) {
                    addChildOpInAnd(left, newChildrenList);
                    addChildOpInAnd(right, newChildrenList);
                } else {
                    List<FilterOperator> leftAndChildren = getAndChild(left);
                    List<FilterOperator> rightAndChildren = getAndChild(right);
                    for (FilterOperator laChild : leftAndChildren) {
                        for (FilterOperator raChild : rightAndChildren) {
                            FilterOperator r = mergeToConjunction(laChild.clone(), raChild.clone());
                            newChildrenList.add(r);
                        }
                    }
                    filter.setTokenIntType(KW_OR);
                }
                break;
            default:
                throw new LogicalOptimizeException("get DNF failed, this tokenType is:"
                        + filter.getTokenIntType());
        }
        filter.setChildren(newChildrenList);
        return filter;
    }

    /**
     * used by getDNF. merge two conjunction filter operators into a conjunction.<br>
     * conjunction operator consists of {@code FunctionOperator} and inner operator which token is
     * KW_AND.<br>
     * e.g. (a and b) merge (c) is (a and b and c)
     *
     * @param operator1 To be merged operator
     * @param operator2 To be merged operator
     * @return merged operator
     * @throws LogicalOptimizeException exception in DNF optimizing
     */
    private FilterOperator mergeToConjunction(FilterOperator operator1, FilterOperator operator2)
            throws LogicalOptimizeException {
        List<FilterOperator> retChildrenList = new ArrayList<>();
        addChildOpInAnd(operator1, retChildrenList);
        addChildOpInAnd(operator2, retChildrenList);
        FilterOperator ret = new FilterOperator(KW_AND, false);
        ret.setChildren(retChildrenList);
        return ret;
    }

    /**
     * used by getDNF. get conjunction node. <br>
     * If child is basic function or AND node, return a list just containing this. <br>
     * If this child is OR, return children of OR.
     * 
     * @param child operator
     * @return children operator
     */
    private List<FilterOperator> getAndChild(FilterOperator child) {
        switch (child.getTokenIntType()) {
            case KW_OR:
                return child.getChildren();
            default:
                // other token type means leaf node or and
                List<FilterOperator> ret = new ArrayList<>();
                ret.add(child);
                return ret;
        }
    }

    /**
     * If operator is leaf, add it in newChildrenList.
     * If operator is And, add its children to newChildrenList.
     * 
     * @param operator which children should be added in new children list
     * @param newChildrenList new children list
     * @throws LogicalOptimizeException exception in DNF optimizing
     */
    private void addChildOpInAnd(FilterOperator operator, List<FilterOperator> newChildrenList)
            throws LogicalOptimizeException {
        if (operator.isLeaf())
            newChildrenList.add(operator);
        else if (operator.getTokenIntType() == KW_AND)
            newChildrenList.addAll(operator.getChildren());
        else {
            throw new LogicalOptimizeException(
                    "add all children of an OR operator to newChildrenList in AND");
        }
    }

    /**
     * used by getDNF. If operator is leaf or And, add operator to newChildrenList.
     * Else add operator's children to newChildrenList
     * 
     * @param operator to be added in new children list
     * @param newChildrenList new children list
     */
    private void addChildOpInOr(FilterOperator operator, List<FilterOperator> newChildrenList) {
        if (operator.isLeaf() || operator.getTokenIntType() == KW_AND)
            newChildrenList.add(operator);
        else
            newChildrenList.addAll(operator.getChildren());
    }

}
