package com.tinkerpop.gremlin.structure;

import com.tinkerpop.gremlin.AbstractGremlinTest;
import com.tinkerpop.gremlin.ExceptionCoverage;
import com.tinkerpop.gremlin.FeatureRequirementSet;
import org.javatuples.Pair;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
@ExceptionCoverage(exceptionClass = Vertex.Exceptions.class, methods = {
        "multiplePropertiesExistForProvidedKey",
})
@RunWith(Enclosed.class)
public class MetaPropertyTest extends AbstractGremlinTest {

    public static class MetaPropertyAddition extends AbstractGremlinTest {

        @Test
        public void shouldAddMultiProperties() {
            final Vertex v = g.addVertex("name", "marko", "age", 34);
            tryCommit(g, g -> {
                assertEquals("marko", v.property("name").value());
                assertEquals("marko", v.value("name"));
                assertEquals(34, v.property("age").value());
                assertEquals(34, v.<Integer>value("age").intValue());
                assertEquals(1, v.properties("name").count().next().intValue());
                assertEquals(2, v.properties().count().next().intValue());
                assertEquals(1, g.V().count().next().intValue());
                assertEquals(0, g.E().count().next().intValue());
            });

            final MetaProperty<String> property = v.property("name", "marko a. rodriguez");
            tryCommit(g, g -> assertEquals(v, property.getElement()));

            try {
                v.property("name");
                fail("This should throw a: " + Vertex.Exceptions.multiplePropertiesExistForProvidedKey("name"));
            } catch (final IllegalStateException e) {
                assertEquals(Vertex.Exceptions.multiplePropertiesExistForProvidedKey("name").getMessage(), e.getMessage());
            } catch (final Exception e) {
                fail("This should throw a: " + Vertex.Exceptions.multiplePropertiesExistForProvidedKey("name"));
            }
            assertTrue(v.valueMap().next().get("name").contains("marko"));
            assertTrue(v.valueMap().next().get("name").contains("marko a. rodriguez"));
            assertEquals(3, v.properties().count().next().intValue());
            assertEquals(2, v.properties("name").count().next().intValue());
            assertEquals(1, g.V().count().next().intValue());
            assertEquals(0, g.E().count().next().intValue());

            assertEquals(v, v.property("name", "mrodriguez").getElement());
            tryCommit(g, g -> {
                assertEquals(3, v.properties("name").count().next().intValue());
                assertEquals(4, v.properties().count().next().intValue());
                assertEquals(1, g.V().count().next().intValue());
                assertEquals(0, g.E().count().next().intValue());
            });

            v.<String>properties("name").sideEffect(meta -> {
                meta.get().property("counter", meta.get().value().length());
                meta.get().property(Graph.Key.hide("counter"), meta.get().value().length());
            }).iterate();
            tryCommit(g, g -> {
                v.properties().forEach(meta -> {
                    assertEquals(MetaProperty.DEFAULT_LABEL, meta.label());
                    assertTrue(meta.isPresent());
                    assertFalse(meta.isHidden());
                    assertEquals(v, meta.getElement());
                    if (meta.key().equals("age")) {
                        assertEquals(meta.value(), 34);
                        assertEquals(0, meta.properties().count().next().intValue());
                    }
                    if (meta.key().equals("name")) {
                        assertEquals(((String) meta.value()).length(), meta.<Integer>value("counter").intValue());
                        assertEquals(((String) meta.value()).length(), meta.<Integer>value(Graph.Key.hide("counter")).intValue());
                        assertEquals(1, meta.properties().count().next().intValue());
                        assertEquals(1, meta.keys().size());
                        assertTrue(meta.keys().contains("counter"));
                        assertEquals(1, meta.hiddens().count().next().intValue());
                        assertEquals(1, meta.hiddenKeys().size());
                        assertTrue(meta.hiddenKeys().contains("counter"));
                    }
                });
                assertEquals(1, g.V().count().next().intValue());
                assertEquals(0, g.E().count().next().intValue());
            });
        }

