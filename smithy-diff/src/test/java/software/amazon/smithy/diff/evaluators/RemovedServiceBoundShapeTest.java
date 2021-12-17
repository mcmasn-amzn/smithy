/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.ValidationEvent;

public class RemovedServiceBoundShapeTest {
    @Test
    public void detectsRemovedShapeBindings() {
        Shape s1 = StructureShape.builder()
                .id("foo.baz#S1")
                .build();
        Shape e2 = StructureShape.builder()
                .id("foo.baz#S2")
                .build();
        ServiceShape service1 = ServiceShape.builder()
                .id("foo.baz#Service")
                .version("X")
                .addShape(s1)
                .addShape(e2)
                .build();
        ServiceShape service2 = service1.toBuilder().clearShapes().build();
        Model modelA = Model.assembler().addShapes(service1, s1, e2).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(service2, s1, e2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        // Emits one even for both removals.
        assertThat(TestHelper.findEvents(events, "RemovedServiceBoundShape").size(), equalTo(2));
    }
}
