// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.agora.data.sync;

/**
 * Represents a sort order for a Firestore Query
 */
public class OrderBy {
    /**
     * The direction of the ordering
     */
    public enum Direction {
        ASCENDING,
        DESCENDING;
    }

    public static OrderBy getInstance(Direction direction, String field) {
        return new OrderBy(direction, field);
    }

    public Direction getDirection() {
        return direction;
    }

    public String getField() {
        return field;
    }

    private final Direction direction;
    final String field;

    private OrderBy(Direction direction, String field) {
        this.direction = direction;
        this.field = field;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof OrderBy)) {
            return false;
        }

        OrderBy other = (OrderBy) o;
        return direction == other.direction && field.equals(other.field);
    }

    @Override
    public int hashCode() {
        int result = 29;
        result = 31 * result + direction.hashCode();
        result = 31 * result + field.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return (direction == Direction.ASCENDING ? "" : "-") + field;
    }
}
