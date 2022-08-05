/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.knowledge;

import java.lang.ref.WeakReference;
import java.util.Objects;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.SparseTrait;

/**
 * An index that checks if a member is nullable.
 */
public class NullableIndex implements KnowledgeIndex {

    private final WeakReference<Model> model;

    public NullableIndex(Model model) {
        this.model = new WeakReference<>(model);
    }

    public static NullableIndex of(Model model) {
        return model.getKnowledge(NullableIndex.class, NullableIndex::new);
    }

    /**
     * Defines the type of model consumer to assume when determining if
     * a member should be considered nullable or always present.
     */
    public enum CheckMode {
        /**
         * A client, or any other kind of non-authoritative model consumer
         * that must honor the {@link InputTrait} and {@link ClientOptionalTrait}.
         */
        CLIENT,

        /**
         * A server, or any other kind of authoritative model consumer
         * that does not honor the {@link InputTrait} and {@link ClientOptionalTrait}.
         *
         * <p>This mode should only be used for model consumers that have
         * perfect knowledge of the model because they are built and deployed
         * in lock-step with model updates. A client does not have perfect
         * knowledge of a model because it has to be generated, deployed,
         * and then migrated to when model updates are released. However, a
         * server is required to be updated in order to implement newly added
         * model components.
         */
        SERVER
    }

    /**
     * Checks if a member is nullable using {@link CheckMode#CLIENT}.
     *
     * @param member Member to check.
     * @return Returns true if the member is optional in
     *  non-authoritative consumers of the model like clients.
     * @see #isMemberNullable(MemberShape, CheckMode)
     */
    public boolean isMemberNullable(MemberShape member) {
        return isMemberNullable(member, CheckMode.CLIENT);
    }

    /**
     * Checks if a member is nullable using v2 nullability rules.
     *
     * <p>A {@code checkMode} parameter is required to declare what kind of
     * model consumer is checking if the member is optional. The authoritative
     * consumers like servers do not need to honor the {@link InputTrait} or
     * {@link ClientOptionalTrait}, while non-authoritative consumers like clients
     * must honor these traits.
     *
     * @param member Member to check.
     * @param checkMode The mode used when checking if the member is considered nullable.
     * @return Returns true if the member is optional.
     */
    public boolean isMemberNullable(MemberShape member, CheckMode checkMode) {
        Model m = Objects.requireNonNull(model.get());
        Shape container = m.expectShape(member.getContainer());

        switch (container.getType()) {
            case STRUCTURE:
                // Client mode honors the nullable and input trait.
                if (checkMode == CheckMode.CLIENT
                        && (member.hasTrait(ClientOptionalTrait.class) || container.hasTrait(InputTrait.class))) {
                    return true;
                }

                // Structure members that are @required or @default are not nullable.
                return !member.hasTrait(DefaultTrait.class) && !member.hasTrait(RequiredTrait.class);
            case UNION:
            case SET:
                // Union and set members are never null.
                return false;
            case MAP:
                // Map keys are never null.
                if (member.getMemberName().equals("key")) {
                    return false;
                }
                // fall-through.
            case LIST:
                // Map values and list members are only null if they have the @sparse trait.
                return container.hasTrait(SparseTrait.class);
            default:
                return false;
        }
    }

    /**
     * Checks if the given shape is optional using Smithy IDL 1.0 semantics.
     *
     * <p>This means that the default trait is ignored, the required trait
     * is ignored, and only the box trait and sparse traits are used.
     *
     * <p>Use {@link #isMemberNullable(MemberShape)} to check using Smithy
     * IDL 2.0 semantics that take required, default, and other traits
     * into account.
     *
     * @param shapeId Shape or shape ID to check.
     * @return Returns true if the shape is nullable.
     */
    @Deprecated
    public final boolean isNullable(ToShapeId shapeId) {
        Model m = Objects.requireNonNull(model.get());
        Shape shape = m.getShape(shapeId.toShapeId()).orElse(null);

        if (shape == null) {
            return false;
        }

        switch (shape.getType()) {
            case MEMBER:
                return isMemberNullableInV1(m, shape.asMemberShape().get());
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLOAT:
            case DOUBLE:
                return shape.hasTrait(BoxTrait.class);
            default:
                return true;
        }
    }

    private boolean isMemberNullableInV1(Model model, MemberShape member) {
        Shape container = model.getShape(member.getContainer()).orElse(null);

        // Ignore broken models in this index. Other validators handle these checks.
        if (container == null) {
            return false;
        }

        switch (container.getType()) {
            case STRUCTURE:
                // Only structure shapes look at the box trait.
                if (member.hasTrait(BoxTrait.class)) {
                    return true;
                } else {
                    return isNullable(member.getTarget());
                }
            case MAP:
                // Map keys can never be null.
                if (member.getMemberName().equals("key")) {
                    return false;
                } // fall-through
            case LIST:
                // Sparse lists and maps are considered nullable.
                return container.hasTrait(SparseTrait.class);
            default:
                return false;
        }
    }
}