        @Test
        public void shouldHandleSingleMetaProperties() {
            final Vertex v = g.addVertex("name", "marko", "name", "marko a. rodriguez", "name", "marko rodriguez");
            tryCommit(g, g -> {
                assertEquals(3, v.properties().count().next().intValue());
                assertEquals(3, v.properties("name").count().next().intValue());
                assertTrue(v.properties("name").value().toList().contains("marko"));
                assertTrue(v.properties("name").value().toList().contains("marko a. rodriguez"));
                assertTrue(v.properties("name").value().toList().contains("marko rodriguez"));
            });
            v.properties("name").remove();
            tryCommit(g, g -> {
                assertEquals(0, v.properties().count().next().intValue());
                assertEquals(0, v.properties("name").count().next().intValue());
            });
            v.property("name", "marko");
            v.property("name", "marko a. rodriguez");
            v.property("name", "marko rodriguez");
            tryCommit(g, g -> {
                assertEquals(3, v.properties().count().next().intValue());
                assertEquals(3, v.properties("name").count().next().intValue());
                assertTrue(v.properties("name").value().toList().contains("marko"));
                assertTrue(v.properties("name").value().toList().contains("marko a. rodriguez"));
                assertTrue(v.properties("name").value().toList().contains("marko rodriguez"));
            });
            v.singleProperty("name", "okram", "acl", "private", "date", 2014);
            tryCommit(g, g -> {
                assertEquals(1, v.properties("name").count().next().intValue());
                assertEquals(1, v.properties().count().next().intValue());
                assertEquals(2, v.property("name").valueMap().next().size());
                assertEquals("private", v.property("name").valueMap().next().get("acl"));
                assertEquals(2014, v.property("name").valueMap().next().get("date"));
            });

            v.remove();
            tryCommit(g, g -> {
                assertEquals(0, g.V().count().next().intValue());
                assertEquals(0, g.E().count().next().intValue());
            });

            final Vertex u = g.addVertex("name", "marko", "name", "marko a. rodriguez", "name", "marko rodriguez");
            tryCommit(g);
            // TODO: Neo4j no happy long time ---- u.properties().remove();
            u.singleProperty("name", "okram", "acl", "private", "date", 2014);
            tryCommit(g, g -> {
                // u.properties().forEach(p -> System.out.println(p + "::" + p.properties().toList()));
                assertEquals(1, u.properties("name").count().next().intValue());
                assertEquals(1, u.properties().count().next().intValue());
                assertEquals(2, u.property("name").valueMap().next().size());
                assertEquals("private", u.property("name").valueMap().next().get("acl"));
                assertEquals(2014, u.property("name").valueMap().next().get("date"));
            });

        }
    }

    public static class MetaPropertyRemoval extends AbstractGremlinTest {

        @Test
        public void shouldRemoveMultiProperties() {
            Vertex v = g.addVertex("name", "marko", "age", 34);
            v.property("name", "marko a. rodriguez");
            tryCommit(g);
            v.property("name", "marko rodriguez");
            v.property("name", "marko");
            tryCommit(g, g -> {
                assertEquals(5, v.properties().count().next().intValue());
                assertEquals(4, v.properties().has(MetaProperty.KEY, "name").count().next().intValue());
                assertEquals(4, v.properties("name").count().next().intValue());
                assertEquals(1, v.properties("name").has(MetaProperty.VALUE, "marko a. rodriguez").count().next().intValue());
                assertEquals(1, v.properties("name").has(MetaProperty.VALUE, "marko rodriguez").count().next().intValue());
                assertEquals(2, v.properties("name").has(MetaProperty.VALUE, "marko").count().next().intValue());
                assertEquals(1, g.V().count().next().intValue());
                assertEquals(0, g.E().count().next().intValue());
            });

            v.properties().has(MetaProperty.VALUE, "marko").remove();
            tryCommit(g, g -> {
                assertEquals(3, v.properties().count().next().intValue());
                assertEquals(2, v.properties().has(MetaProperty.KEY, "name").count().next().intValue());
                assertEquals(1, g.V().count().next().intValue());
                assertEquals(0, g.E().count().next().intValue());
            });

            v.property("age").remove();
            tryCommit(g, g -> {
                assertEquals(2, v.properties().count().next().intValue());
                assertEquals(2, v.properties().has(MetaProperty.KEY, "name").count().next().intValue());
                assertEquals(1, g.V().count().next().intValue());
                assertEquals(0, g.E().count().next().intValue());
            });

            v.properties("name").has(MetaProperty.KEY, "name").remove();
            tryCommit(g, g -> {
                assertEquals(0, v.properties().count().next().intValue());
                assertEquals(0, v.properties().has(MetaProperty.KEY, "name").count().next().intValue());
                assertEquals(1, g.V().count().next().intValue());
                assertEquals(0, g.E().count().next().intValue());
            });
        }

