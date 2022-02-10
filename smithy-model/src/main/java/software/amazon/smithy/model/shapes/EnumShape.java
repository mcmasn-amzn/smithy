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

package software.amazon.smithy.model.shapes;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.synthetic.SyntheticEnumTrait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ListUtils;

public final class EnumShape extends StringShape {

    private final Map<String, MemberShape> members;
    private volatile List<String> memberNames;

    private EnumShape(Builder builder) {
        super(builder);
        members = builder.members.get();
    }

    /**
     * Gets the members of the shape, including mixin members.
     *
     * @return Returns the immutable member map.
     */
    public Map<String, MemberShape> getAllMembers() {
        return members;
    }

    /**
     * Returns an ordered list of member names based on the order they are
     * defined in the model, including mixin members.
     *
     * @return Returns an immutable list of member names.
     */
    public List<String> getMemberNames() {
        List<String> names = memberNames;
        if (names == null) {
            names = ListUtils.copyOf(members.keySet());
            memberNames = names;
        }

        return names;
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }

        // Members are ordered, so do a test on the ordering and their values.
        EnumShape b = (EnumShape) other;
        return getMemberNames().equals(b.getMemberNames()) && members.equals(b.members);
    }

    @Override
    public Optional<Trait> findTrait(ShapeId id) {
        if (id.equals(EnumTrait.ID)) {
            return super.findTrait(SyntheticEnumTrait.ID);
        }
        return super.findTrait(id);
    }

    /**
     * Get a specific member by name.
     *
     * @param name Name of the member to retrieve.
     * @return Returns the optional member.
     */
    public Optional<MemberShape> getMember(String name) {
        return Optional.ofNullable(members.get(name));
    }

    @Override
    public Collection<MemberShape> members() {
        return members.values();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return (Builder) updateBuilder(builder());
    }

    @Override
    public <R> R accept(ShapeVisitor<R> cases) {
        return cases.enumShape(this);
    }

    @Override
    public Optional<EnumShape> asEnumShape() {
        return Optional.of(this);
    }

    /**
     * Converts a base {@link StringShape} to an {@link EnumShape} if possible.
     *
     * The result will be empty if the given shape doesn't have the {@link EnumTrait}
     * or if the enum definitions don't have names.
     *
     * @param shape A base {@link StringShape} to convert.
     * @return Optionally returns an {@link EnumShape} equivalent of the given shape.
     */
    public static Optional<EnumShape> fromStringShape(StringShape shape) {
        if (!shape.hasTrait(EnumTrait.ID)) {
            return Optional.empty();
        }
        StringShape stringWithoutEnumTrait = shape.toBuilder().removeTrait(EnumTrait.ID).build();
        Builder enumBuilder = EnumShape.builder();
        stringWithoutEnumTrait.updateBuilder(enumBuilder);
        try {
            return Optional.of(enumBuilder.members(shape.expectTrait(EnumTrait.class)).build());
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }

    @Override
    public ShapeType getType() {
        return ShapeType.ENUM;
    }

    public static final class Builder extends StringShape.Builder {
        private final BuilderRef<Map<String, MemberShape>> members = BuilderRef.forOrderedMap();

        @Override
        public EnumShape build() {
            return new EnumShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.ENUM;
        }

        @Override
        public Builder id(ShapeId shapeId) {
            super.id(shapeId);
            for (MemberShape member : members.peek().values()) {
                addMember(member.toBuilder().id(shapeId.withMember(member.getMemberName())).build());
            }
            return this;
        }

        public Builder members(EnumTrait trait) {
            if (getId() == null) {
                throw new IllegalStateException("An id must be set before adding a named enum trait to a string.");
            }
            clearMembers();
            SyntheticEnumTrait.Builder traitBuilder = SyntheticEnumTrait.builder();

            for (EnumDefinition definition : trait.getValues()) {
                Optional<MemberShape> member = definition.asMember(getId());
                if (member.isPresent()) {
                    addMember(member.get(), false);
                    traitBuilder.addEnum(definition);
                } else {
                    throw new IllegalStateException(String.format(
                            "Unable to convert enum trait entry with name: `%s` and value `%s` to an enum member.",
                            definition.getName().orElse(""), definition.getValue()
                    ));
                }
            }
            super.addTrait(traitBuilder.build());

            return this;
        }

        /**
         * Replaces the members of the builder.
         *
         * @param members Members to add to the builder.
         * @return Returns the builder.
         */
        public Builder members(Collection<MemberShape> members) {
            clearMembers();
            for (MemberShape member : members) {
                addMember(member);
            }
            return this;
        }

        /**
         * Removes all members from the shape.
         *
         * @return Returns the builder.
         */
        public Builder clearMembers() {
            members.clear();
            super.removeTrait(SyntheticEnumTrait.ID);
            return this;
        }

        @Override
        public Builder addMember(MemberShape member) {
            return addMember(member, true);
        }

        private Builder addMember(MemberShape member, boolean updateEnumTrait) {
            if (!member.getTarget().equals(UnitTypeTrait.UNIT)) {
                throw new SourceException(String.format(
                        "Enum members may only target `smithy.api#Unit`, but found `%s`", member.getTarget()
                ), getSourceLocation());
            }
            if (!member.hasTrait(EnumValueTrait.ID)) {
                member = member.toBuilder()
                        .addTrait(EnumValueTrait.builder().stringValue(member.getMemberName()).build())
                        .build();
            } else if (!member.expectTrait(EnumValueTrait.class).getStringValue().isPresent()) {
                throw new SourceException(
                        "Enum members MUST have the enumValue trait with the `string` member set",
                        getSourceLocation());
            }
            members.get().put(member.getMemberName(), member);

            if (updateEnumTrait) {
                SyntheticEnumTrait.Builder builder;
                if (getTraits().containsKey(SyntheticEnumTrait.ID)) {
                    builder = ((SyntheticEnumTrait) getTraits().get(SyntheticEnumTrait.ID)).toBuilder();
                } else {
                    builder = SyntheticEnumTrait.builder();
                }
                builder.addEnum(EnumDefinition.fromMember(member));
                super.addTrait(builder.build());
            }

            return this;
        }

        /**
         * Adds a member to the builder.
         *
         * @param memberName Member name to add.
         * @param enumValue The value of the enum.
         * @return Returns the builder.
         */
        public Builder addMember(String memberName, String enumValue) {
            return addMember(memberName, enumValue, null);
        }

        /**
         * Adds a member to the builder.
         *
         * @param memberName Member name to add.
         * @param enumValue The value of the enum.
         * @param memberUpdater Consumer that can update the created member shape.
         * @return Returns the builder.
         */
        public Builder addMember(String memberName, String enumValue, Consumer<MemberShape.Builder> memberUpdater) {
            if (getId() == null) {
                throw new IllegalStateException("An id must be set before setting a member with a target");
            }

            MemberShape.Builder builder = MemberShape.builder()
                    .target(UnitTypeTrait.UNIT)
                    .id(getId().withMember(memberName))
                    .addTrait(EnumValueTrait.builder().stringValue(enumValue).build());

            if (memberUpdater != null) {
                memberUpdater.accept(builder);
            }

            return addMember(builder.build());
        }

        /**
         * Removes a member by name.
         *
         * <p>Note that removing a member that was added by a mixin results in
         * an inconsistent model. It's best to use ModelTransform to ensure
         * that the model remains consistent when removing members.
         *
         * @param member Member name to remove.
         * @return Returns the builder.
         */
        public Builder removeMember(String member) {
            if (members.hasValue()) {
                members.get().remove(member);
                SyntheticEnumTrait trait = (SyntheticEnumTrait) getTraits().get(SyntheticEnumTrait.ID);
                super.addTrait(trait.toBuilder().removeEnumByName(member).build());
            }
            return this;
        }

        @Override
        public Builder addTrait(Trait trait) {
            if (trait instanceof EnumTrait) {
                throw new SourceException(
                        "The enum trait cannot be added directly to an enum shape.", getSourceLocation());
            }
            return (Builder) super.addTrait(trait);
        }

        @Override
        public Builder removeTrait(ShapeId traitId) {
            if (traitId.equals(SyntheticEnumTrait.ID)) {
                throw new SourceException(
                        "The enum trait cannot be removed directly from an enum shape.", getSourceLocation());
            }
            return (Builder) super.removeTrait(traitId);
        }
    }
}