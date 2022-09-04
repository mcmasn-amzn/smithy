/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.loader;

import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.BoxV1Trait;

/**
 * Ensures that the box trait on root level shapes isn't lost when serializing
 * a v1 model as a v2 model.
 *
 * <p>This is useful for tooling that integrates Smithy models with other
 * modeling languages that require knowledge of the box trait on root level
 * shapes.
 */
enum ApplyV1BoxTrait {

    // This is a singleton implemented via an enum.
    INSTANCE;

    void handleTagBoxing(AbstractShapeBuilder<?, ?> builder) {
        if (builder.getAllTraits().containsKey(BoxTrait.ID)) {
            builder.addTrait(new BoxV1Trait());
        } else if (builder.getAllTraits().containsKey(BoxV1Trait.ID)) {
            builder.addTrait(new BoxTrait());
        }
    }
}