        @Test
        public void shouldRemoveMultiPropertiesWhenVerticesAreRemoved() {
            Vertex marko = g.addVertex("name", "marko", "name", "okram");
            Vertex stephen = g.addVertex("name", "stephen", "name", "spmallette");
            tryCommit(g, g -> {
                assertEquals(2, g.V().count().next().intValue());
                assertEquals(0, g.E().count().next().intValue());
                assertEquals(2, marko.properties("name").count().next().intValue());
                assertEquals(2, stephen.properties("name").count().next().intValue());
                assertEquals(2, marko.properties().count().next().intValue());
                assertEquals(2, stephen.properties().count().next().intValue());
                assertEquals(0, marko.properties("blah").count().next().intValue());
                assertEquals(0, stephen.properties("blah").count().next().intValue());
                assertEquals(2, g.V().count().next().intValue());
                assertEquals(0, g.E().count().next().intValue());
            });

            stephen.remove();
            tryCommit(g, g -> {
                assertEquals(1, g.V().count().next().intValue());
                assertEquals(0, g.E().count().next().intValue());
                assertEquals(2, marko.properties("name").count().next().intValue());
                assertEquals(2, marko.properties().count().next().intValue());
                assertEquals(0, marko.properties("blah").count().next().intValue());
            });

            for (int i = 0; i < 100; i++) {
                marko.property("name", i);
            }
            tryCommit(g, g -> {
                assertEquals(1, g.V().count().next().intValue());
                assertEquals(0, g.E().count().next().intValue());
                assertEquals(102, marko.properties("name").count().next().intValue());
                assertEquals(102, marko.properties().count().next().intValue());
                assertEquals(0, marko.properties("blah").count().next().intValue());
            });
            g.V().properties("name").has(MetaProperty.VALUE, (a, b) -> ((Class) b).isAssignableFrom(a.getClass()), Integer.class).remove();
            tryCommit(g, g -> {
                assertEquals(1, g.V().count().next().intValue());
                assertEquals(0, g.E().count().next().intValue());
                assertEquals(2, marko.properties("name").count().next().intValue());
                assertEquals(2, marko.properties().count().next().intValue());
                assertEquals(0, marko.properties("blah").count().next().intValue());
            });

            marko.remove();
            tryCommit(g, g -> {
                assertEquals(0, g.V().count().next().intValue());
                assertEquals(0, g.E().count().next().intValue());
            });
        /*
        TODO: Stephen, Neo4j and TinkerGraph have different (though valid) behaviors here. Thoughts?
        assertEquals(0, marko.properties("name").count().next().intValue());
        assertEquals(0, marko.properties().count().next().intValue());
        assertEquals(0, marko.properties("blah").count().next().intValue());*/


        }
    }

    public static class MetaPropertyProperties extends AbstractGremlinTest {

        @Test
        public void shouldSupportPropertiesOnMultiProperties() {
            Vertex v = g.addVertex("name", "marko", "age", 34);
            tryCommit(g, g -> {
                assertEquals(2, g.V().properties().count().next().intValue());
                assertEquals(1, g.V().count().next().intValue());
                assertEquals(0, g.E().count().next().intValue());
                // TODO: Neo4j needs a better ID system for MetaProperties
                assertEquals(v.property("name"), v.property("name").property("acl", "public").getElement());
                assertEquals(v.property("age"), v.property("age").property("acl", "private").getElement());
            });

            v.property("name").property("acl", "public");
            v.property("age").property("acl", "private");
            tryCommit(g, g -> {

                assertEquals(2, g.V().properties().count().next().intValue());
                assertEquals(1, g.V().properties("age").count().next().intValue());
                assertEquals(1, g.V().properties("name").count().next().intValue());
                assertEquals(1, g.V().properties("age").properties().count().next().intValue());
                assertEquals(1, g.V().properties("name").properties().count().next().intValue());
                assertEquals(1, g.V().properties("age").properties("acl").count().next().intValue());
                assertEquals(1, g.V().properties("name").properties("acl").count().next().intValue());
                assertEquals("private", g.V().properties("age").properties("acl").value().next());
                assertEquals("public", g.V().properties("name").properties("acl").value().next());
                assertEquals("private", g.V().properties("age").value("acl").next());
                assertEquals("public", g.V().properties("name").value("acl").next());
                assertEquals(1, g.V().count().next().intValue());
                assertEquals(0, g.E().count().next().intValue());
            });

            v.property("age").property("acl", "public");
            v.property("age").property("changeDate", 2014);
            tryCommit(g, g -> {
                assertEquals("public", g.V().properties("age").value("acl").next());
                assertEquals(2014, g.V().properties("age").value("changeDate").next());
                assertEquals(1, v.properties("age").valueMap().count().next().intValue());
                assertEquals(2, v.properties("age").valueMap().next().size());
                assertTrue(v.properties("age").valueMap().next().containsKey("acl"));
                assertTrue(v.properties("age").valueMap().next().containsKey("changeDate"));
                assertEquals("public", v.properties("age").valueMap().next().get("acl"));
                assertEquals(2014, v.properties("age").valueMap().next().get("changeDate"));
            });
        }
    }


