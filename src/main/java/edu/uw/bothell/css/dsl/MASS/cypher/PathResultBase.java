package edu.uw.bothell.css.dsl.MASS.cypher;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class PathResultBase {
    private final List<String> elements;

    public PathResultBase(String... elements) {
        this.elements = Lists.newArrayList(elements);
    }

    public PathResultBase(List<String> elements) {
        this.elements = elements;
    }

    public PathResultBase(PathResultBase path, String... additionalElements) {
        this.elements = new ArrayList<>();
        elements.addAll(path.elements);
        Collections.addAll(elements, additionalElements);
    }

    public Stream<String> getElements() {
        return elements.stream();
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements);
    }
}
