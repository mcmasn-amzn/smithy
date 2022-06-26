package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class SetShapeTest {

    @Test
    public void builderUpdatesMemberId() {
        SetShape shape = SetShape.builder()
                .id("ns.foo#bar")
                .member(ShapeId.from("ns.foo#bam"))
                .id("ns.bar#bar")
                .build();
        assertThat(shape.getMember().getId(), equalTo(ShapeId.from("ns.bar#bar$member")));
        assertThat(shape.getMember().getTarget(), equalTo(ShapeId.from("ns.foo#bam")));
    }

    @Test
    public void worksWithListsAndCollectionsStill() {
        // This test simply ensures we don't mess up existing code that did something
        // similar. If we want to support that existing code, then SetShape can't
        // be a true subtype of ListShape due to it also needing to subclass the
        // ListShape.Builder, which is unfortunately generic over CollectionShape.Builder.
        SetShape s = SetShape.builder()
                .id("smithy.example#Set")
                .member(ShapeId.from("smithy.example#String"))
                .build();
        SetShape s2 = this.<SetShape.Builder, SetShape>renameCollection(s);

        ListShape l = ListShape.builder()
                .id("smithy.example#L")
                .member(ShapeId.from("smithy.example#String"))
                .build();
        ListShape l2 = this.<ListShape.Builder, ListShape>renameCollection(l);
    }

    private <B extends CollectionShape.Builder<B, S>, S extends CollectionShape> S renameCollection(S shape) {
        return this.<B, S>toBuilder(shape).build();
    }

    private <B extends AbstractShapeBuilder<B, S>, S extends Shape> B toBuilder(S shape) {
        return Shape.<B, S>shapeToBuilder(shape).id(shape.getId());
    }
}
