package io.yokota.json.lenses.ops;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AddProperty.class, name = "add"),
    @JsonSubTypes.Type(value = ConvertValue.class, name = "convert"),
    @JsonSubTypes.Type(value = HeadProperty.class, name = "head"),
    @JsonSubTypes.Type(value = HoistProperty.class, name = "hoist"),
    @JsonSubTypes.Type(value = LensIn.class, name = "in"),
    @JsonSubTypes.Type(value = LensMap.class, name = "map"),
    @JsonSubTypes.Type(value = PlungeProperty.class, name = "plunge"),
    @JsonSubTypes.Type(value = RemoveProperty.class, name = "remove"),
    @JsonSubTypes.Type(value = RenameProperty.class, name = "rename"),
    @JsonSubTypes.Type(value = WrapProperty.class, name = "wrap")
})
public abstract class LensOp {
    private String type;

    public LensOp() {
    }

    public String getType() {
        return type;
    }


    public abstract JsonNode apply(JsonNode patchOp);

    public abstract LensOp reverse();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LensOp property = (LensOp) o;
        return Objects.equals(type, property.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}