    public static class MetaPropertyTraversals extends AbstractGremlinTest {
        @Test
        public void shouldHandleMetaPropertyTraversals() {
            Vertex v = g.addVertex("i", 1, "i", 2, "i", 3);
            tryCommit(g, g -> {
                assertEquals(3, v.properties().count().next().intValue());
                assertEquals(3, v.properties("i").count().next().intValue());
            });

            v.properties("i").sideEffect(m -> m.get().<Object>property("aKey", "aValue")).iterate();
            v.properties("i").properties("aKey").forEach(p -> assertEquals("aValue", p.value()));
            tryCommit(g, g -> {
                assertEquals(3, v.properties("i").properties("aKey").count().next().intValue());
                assertEquals(3, g.V().properties("i").properties("aKey").count().next().intValue());
                assertEquals(1, g.V().properties("i").has(MetaProperty.VALUE, 1).properties("aKey").count().next().intValue());
                assertEquals(3, g.V().properties("i").has(MetaProperty.KEY, "i").properties().count().next().intValue());
            });
        }
    }

    @RunWith(Parameterized.class)
    public static class MetaPropertiesShouldHideCorrectly extends AbstractGremlinTest {

        @Parameterized.Parameters(name = "{index}: {0}")
        public static Iterable<Object[]> data() {
            final List<Pair<String, BiFunction<Graph, Vertex, Boolean>>> tests = new ArrayList<>();
            tests.add(Pair.with("v.property(\"age\").isPresent()", (Graph g, Vertex v) -> v.property("age").isPresent()));
            tests.add(Pair.with("v.value(\"age\").equals(16)", (Graph g, Vertex v) -> v.value("age").equals(16)));
            tests.add(Pair.with("v.properties(\"age\").count().next().intValue() == 1", (Graph g, Vertex v) -> v.properties("age").count().next().intValue() == 1));
            tests.add(Pair.with("v.properties(\"age\").value().next().equals(16)", (Graph g, Vertex v) -> v.properties("age").value().next().equals(16)));
            tests.add(Pair.with("v.hiddens(\"age\").count().next().intValue() == 2", (Graph g, Vertex v) -> v.hiddens("age").count().next().intValue() == 2));
            tests.add(Pair.with("v.hiddens(Graph.Key.hide(\"age\")).count().next().intValue() == 0", (Graph g, Vertex v) -> v.hiddens(Graph.Key.hide("age")).count().next().intValue() == 0));
            tests.add(Pair.with("v.properties(Graph.Key.hide(\"age\")).count().next().intValue() == 0", (Graph g, Vertex v) -> v.properties(Graph.Key.hide("age")).count().next().intValue() == 0));
            tests.add(Pair.with("v.hiddens(\"age\").value().toList().contains(34)", (Graph g, Vertex v) -> v.hiddens("age").value().toList().contains(34)));
            tests.add(Pair.with("v.hiddens(\"age\").value().toList().contains(29)", (Graph g, Vertex v) -> v.hiddens("age").value().toList().contains(29)));
            tests.add(Pair.with("v.hiddenKeys().size() == 2", (Graph g, Vertex v) -> v.hiddenKeys().size() == 2));
            tests.add(Pair.with("v.keys().size() == 3", (Graph g, Vertex v) -> v.keys().size() == 3));
            tests.add(Pair.with("v.keys().contains(\"age\")", (Graph g, Vertex v) -> v.keys().contains("age")));
            tests.add(Pair.with("v.keys().contains(\"name\")", (Graph g, Vertex v) -> v.keys().contains("name")));
            tests.add(Pair.with("v.hiddenKeys().contains(\"age\")", (Graph g, Vertex v) -> v.hiddenKeys().contains("age")));
            tests.add(Pair.with("v.property(Graph.Key.hide(\"color\")).key().equals(\"color\")", (Graph g, Vertex v) -> v.property(Graph.Key.hide("color")).key().equals("color")));

            return tests.stream().map(d -> {
                final Object[] o = new Object[2];
                o[0] = d.getValue0();
                o[1] = d.getValue1();
                return o;
            }).collect(Collectors.toList());
        }

        @Parameterized.Parameter(value = 0)
        public String name;

        @Parameterized.Parameter(value = 1)
        public BiFunction<Graph, Vertex, Boolean> streamGetter;

        @Test
        @FeatureRequirementSet(FeatureRequirementSet.Package.SIMPLE)
        public void shouldHandleHiddenMetaProperties() {
            Vertex v = g.addVertex(Graph.Key.hide("age"), 34, Graph.Key.hide("age"), 29, "age", 16, "name", "marko", "food", "taco", Graph.Key.hide("color"), "purple");
            tryCommit(g, g -> {
                assertTrue(streamGetter.apply(g, v));
            });
        }
    }
}