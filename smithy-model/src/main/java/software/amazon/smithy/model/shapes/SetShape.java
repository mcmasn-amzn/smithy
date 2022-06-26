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

import java.util.Optional;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code set} shape.
 *
 * <p>Sets are deprecated. Use list shapes with the uniqueItems trait instead.
 */
@Deprecated
public final class SetShape extends CollectionShape implements ToSmithyBuilder<SetShape> {

    private volatile ListShape listCopy;

    private SetShape(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        // Always add a synthetic UniqueItems trait.
        return new Builder().addTrait(new UniqueItemsTrait(true));
    }

    @Override
    public Builder toBuilder() {
        return builder().from(this).member(getMember());
    }

    @Override
    public <R> R accept(ShapeVisitor<R> visitor) {
        return visitor.setShape(this);
    }

    @Override
    public Optional<SetShape> asSetShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.SET;
    }

    @Override
    public Optional<ListShape> asListShape() {
        ListShape list = listCopy;

        if (list == null) {
            listCopy = list = ListShape.builder()
                    .id(getId())
                    .source(getSourceLocation())
                    .traits(getAllTraits().values())
                    .member(getMember())
                    .build();
        }

        return Optional.of(list);
    }

    @Override
    public boolean isListShape() {
        return true;
    }

    /**
     * Builder used to create a {@link SetShape}.
     */
    public static final class Builder extends CollectionShape.Builder<Builder, SetShape> {
        @Override
        public SetShape build() {
            return new SetShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.SET;
        }
    }
}
