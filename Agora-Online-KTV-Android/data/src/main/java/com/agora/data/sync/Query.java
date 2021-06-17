package com.agora.data.sync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/1
 */
public class Query {
    private List<FieldFilter> filters = new ArrayList<>();
    private Long limit;
    private List<OrderBy> orderByList = new ArrayList<>();

    public enum Direction {
        ASCENDING,
        DESCENDING
    }

    public Query limit(long limit) {
        this.limit = limit;
        return this;
    }

    public Query orderBy(OrderBy orderBy) {
        orderByList.add(orderBy);
        return this;
    }

    public Query whereEqualTo(@NonNull String field, @Nullable Object value) {
        return whereHelper(FieldFilter.Operator.EQUAL, field, value);
    }

    private Query whereHelper(FieldFilter.Operator op, String field, Object value) {
        FieldFilter filter = FieldFilter.create(op, field, value);
        filters.add(filter);
        return this;
    }

    public List<FieldFilter> getFilters() {
        return filters;
    }

    public List<OrderBy> getOrderByList() {
        return orderByList;
    }
}
