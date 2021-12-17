/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;

public class ServiceShapeTest {
    @Test
    public void returnsAppropriateType() {
        ServiceShape shape = ServiceShape.builder().id("ns.foo#Bar").version("2017-01-17").build();

        assertEquals(shape.getType(), ShapeType.SERVICE);
    }

    @Test
    public void mustNotContainMembersInShapeId() {
        Assertions.assertThrows(SourceException.class, () -> {
            ServiceShape.builder().id("ns.foo#Bar$baz").build();
        });
    }

    @Test
    public void convertsToBuilder() {
        ServiceShape service = ServiceShape.builder()
                .id("ns.foo#Bar")
                .version("2017-01-17")
                .build();
        assertEquals(service, service.toBuilder().build());
    }

    @Test
    public void providesContextualShapeName() {
        ShapeId id = ShapeId.from("foo.bar#Name");
        ServiceShape serviceShape = ServiceShape.builder()
                .id("smithy.example#Service")
                .version("1")
                .putRename(id, "FooName")
                .build();

        assertThat(serviceShape.getContextualName(id), equalTo("FooName"));
    }

    @Test
    public void versionDefaultsToEmptyString() {
        ServiceShape shape = ServiceShape.builder()
                .id("com.foo#Example")
                .build();

        assertThat(shape.getVersion(), equalTo(""));
    }

    @Test
    public void hasErrors() {
        ServiceShape shape = ServiceShape.builder()
                .id("com.foo#Example")
                .version("x")
                .addError("com.foo#Common1")
                .addError(ShapeId.from("com.foo#Common2"))
                .build();

        assertThat(shape, equalTo(shape));
        assertThat(shape, equalTo(shape.toBuilder().build()));

        ServiceShape shape2 = shape.toBuilder()
                .errors(Arrays.asList(ShapeId.from("com.foo#Common1"), ShapeId.from("com.foo#Common2")))
                .build();

        assertThat(shape, equalTo(shape2));
    }

    @Test
    public void hasShapes() {
        ServiceShape shape = ServiceShape.builder()
                .id("com.foo#Example")
                .version("x")
                .addShape("com.foo#Common1")
                .addShape(ShapeId.from("com.foo#Common2"))
                .build();

        assertThat(shape, equalTo(shape));
        assertThat(shape, equalTo(shape.toBuilder().build()));

        ServiceShape shape2 = shape.toBuilder()
                .shapes(Arrays.asList(ShapeId.from("com.foo#Common1"), ShapeId.from("com.foo#Common2")))
                .build();

        assertThat(shape, equalTo(shape2));
    }
}
