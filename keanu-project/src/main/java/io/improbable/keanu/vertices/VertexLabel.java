package io.improbable.keanu.vertices;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class VertexLabel {
    private static final char NAMESPACE_SEPARATOR = '.';
    private final String name;
    private final List<String> namespace;

    public VertexLabel(String name, String... namespace) {
        this(name, ImmutableList.copyOf(namespace));
    }

    public VertexLabel(String name, List<String> namespace) {
        this.name = name;
        this.namespace = ImmutableList.copyOf(namespace);
    }

    public boolean isInNamespace(String... namespace) {

        if (namespace.length > this.namespace.size()) {
            return false;
        }

        for (int i = 0; i < namespace.length; i++) {
            if (!this.namespace.get(i).equals(namespace[i])) {
                return false;
            }
        }
        return true;
    }

    public VertexLabel withExtraNamespace(String topLevelNamespace) {
        List<String> newNamespace = ImmutableList.<String>builder().addAll(namespace).add(topLevelNamespace).build();
        return new VertexLabel(this.name, newNamespace);
    }

    public VertexLabel withoutOuterNamespace() {
        List<String> reducedNamespace = namespace.subList(0, namespace.size() - 1);
        return new VertexLabel(this.name, reducedNamespace);
    }

    public Optional<String> getOuterNamespace() {
        try {
            return Optional.of(namespace.get(namespace.size() - 1));
        } catch (IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    public String getUnqualifiedName() {
        return name;
    }

    public String getQualifiedName() {
        ImmutableList<String> names = ImmutableList.<String>builder().add(name).addAll(namespace).build();
        return Joiner.on(NAMESPACE_SEPARATOR).join(Lists.reverse(names));
    }

    @Override
    public String toString() {
        return getQualifiedName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VertexLabel that = (VertexLabel) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(namespace, that.namespace);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name, namespace);
    }
}
